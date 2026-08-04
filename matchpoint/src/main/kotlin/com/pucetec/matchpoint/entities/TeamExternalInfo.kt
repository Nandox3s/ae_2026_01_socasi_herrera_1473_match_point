package com.pucetec.matchpoint.entities

import jakarta.persistence.Embeddable

@Embeddable
class TeamExternalInfo(
    val name: String,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String
)
