package com.pucetec.matchpoint.entities

import com.pucetec.matchpoint.enums.ReservationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "reservations")
class Reservation(

    @ManyToOne
    @JoinColumn(name = "court_id")
    val court: Court,

    @Column(name = "owner_user")
    val ownerUser: String,

    // Copia del nombre que devolvio el microservicio `users`. Es dato ajeno: llega por
    // HTTP y se guarda aqui, nunca se consulta la base del otro servicio.
    @Column(name = "owner_name")
    val ownerName: String,

    @Column(name = "starts_at")
    val startsAt: LocalDateTime,

    @Column(name = "duration_minutes")
    val durationMinutes: Int,

    @Enumerated(EnumType.STRING)
    var status: ReservationStatus = ReservationStatus.CONFIRMED,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
) {

    fun endsAt(): LocalDateTime = startsAt.plusMinutes(durationMinutes.toLong())
}
