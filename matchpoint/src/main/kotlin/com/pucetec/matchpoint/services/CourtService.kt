package com.pucetec.matchpoint.services

import com.pucetec.matchpoint.audit.AuditAction
import com.pucetec.matchpoint.audit.AuditService
import com.pucetec.matchpoint.dto.CourtResponse
import com.pucetec.matchpoint.dto.CreateCourtRequest
import com.pucetec.matchpoint.dto.UpdateCourtRequest
import com.pucetec.matchpoint.entities.Court
import com.pucetec.matchpoint.enums.ReservationStatus
import com.pucetec.matchpoint.enums.SportType
import com.pucetec.matchpoint.exceptions.CourtNotFoundException
import com.pucetec.matchpoint.exceptions.InvalidCourtException
import com.pucetec.matchpoint.exceptions.NotYourCourtException
import com.pucetec.matchpoint.logging.logLine
import com.pucetec.matchpoint.mappers.CourtMapper
import com.pucetec.matchpoint.repositories.CourtRepository
import com.pucetec.matchpoint.repositories.ReservationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class CourtService(
    private val courtRepository: CourtRepository,
    private val reservationRepository: ReservationRepository,
    private val courtMapper: CourtMapper,
    private val auditService: AuditService
) {

    private val logger = LoggerFactory.getLogger(CourtService::class.java)

    fun createCourt(request: CreateCourtRequest, managerUser: String): CourtResponse {
        if (request.name.isBlank() || request.sector.isBlank() || request.floorType.isBlank()) {
            throw InvalidCourtException("Name, sector and floor type are required")
        }
        if (request.pricePerHour <= BigDecimal.ZERO) {
            throw InvalidCourtException("Price per hour must be greater than 0")
        }
        val saved = courtRepository.save(courtMapper.toEntity(request, managerUser))
        auditService.record(ENTITY_NAME, saved.id, AuditAction.INSERT, newValues = describe(saved))
        logger.info(
            logLine(
                "court.created",
                "Court created",
                "courtId" to saved.id,
                "sector" to saved.sector,
                "managerUser" to managerUser
            )
        )
        return courtMapper.toResponse(saved)
    }

    fun listCourts(sector: String?, sport: SportType?): List<CourtResponse> {
        val courts = courtRepository.findAll().filter { matches(it, sector, sport) }
        logger.debug(logLine("court.listed", "Courts listed", "total" to courts.size))
        return courtMapper.toResponseList(courts)
    }

    fun getCourt(id: Long): CourtResponse {
        val court = courtRepository.findById(id)
            .orElseThrow { CourtNotFoundException("Court $id was not found") }
        return courtMapper.toResponse(court)
    }

    fun availableCourts(
        sector: String?,
        sport: SportType?,
        startsAt: LocalDateTime?,
        durationMinutes: Int?
    ): List<CourtResponse> {
        val candidates = courtRepository.findByActiveTrue().filter { matches(it, sector, sport) }
        val free = if (startsAt != null && durationMinutes != null && durationMinutes > 0) {
            val endsAt = startsAt.plusMinutes(durationMinutes.toLong())
            candidates.filter { court -> isFree(court.id, startsAt, endsAt) }
        } else {
            candidates
        }
        logger.debug(logLine("court.availability.checked", "Availability resolved", "total" to free.size))
        return courtMapper.toResponseList(free)
    }

    fun updateCourt(id: Long, request: UpdateCourtRequest, managerUser: String): CourtResponse {
        val court = findCourtOwnedBy(id, managerUser)
        val previous = describe(court)
        request.pricePerHour?.let {
            if (it <= BigDecimal.ZERO) throw InvalidCourtException("Price per hour must be greater than 0")
            court.pricePerHour = it
        }
        request.active?.let { court.active = it }
        val saved = courtRepository.save(court)
        auditService.record(
            ENTITY_NAME,
            saved.id,
            AuditAction.UPDATE,
            oldValues = previous,
            newValues = describe(saved)
        )
        logger.info(
            logLine(
                "court.updated",
                "Court updated",
                "courtId" to saved.id,
                "active" to saved.active,
                "pricePerHour" to saved.pricePerHour
            )
        )
        return courtMapper.toResponse(saved)
    }

    private fun matches(court: Court, sector: String?, sport: SportType?): Boolean =
        (sector == null || court.sector.equals(sector, ignoreCase = true)) &&
            (sport == null || court.sportType == sport)

    private fun isFree(courtId: Long, startsAt: LocalDateTime, endsAt: LocalDateTime): Boolean =
        reservationRepository.findByCourtIdAndStatus(courtId, ReservationStatus.CONFIRMED)
            .none { startsAt < it.endsAt() && it.startsAt < endsAt }

    private fun findCourtOwnedBy(id: Long, managerUser: String): Court {
        val court = courtRepository.findById(id)
            .orElseThrow { CourtNotFoundException("Court $id was not found") }
        if (court.managerUser != managerUser) {
            throw NotYourCourtException("Court $id does not belong to you")
        }
        return court
    }

    private fun describe(court: Court): String =
        "name=${court.name} sector=${court.sector} pricePerHour=${court.pricePerHour} " +
            "active=${court.active} managerUser=${court.managerUser}"

    private companion object {
        const val ENTITY_NAME = "courts"
    }
}
