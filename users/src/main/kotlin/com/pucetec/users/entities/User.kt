package com.pucetec.users.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Column(name = "cognito_id", unique = true, nullable = false)
    val cognitoId: String = "",

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
