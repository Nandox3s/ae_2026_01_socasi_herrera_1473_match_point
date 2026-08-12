package com.pucetec.matchpoint.dto

import com.pucetec.matchpoint.clients.UserProfile

data class MeResponse(
    val username: String,
    val sub: String,
    val email: String?,
    val groups: List<String>,

    val profile: UserProfile?
)
