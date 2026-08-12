package com.pucetec.matchpoint.entities

import com.pucetec.matchpoint.enums.SportType
import com.pucetec.matchpoint.enums.TournamentStatus
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
@Table(name = "tournaments")
class Tournament(
    val name: String,

    @Enumerated(EnumType.STRING)
    val sportType: SportType,

    val maxTeams: Int,

    val prize: String? = null,

    val managerUser: String,

    @Enumerated(EnumType.STRING)
    var status: TournamentStatus = TournamentStatus.REGISTRATION,

    @ManyToOne
    @JoinColumn(name = "court_id")
    val court: Court? = null,

    @ManyToOne
    @JoinColumn(name = "champion_team_id")
    var championTeam: Team? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)
