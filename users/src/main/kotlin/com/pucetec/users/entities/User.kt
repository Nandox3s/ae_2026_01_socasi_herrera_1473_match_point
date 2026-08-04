package com.pucetec.users.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

// La unica funcion del micro: asociar un cognitoId (el "sub" del token de Cognito)
// con los datos propios del usuario que viven en NUESTRA base de datos.
@Entity
@Table(name = "users")
class User(

    // El identificador del usuario en Cognito (claim "sub"). Es unico: un
    // usuario de Cognito se asocia a un unico perfil en este micro.
    @Column(name = "cognito_id", unique = true, nullable = false)
    val cognitoId: String = "",

    // El claim "username" del token. Es la llave con la que matchpoint marca al
    // dueno de una cancha, un torneo o una reserva.
    @Column(nullable = false)
    var username: String = "",

    @Column(nullable = false)
    var name: String = "",

    var email: String? = null,

    var phone: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
)
