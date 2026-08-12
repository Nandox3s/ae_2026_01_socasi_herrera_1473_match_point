package com.pucetec.matchpoint.entities

import com.pucetec.matchpoint.enums.SportType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "courts")
class Court(
    val name: String,

    val sector: String,

    val hasParking: Boolean = false,

    @Enumerated(EnumType.STRING)
    val sportType: SportType,

    val floorType: String,

    @Column(precision = 8, scale = 2)
    var pricePerHour: BigDecimal,

    var active: Boolean = true,

    val managerUser: String,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)
