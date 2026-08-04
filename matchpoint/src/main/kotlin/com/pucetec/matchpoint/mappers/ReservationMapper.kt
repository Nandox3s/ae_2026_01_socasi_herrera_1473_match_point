package com.pucetec.matchpoint.mappers

import com.pucetec.matchpoint.dto.CreateReservationRequest
import com.pucetec.matchpoint.dto.ReservationResponse
import com.pucetec.matchpoint.entities.Court
import com.pucetec.matchpoint.entities.Reservation
import org.springframework.stereotype.Component

@Component
class ReservationMapper {

    fun toEntity(
        request: CreateReservationRequest,
        court: Court,
        ownerUser: String,
        ownerName: String
    ): Reservation =
        Reservation(
            court = court,
            ownerUser = ownerUser,
            ownerName = ownerName,
            startsAt = request.startsAt,
            durationMinutes = request.durationMinutes
        )

    fun toResponse(reservation: Reservation): ReservationResponse =
        ReservationResponse(
            id = reservation.id,
            courtId = reservation.court.id,
            courtName = reservation.court.name,
            ownerUser = reservation.ownerUser,
            ownerName = reservation.ownerName,
            startsAt = reservation.startsAt,
            durationMinutes = reservation.durationMinutes,
            status = reservation.status,
            createdAt = reservation.createdAt
        )

    fun toResponseList(reservations: List<Reservation>): List<ReservationResponse> =
        reservations.map { toResponse(it) }
}
