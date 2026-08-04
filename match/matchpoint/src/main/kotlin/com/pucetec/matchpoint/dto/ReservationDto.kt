package com.pucetec.matchpoint.dto

import com.pucetec.matchpoint.enums.ReservationStatus
import java.time.LocalDateTime

data class CreateReservationRequest(
    val courtId: Long,
    val startsAt: LocalDateTime,
    val durationMinutes: Int
)

data class ReservationResponse(
    val id: Long,
    val courtId: Long,
    val courtName: String,
    val ownerUser: String,
    // Viene del microservicio `users`, no de la base de matchpoint.
    val ownerName: String,
    val startsAt: LocalDateTime,
    val durationMinutes: Int,
    val status: ReservationStatus,
    val createdAt: LocalDateTime
)
