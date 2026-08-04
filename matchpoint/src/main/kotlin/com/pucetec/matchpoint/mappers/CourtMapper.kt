package com.pucetec.matchpoint.mappers

import com.pucetec.matchpoint.dto.CourtResponse
import com.pucetec.matchpoint.dto.CreateCourtRequest
import com.pucetec.matchpoint.entities.Court
import org.springframework.stereotype.Component

@Component
class CourtMapper {

    fun toEntity(request: CreateCourtRequest, managerUser: String): Court =
        Court(
            name = request.name,
            sector = request.sector,
            hasParking = request.hasParking,
            sportType = request.sportType,
            floorType = request.floorType,
            pricePerHour = request.pricePerHour,
            managerUser = managerUser
        )

    fun toResponse(court: Court): CourtResponse =
        CourtResponse(
            id = court.id,
            name = court.name,
            sector = court.sector,
            hasParking = court.hasParking,
            sportType = court.sportType,
            floorType = court.floorType,
            pricePerHour = court.pricePerHour,
            active = court.active,
            managerUser = court.managerUser,
            createdAt = court.createdAt
        )

    fun toResponseList(courts: List<Court>): List<CourtResponse> = courts.map { toResponse(it) }
}
