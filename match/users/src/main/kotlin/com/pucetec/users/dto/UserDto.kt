package com.pucetec.users.dto

import java.time.LocalDateTime

/***
 * Lo que envia el cliente al registrar/actualizar su perfil.
 * Ni el cognitoId ni el username viajan aqui: los dos salen del token.
 * {name: ana, email: ana@puce.edu.ec, phone: 0999999999}
 */
data class UserRequest(
    val name: String,
    val email: String?,
    val phone: String?,
)

/***
 * Lo que devuelve el micro: el perfil ya asociado a su cognitoId.
 */
data class UserResponse(
    val id: Long,
    val cognitoId: String,
    val username: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val createdAt: LocalDateTime,
)
