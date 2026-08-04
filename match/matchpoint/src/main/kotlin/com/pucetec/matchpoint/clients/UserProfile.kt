package com.pucetec.matchpoint.clients

/**
 * Lo que devuelve el microservicio `users`. matchpoint NUNCA lee la base de users:
 * este objeto llega por HTTP, con el token del usuario propagado.
 */
data class UserProfile(
    val id: Long,
    val cognitoId: String,
    val username: String,
    val name: String,
    val email: String?,
    val phone: String?
)
