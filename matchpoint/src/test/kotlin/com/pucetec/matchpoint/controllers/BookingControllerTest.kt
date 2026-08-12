package com.pucetec.matchpoint.controllers

import com.pucetec.matchpoint.config.SecurityConfig
import com.pucetec.matchpoint.dto.CourtResponse
import com.pucetec.matchpoint.dto.ReservationResponse
import com.pucetec.matchpoint.enums.ReservationStatus
import com.pucetec.matchpoint.enums.SportType
import com.pucetec.matchpoint.exceptions.CourtNotAvailableException
import com.pucetec.matchpoint.exceptions.CourtNotFoundException
import com.pucetec.matchpoint.exceptions.GlobalExceptionHandler
import com.pucetec.matchpoint.exceptions.InvalidCourtException
import com.pucetec.matchpoint.exceptions.NotYourCourtException
import com.pucetec.matchpoint.exceptions.ProfileNotRegisteredException
import com.pucetec.matchpoint.exceptions.UsersServiceUnavailableException
import com.pucetec.matchpoint.services.CourtService
import com.pucetec.matchpoint.services.ReservationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(CourtController::class, ReservationController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class BookingControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var courtService: CourtService

    @MockitoBean
    lateinit var reservationService: ReservationService

    private fun manager() = jwt()
        .authorities(SimpleGrantedAuthority("ROLE_MANAGER"))
        .jwt { it.subject("sub-manager").claim("username", "manager_josue") }

    private fun player() = jwt()
        .authorities(SimpleGrantedAuthority("ROLE_PLAYER"))
        .jwt { it.subject("sub-player").claim("username", "player_fernando") }

    private val courtBody =
        """{"name":"North Court 1","sector":"North","hasParking":true,"sportType":"BASKET","floorType":"Concrete","pricePerHour":12.50}"""

    private val reservationBody =
        """{"courtId":1,"startsAt":"2026-08-10T18:00:00","durationMinutes":60}"""

    private fun sampleCourt() = CourtResponse(
        id = 1, name = "North Court 1", sector = "North", hasParking = true,
        sportType = SportType.BASKET, floorType = "Concrete", pricePerHour = BigDecimal("12.50"),
        active = true, managerUser = "manager_josue", createdAt = LocalDateTime.of(2026, 7, 5, 8, 0)
    )

    private fun sampleReservation() = ReservationResponse(
        id = 1, courtId = 1, courtName = "North Court 1", ownerUser = "player_fernando",
        ownerName = "Fernando Socasi", startsAt = LocalDateTime.of(2026, 8, 10, 18, 0),
        durationMinutes = 60, status = ReservationStatus.CONFIRMED,
        createdAt = LocalDateTime.of(2026, 7, 8, 12, 0)
    )

    @Test
    fun `GET courts is public`() {
        whenever(courtService.listCourts(anyOrNull(), anyOrNull())).thenReturn(listOf(sampleCourt()))

        mockMvc.perform(get("/courts?sector=North&sport=BASKET"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("North Court 1"))
    }

    @Test
    fun `GET courts available is public`() {
        whenever(courtService.availableCourts(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(sampleCourt()))

        mockMvc.perform(get("/courts/available?startsAt=2026-08-10T18:00:00&durationMinutes=60"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET court detail is public`() {
        whenever(courtService.getCourt(1L)).thenReturn(sampleCourt())

        mockMvc.perform(get("/courts/1")).andExpect(status().isOk)
    }

    @Test
    fun `POST courts without a token returns 401`() {
        mockMvc.perform(post("/courts").contentType(MediaType.APPLICATION_JSON).content(courtBody))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST courts with PLAYER returns 403`() {
        mockMvc.perform(post("/courts").with(player()).contentType(MediaType.APPLICATION_JSON).content(courtBody))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST courts with MANAGER returns 201 and uses the username from the token`() {
        whenever(courtService.createCourt(any(), any())).thenReturn(sampleCourt())

        mockMvc.perform(post("/courts").with(manager()).contentType(MediaType.APPLICATION_JSON).content(courtBody))
            .andExpect(status().isCreated)

        verify(courtService).createCourt(any(), eq("manager_josue"))
    }

    @Test
    fun `PATCH court with MANAGER returns 200`() {
        whenever(courtService.updateCourt(any(), any(), any())).thenReturn(sampleCourt())

        mockMvc.perform(
            patch("/courts/1").with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"pricePerHour":20.00,"active":false}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `PATCH court with PLAYER returns 403`() {
        mockMvc.perform(
            patch("/courts/1").with(player()).contentType(MediaType.APPLICATION_JSON).content("""{"active":false}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST reservations with MANAGER returns 403`() {
        mockMvc.perform(
            post("/reservations").with(manager()).contentType(MediaType.APPLICATION_JSON).content(reservationBody)
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST reservations with PLAYER returns 201`() {
        whenever(reservationService.createReservation(any(), any())).thenReturn(sampleReservation())

        mockMvc.perform(
            post("/reservations").with(player()).contentType(MediaType.APPLICATION_JSON).content(reservationBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.ownerName").value("Fernando Socasi"))
    }

    @Test
    fun `GET my reservations with PLAYER returns 200`() {
        whenever(reservationService.listMine("player_fernando")).thenReturn(listOf(sampleReservation()))

        mockMvc.perform(get("/reservations/me").with(player()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET my reservations with MANAGER returns 403`() {
        mockMvc.perform(get("/reservations/me").with(manager())).andExpect(status().isForbidden)
    }

    @Test
    fun `GET reservation detail with PLAYER returns 200`() {
        whenever(reservationService.getMine(1L, "player_fernando")).thenReturn(sampleReservation())

        mockMvc.perform(get("/reservations/1").with(player())).andExpect(status().isOk)
    }

    @Test
    fun `DELETE reservation with PLAYER returns 204`() {
        mockMvc.perform(delete("/reservations/1").with(player())).andExpect(status().isNoContent)

        verify(reservationService).cancel(1L, "player_fernando")
    }

    @Test
    fun `a missing court is answered with 404`() {
        whenever(courtService.getCourt(9L)).thenThrow(CourtNotFoundException("Court 9 was not found"))

        mockMvc.perform(get("/courts/9"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Court 9 was not found"))
    }

    @Test
    fun `a court owned by somebody else is answered with 403`() {
        whenever(courtService.updateCourt(any(), any(), any()))
            .thenThrow(NotYourCourtException("Court 1 does not belong to you"))

        mockMvc.perform(
            patch("/courts/1").with(manager()).contentType(MediaType.APPLICATION_JSON).content("""{"active":false}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `an invalid court is answered with 400`() {
        whenever(courtService.createCourt(any(), any()))
            .thenThrow(InvalidCourtException("Price per hour must be greater than 0"))

        mockMvc.perform(post("/courts").with(manager()).contentType(MediaType.APPLICATION_JSON).content(courtBody))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `an occupied court is answered with 409`() {
        whenever(reservationService.createReservation(any(), any()))
            .thenThrow(CourtNotAvailableException("Court 1 is already booked on that time slot"))

        mockMvc.perform(
            post("/reservations").with(player()).contentType(MediaType.APPLICATION_JSON).content(reservationBody)
        ).andExpect(status().isConflict)
    }

    @Test
    fun `a player without a profile in the users microservice is answered with 409`() {
        whenever(reservationService.createReservation(any(), any()))
            .thenThrow(ProfileNotRegisteredException("Register your profile in the users microservice"))

        mockMvc.perform(
            post("/reservations").with(player()).contentType(MediaType.APPLICATION_JSON).content(reservationBody)
        ).andExpect(status().isConflict)
    }

    @Test
    fun `the users microservice being down is answered with 503`() {
        whenever(reservationService.createReservation(any(), any()))
            .thenThrow(UsersServiceUnavailableException("The users microservice is not reachable right now"))

        mockMvc.perform(
            post("/reservations").with(player()).contentType(MediaType.APPLICATION_JSON).content(reservationBody)
        ).andExpect(status().isServiceUnavailable)
    }
}
