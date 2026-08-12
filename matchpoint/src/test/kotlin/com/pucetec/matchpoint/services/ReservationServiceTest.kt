package com.pucetec.matchpoint.services

import com.pucetec.matchpoint.audit.AuditAction
import com.pucetec.matchpoint.audit.AuditService
import com.pucetec.matchpoint.clients.UserProfile
import com.pucetec.matchpoint.clients.UsersClient
import com.pucetec.matchpoint.dto.CreateReservationRequest
import com.pucetec.matchpoint.entities.Court
import com.pucetec.matchpoint.entities.Reservation
import com.pucetec.matchpoint.enums.ReservationStatus
import com.pucetec.matchpoint.enums.SportType
import com.pucetec.matchpoint.exceptions.CourtNotAvailableException
import com.pucetec.matchpoint.exceptions.CourtNotFoundException
import com.pucetec.matchpoint.exceptions.InvalidReservationException
import com.pucetec.matchpoint.exceptions.NotYourReservationException
import com.pucetec.matchpoint.exceptions.ProfileNotRegisteredException
import com.pucetec.matchpoint.exceptions.ReservationNotFoundException
import com.pucetec.matchpoint.mappers.ReservationMapper
import com.pucetec.matchpoint.repositories.CourtRepository
import com.pucetec.matchpoint.repositories.ReservationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class ReservationServiceTest {
    private lateinit var reservationRepository: ReservationRepository
    private lateinit var courtRepository: CourtRepository
    private lateinit var usersClient: UsersClient
    private lateinit var auditService: AuditService
    private lateinit var service: ReservationService

    private val player = "player_fernando"
    private val startsAt = LocalDateTime.of(2026, 8, 10, 18, 0)

    @BeforeEach
    fun setUp() {
        reservationRepository = mock()
        courtRepository = mock()
        usersClient = mock()
        auditService = mock()
        service = ReservationService(
            reservationRepository, courtRepository, ReservationMapper(), usersClient, auditService
        )
        whenever(usersClient.fetchCurrentProfile()).thenReturn(profile())
    }

    private fun profile() = UserProfile(
        id = 3L,
        cognitoId = "seed-sub-player-fernando",
        username = player,
        name = "Fernando Socasi",
        email = "fernando.socasi@puce.edu.ec",
        phone = "0999555666"
    )

    private fun court(id: Long = 1L, active: Boolean = true) = Court(
        name = "North Court 1",
        sector = "North",
        hasParking = true,
        sportType = SportType.BASKET,
        floorType = "Concrete",
        pricePerHour = BigDecimal("12.50"),
        active = active,
        managerUser = "manager_josue",
        id = id
    )

    private fun reservation(
        id: Long = 1L,
        owner: String = player,
        at: LocalDateTime = startsAt,
        minutes: Int = 60,
        status: ReservationStatus = ReservationStatus.CONFIRMED
    ) = Reservation(
        court = court(),
        ownerUser = owner,
        ownerName = "Fernando Socasi",
        startsAt = at,
        durationMinutes = minutes,
        status = status,
        id = id
    )

    private fun request(minutes: Int = 60, courtId: Long = 1L) =
        CreateReservationRequest(courtId, startsAt, minutes)

    @Test
    fun `createReservation asks the users microservice for the profile and stores its name`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court()))
        whenever(reservationRepository.findByCourtIdAndStatus(1L, ReservationStatus.CONFIRMED))
            .thenReturn(emptyList())
        whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.arguments[0] as Reservation }

        val response = service.createReservation(request(), player)

        assertEquals(player, response.ownerUser)
        assertEquals("Fernando Socasi", response.ownerName)
        assertEquals(1L, response.courtId)
        verify(usersClient).fetchCurrentProfile()
        verify(auditService).record(eq("reservations"), any(), eq(AuditAction.INSERT), anyOrNull(), anyOrNull())
    }

    @Test
    fun `createReservation is refused when the player has no profile in the users microservice`() {
        whenever(usersClient.fetchCurrentProfile())
            .thenThrow(ProfileNotRegisteredException("Register your profile first"))

        assertThrows<ProfileNotRegisteredException> { service.createReservation(request(), player) }
        verify(courtRepository, never()).findById(any())
    }

    @Test
    fun `createReservation fails when the court does not exist`() {
        whenever(courtRepository.findById(9L)).thenReturn(Optional.empty())

        assertThrows<CourtNotFoundException> { service.createReservation(request(courtId = 9L), player) }
    }

    @Test
    fun `createReservation refuses an inactive court`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court(active = false)))

        assertThrows<CourtNotAvailableException> { service.createReservation(request(), player) }
    }

    @Test
    fun `createReservation refuses a duration that is not positive`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court()))

        assertThrows<InvalidReservationException> { service.createReservation(request(minutes = 0), player) }
    }

    @Test
    fun `createReservation refuses an overlapping time slot`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court()))
        whenever(reservationRepository.findByCourtIdAndStatus(1L, ReservationStatus.CONFIRMED))
            .thenReturn(listOf(reservation(at = startsAt.minusMinutes(30), minutes = 60)))

        assertThrows<CourtNotAvailableException> { service.createReservation(request(), player) }
    }

    @Test
    fun `listMine only returns the reservations of the caller`() {
        whenever(reservationRepository.findByOwnerUserOrderByStartsAtDesc(player))
            .thenReturn(listOf(reservation(), reservation(id = 2L)))

        assertEquals(2, service.listMine(player).size)
    }

    @Test
    fun `getMine returns the detail of an own reservation`() {
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation()))

        assertEquals(1L, service.getMine(1L, player).id)
    }

    @Test
    fun `getMine fails when the reservation does not exist`() {
        whenever(reservationRepository.findById(9L)).thenReturn(Optional.empty())

        assertThrows<ReservationNotFoundException> { service.getMine(9L, player) }
    }

    @Test
    fun `getMine refuses a reservation of another player`() {
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation(owner = "player_luis")))

        assertThrows<NotYourReservationException> { service.getMine(1L, player) }
    }

    @Test
    fun `cancel switches the status to CANCELLED`() {
        val existing = reservation()
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.arguments[0] as Reservation }

        service.cancel(1L, player)

        assertEquals(ReservationStatus.CANCELLED, existing.status)
        verify(auditService).record(eq("reservations"), eq(1L), eq(AuditAction.UPDATE), anyOrNull(), anyOrNull())
    }

    @Test
    fun `cancel refuses a reservation of another player`() {
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation(owner = "player_luis")))

        assertThrows<NotYourReservationException> { service.cancel(1L, player) }
    }
}
