package com.pucetec.matchpoint.entities

import com.pucetec.matchpoint.enums.MatchStatus
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
@Table(name = "matches")
class Match(

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    val tournament: Tournament,

    val roundNumber: Int,

    val positionInRound: Int,

    @ManyToOne
    @JoinColumn(name = "home_team_id")
    var homeTeam: Team? = null,

    @ManyToOne
    @JoinColumn(name = "away_team_id")
    var awayTeam: Team? = null,

    var homeScore: Int? = null,

    var awayScore: Int? = null,

    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    var winnerTeam: Team? = null,

    @Enumerated(EnumType.STRING)
    var status: MatchStatus = MatchStatus.PENDING,

    var scheduledAt: LocalDateTime? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)
