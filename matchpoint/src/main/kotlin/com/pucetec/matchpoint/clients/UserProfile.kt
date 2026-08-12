package com.pucetec.matchpoint.clients

data class UserProfile(
    val id: Long,
    val cognitoId: String,
    val username: String,
    val name: String,
    val email: String?,
    val phone: String?
)
