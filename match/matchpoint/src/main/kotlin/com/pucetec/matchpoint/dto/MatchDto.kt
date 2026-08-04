package com.pucetec.matchpoint.dto

import com.pucetec.matchpoint.enums.MatchStatus
import java.time.LocalDateTime

data class RegisterScoreRequest(
    val homeScore: Int,
    val awayScore: Int
)

data class ScheduleMatchRequest(
    val scheduledAt: LocalDateTime
)

data class MatchResponse(
    val id: Long,
    val tournamentId: Long,
    val roundNumber: Int,
    val positionInRound: Int,
    val homeTeamName: String?,
    val awayTeamName: String?,
    val homeScore: Int?,
    val awayScore: Int?,
    val winnerTeamName: String?,
    val status: MatchStatus,
    val scheduledAt: LocalDateTime?
)
