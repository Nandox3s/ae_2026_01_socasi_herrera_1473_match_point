package com.pucetec.matchpoint.entities

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "teams")
class Team(
    @ManyToOne
    @JoinColumn(name = "tournament_id")
    val tournament: Tournament,

    val registeredByUser: String,

    @Embedded
    val externalInfo: TeamExternalInfo,

    @Embedded
    val internalStats: TeamInternalStats = TeamInternalStats(),

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)
