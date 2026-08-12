package com.pucetec.matchpoint.services

import com.pucetec.matchpoint.audit.AuditAction
import com.pucetec.matchpoint.audit.AuditService
import com.pucetec.matchpoint.clients.UsersClient
import com.pucetec.matchpoint.dto.CreateReservationRequest
import com.pucetec.matchpoint.dto.ReservationResponse
import com.pucetec.matchpoint.entities.Reservation
import com.pucetec.matchpoint.enums.ReservationStatus
import com.pucetec.matchpoint.exceptions.CourtNotAvailableException
import com.pucetec.matchpoint.exceptions.CourtNotFoundException
import com.pucetec.matchpoint.exceptions.InvalidReservationException
import com.pucetec.matchpoint.exceptions.NotYourReservationException
import com.pucetec.matchpoint.exceptions.ReservationNotFoundException
import com.pucetec.matchpoint.logging.logLine
import com.pucetec.matchpoint.mappers.ReservationMapper
import com.pucetec.matchpoint.repositories.CourtRepository
import com.pucetec.matchpoint.repositories.ReservationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val courtRepository: CourtRepository,
    private val reservationMapper: ReservationMapper,
    private val usersClient: UsersClient,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(ReservationService::class.java)

    fun createReservation(request: CreateReservationRequest, playerUser: String): ReservationResponse {
        val profile = usersClient.fetchCurrentProfile()

        val court = courtRepository.findById(request.courtId)
            .orElseThrow { CourtNotFoundException("Court ${request.courtId} was not found") }

        if (!court.active) {
            throw CourtNotAvailableException("Court ${court.id} is not active")
        }
        if (request.durationMinutes <= 0) {
            throw InvalidReservationException("Duration must be greater than 0 minutes")
        }

        val endsAt = request.startsAt.plusMinutes(request.durationMinutes.toLong())
        val overlap = reservationRepository
            .findByCourtIdAndStatus(court.id, ReservationStatus.CONFIRMED)
            .any { request.startsAt < it.endsAt() && it.startsAt < endsAt }
        if (overlap) {
            throw CourtNotAvailableException("Court ${court.id} is already booked on that time slot")
        }

        val saved = reservationRepository.save(
            reservationMapper.toEntity(request, court, playerUser, profile.name)
        )
        auditService.record(ENTITY_NAME, saved.id, AuditAction.INSERT, newValues = describe(saved))
        logger.info(
            logLine(
                "reservation.created",
                "Reservation created",
                "reservationId" to saved.id,
                "courtId" to court.id,
                "ownerUser" to playerUser,
                "startsAt" to saved.startsAt
            )
        )
        return reservationMapper.toResponse(saved)
    }

    fun listMine(playerUser: String): List<ReservationResponse> {
        val reservations = reservationRepository.findByOwnerUserOrderByStartsAtDesc(playerUser)
        logger.debug(logLine("reservation.listed", "Own reservations listed", "total" to reservations.size))
        return reservationMapper.toResponseList(reservations)
    }

    fun getMine(id: Long, playerUser: String): ReservationResponse =
        reservationMapper.toResponse(findReservationOwnedBy(id, playerUser))

    fun cancel(id: Long, playerUser: String) {
        val reservation = findReservationOwnedBy(id, playerUser)
        val previous = describe(reservation)
        reservation.status = ReservationStatus.CANCELLED
        val saved = reservationRepository.save(reservation)
        auditService.record(
            ENTITY_NAME,
            saved.id,
            AuditAction.UPDATE,
            oldValues = previous,
            newValues = describe(saved)
        )
        logger.info(
            logLine(
                "reservation.cancelled",
                "Reservation cancelled",
                "reservationId" to id,
                "ownerUser" to playerUser
            )
        )
    }

    private fun findReservationOwnedBy(id: Long, playerUser: String): Reservation {
        val reservation = reservationRepository.findById(id)
            .orElseThrow { ReservationNotFoundException("Reservation $id was not found") }
        if (reservation.ownerUser != playerUser) {
            throw NotYourReservationException("Reservation $id does not belong to you")
        }
        return reservation
    }

    private fun describe(reservation: Reservation): String =
        "courtId=${reservation.court.id} ownerUser=${reservation.ownerUser} " +
            "startsAt=${reservation.startsAt} durationMinutes=${reservation.durationMinutes} " +
            "status=${reservation.status}"

    private companion object {
        const val ENTITY_NAME = "reservations"
    }
}
