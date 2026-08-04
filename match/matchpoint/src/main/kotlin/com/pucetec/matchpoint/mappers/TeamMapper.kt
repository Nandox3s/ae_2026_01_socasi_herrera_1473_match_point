package com.pucetec.matchpoint.mappers

import com.pucetec.matchpoint.dto.RegisterTeamRequest
import com.pucetec.matchpoint.dto.TeamResponse
import com.pucetec.matchpoint.dto.TeamStatsResponse
import com.pucetec.matchpoint.entities.Team
import com.pucetec.matchpoint.entities.TeamExternalInfo
import com.pucetec.matchpoint.entities.TeamInternalStats
import com.pucetec.matchpoint.entities.Tournament
import org.springframework.stereotype.Component

@Component
class TeamMapper {

    fun toEntity(request: RegisterTeamRequest, tournament: Tournament, registeredByUser: String): Team =
        Team(
            tournament = tournament,
            registeredByUser = registeredByUser,
            externalInfo = TeamExternalInfo(
                name = request.name,
                contactName = request.contactName,
                contactEmail = request.contactEmail,
                contactPhone = request.contactPhone
            ),
            internalStats = TeamInternalStats()
        )

    fun toResponse(team: Team): TeamResponse =
        TeamResponse(
            id = team.id,
            tournamentId = team.tournament.id,
            name = team.externalInfo.name,
            contactName = team.externalInfo.contactName,
            contactEmail = team.externalInfo.contactEmail,
            contactPhone = team.externalInfo.contactPhone,
            registeredByUser = team.registeredByUser,
            stats = TeamStatsResponse(
                eliminated = team.internalStats.eliminated,
                matchesPlayed = team.internalStats.matchesPlayed,
                matchesWon = team.internalStats.matchesWon,
                matchesLost = team.internalStats.matchesLost,
                pointsFor = team.internalStats.pointsFor,
                pointsAgainst = team.internalStats.pointsAgainst,
                currentRound = team.internalStats.currentRound
            ),
            createdAt = team.createdAt
        )

    fun toResponseList(teams: List<Team>): List<TeamResponse> = teams.map { toResponse(it) }
}
