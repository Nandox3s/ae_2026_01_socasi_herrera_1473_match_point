package com.pucetec.users.dto

import java.time.LocalDateTime

data class UserRequest(
    val name: String,
    val email: String?,
    val phone: String?,
)

data class UserResponse(
    val id: Long,
    val cognitoId: String,
    val username: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val createdAt: LocalDateTime,
)
