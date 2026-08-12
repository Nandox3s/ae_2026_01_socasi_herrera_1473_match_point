package com.pucetec.matchpoint.services

import com.pucetec.matchpoint.audit.AuditAction
import com.pucetec.matchpoint.audit.AuditService
import com.pucetec.matchpoint.dto.CreateCourtRequest
import com.pucetec.matchpoint.dto.UpdateCourtRequest
import com.pucetec.matchpoint.entities.Court
import com.pucetec.matchpoint.entities.Reservation
import com.pucetec.matchpoint.enums.ReservationStatus
import com.pucetec.matchpoint.enums.SportType
import com.pucetec.matchpoint.exceptions.CourtNotFoundException
import com.pucetec.matchpoint.exceptions.InvalidCourtException
import com.pucetec.matchpoint.exceptions.NotYourCourtException
import com.pucetec.matchpoint.mappers.CourtMapper
import com.pucetec.matchpoint.repositories.CourtRepository
import com.pucetec.matchpoint.repositories.ReservationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class CourtServiceTest {
    private lateinit var courtRepository: CourtRepository
    private lateinit var reservationRepository: ReservationRepository
    private lateinit var auditService: AuditService
    private lateinit var service: CourtService

    private val manager = "manager_josue"

    @BeforeEach
    fun setUp() {
        courtRepository = mock()
        reservationRepository = mock()
        auditService = mock()
        service = CourtService(courtRepository, reservationRepository, CourtMapper(), auditService)
    }

    private fun court(
        id: Long = 1L,
        managerUser: String = manager,
        active: Boolean = true,
        sector: String = "North",
        sportType: SportType = SportType.BASKET
    ) = Court(
        name = "North Court 1",
        sector = sector,
        hasParking = true,
        sportType = sportType,
        floorType = "Concrete",
        pricePerHour = BigDecimal("12.50"),
        active = active,
        managerUser = managerUser,
        id = id
    )

    private fun reservation(startsAt: LocalDateTime, minutes: Int = 60) = Reservation(
        court = court(),
        ownerUser = "player_fernando",
        ownerName = "Fernando Socasi",
        startsAt = startsAt,
        durationMinutes = minutes
    )

    private fun createRequest(
        name: String = "North Court 1",
        sector: String = "North",
        floorType: String = "Concrete",
        price: BigDecimal = BigDecimal("12.50")
    ) = CreateCourtRequest(name, sector, true, SportType.BASKET, floorType, price)

    @Test
    fun `createCourt stores the court and takes the owner from the token`() {
        whenever(courtRepository.save(any<Court>())).thenAnswer { it.arguments[0] as Court }

        val response = service.createCourt(createRequest(), manager)

        assertEquals("North Court 1", response.name)
        assertEquals("North", response.sector)
        assertEquals(manager, response.managerUser)
        verify(auditService).record(eq("courts"), any(), eq(AuditAction.INSERT), anyOrNull(), anyOrNull())
    }

    @Test
    fun `createCourt rejects a blank name`() {
        assertThrows<InvalidCourtException> { service.createCourt(createRequest(name = " "), manager) }
    }

    @Test
    fun `createCourt rejects a blank sector`() {
        assertThrows<InvalidCourtException> { service.createCourt(createRequest(sector = " "), manager) }
    }

    @Test
    fun `createCourt rejects a blank floor type`() {
        assertThrows<InvalidCourtException> { service.createCourt(createRequest(floorType = " "), manager) }
    }

    @Test
    fun `createCourt rejects a price that is not greater than zero`() {
        assertThrows<InvalidCourtException> {
            service.createCourt(createRequest(price = BigDecimal.ZERO), manager)
        }
    }

    @Test
    fun `listCourts without filters returns everything`() {
        whenever(courtRepository.findAll()).thenReturn(listOf(court(id = 1L), court(id = 2L, sector = "South")))

        assertEquals(2, service.listCourts(null, null).size)
    }

    @Test
    fun `listCourts filters by sector ignoring case`() {
        whenever(courtRepository.findAll()).thenReturn(listOf(court(id = 1L), court(id = 2L, sector = "South")))

        val filtered = service.listCourts("north", null)

        assertEquals(1, filtered.size)
        assertEquals(1L, filtered[0].id)
    }

    @Test
    fun `listCourts filters by sport`() {
        whenever(courtRepository.findAll()).thenReturn(listOf(court(id = 1L)))

        assertEquals(1, service.listCourts(null, SportType.BASKET).size)
    }

    @Test
    fun `getCourt returns the detail`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court()))

        assertEquals("North Court 1", service.getCourt(1L).name)
    }

    @Test
    fun `getCourt fails when the court does not exist`() {
        whenever(courtRepository.findById(9L)).thenReturn(Optional.empty())

        assertThrows<CourtNotFoundException> { service.getCourt(9L) }
    }

    @Test
    fun `availableCourts without a time slot returns every active court`() {
        whenever(courtRepository.findByActiveTrue()).thenReturn(listOf(court()))

        assertEquals(1, service.availableCourts(null, null, null, null).size)
    }

    @Test
    fun `availableCourts ignores a duration that is not positive`() {
        whenever(courtRepository.findByActiveTrue()).thenReturn(listOf(court()))

        assertEquals(1, service.availableCourts(null, null, LocalDateTime.of(2026, 8, 10, 18, 0), 0).size)
    }

    @Test
    fun `availableCourts hides a court already booked on that slot`() {
        whenever(courtRepository.findByActiveTrue()).thenReturn(listOf(court()))
        whenever(reservationRepository.findByCourtIdAndStatus(1L, ReservationStatus.CONFIRMED))
            .thenReturn(listOf(reservation(LocalDateTime.of(2026, 8, 10, 18, 0))))

        val free = service.availableCourts(null, null, LocalDateTime.of(2026, 8, 10, 18, 30), 60)

        assertTrue(free.isEmpty())
    }

    @Test
    fun `availableCourts keeps a court whose reservations do not overlap`() {
        whenever(courtRepository.findByActiveTrue()).thenReturn(listOf(court()))
        whenever(reservationRepository.findByCourtIdAndStatus(1L, ReservationStatus.CONFIRMED))
            .thenReturn(listOf(reservation(LocalDateTime.of(2026, 8, 10, 18, 0))))

        val free = service.availableCourts(null, null, LocalDateTime.of(2026, 8, 10, 20, 0), 60)

        assertEquals(1, free.size)
    }

    @Test
    fun `updateCourt changes the price and the active flag`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court()))
        whenever(courtRepository.save(any<Court>())).thenAnswer { it.arguments[0] as Court }

        val response = service.updateCourt(1L, UpdateCourtRequest(BigDecimal("20.00"), false), manager)

        assertEquals(BigDecimal("20.00"), response.pricePerHour)
        assertFalse(response.active)
        verify(auditService).record(eq("courts"), eq(1L), eq(AuditAction.UPDATE), anyOrNull(), anyOrNull())
    }

    @Test
    fun `updateCourt accepts a request that changes nothing`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court()))
        whenever(courtRepository.save(any<Court>())).thenAnswer { it.arguments[0] as Court }

        assertEquals(BigDecimal("12.50"), service.updateCourt(1L, UpdateCourtRequest(), manager).pricePerHour)
    }

    @Test
    fun `updateCourt rejects a price that is not greater than zero`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court()))

        assertThrows<InvalidCourtException> {
            service.updateCourt(1L, UpdateCourtRequest(BigDecimal("-1")), manager)
        }
    }

    @Test
    fun `updateCourt fails when the court does not exist`() {
        whenever(courtRepository.findById(9L)).thenReturn(Optional.empty())

        assertThrows<CourtNotFoundException> { service.updateCourt(9L, UpdateCourtRequest(), manager) }
    }

    @Test
    fun `updateCourt rejects a court owned by another manager`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court(managerUser = "manager_ana")))

        assertThrows<NotYourCourtException> { service.updateCourt(1L, UpdateCourtRequest(), manager) }
    }
}
