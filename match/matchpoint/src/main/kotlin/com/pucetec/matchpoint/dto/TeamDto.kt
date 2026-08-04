package com.pucetec.matchpoint.dto

import java.time.LocalDateTime

data class RegisterTeamRequest(
    val name: String,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String
)

data class TeamStatsResponse(
    val eliminated: Boolean,
    val matchesPlayed: Int,
    val matchesWon: Int,
    val matchesLost: Int,
    val pointsFor: Int,
    val pointsAgainst: Int,
    val currentRound: Int
)

data class TeamResponse(
    val id: Long,
    val tournamentId: Long,
    val name: String,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val registeredByUser: String,
    val stats: TeamStatsResponse,
    val createdAt: LocalDateTime
)
