package com.pucetec.matchpoint.dto

import com.pucetec.matchpoint.enums.SportType
import com.pucetec.matchpoint.enums.TournamentStatus
import java.time.LocalDateTime

data class CreateTournamentRequest(
    val name: String,
    val sportType: SportType,
    val maxTeams: Int,
    val prize: String? = null,
    val courtId: Long? = null
)

data class TournamentResponse(
    val id: Long,
    val name: String,
    val sportType: SportType,
    val maxTeams: Int,
    val registeredTeams: Int,
    val prize: String?,
    val status: TournamentStatus,
    val managerUser: String,
    val courtName: String?,
    val championTeamName: String?,
    val createdAt: LocalDateTime
)

data class RoundResponse(
    val roundNumber: Int,
    val roundName: String,
    val matches: List<MatchResponse>
)

data class TournamentProgressResponse(
    val tournament: TournamentResponse,
    val rounds: List<RoundResponse>,
    val champion: TeamResponse?
)
