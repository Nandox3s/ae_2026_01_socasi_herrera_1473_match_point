package com.pucetec.matchpoint.dto

import com.pucetec.matchpoint.enums.SportType
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateCourtRequest(
    val name: String,
    val sector: String,
    val hasParking: Boolean,
    val sportType: SportType,
    val floorType: String,
    val pricePerHour: BigDecimal
)

data class UpdateCourtRequest(
    val pricePerHour: BigDecimal? = null,
    val active: Boolean? = null
)

data class CourtResponse(
    val id: Long,
    val name: String,
    val sector: String,
    val hasParking: Boolean,
    val sportType: SportType,
    val floorType: String,
    val pricePerHour: BigDecimal,
    val active: Boolean,
    val managerUser: String,
    val createdAt: LocalDateTime
)
