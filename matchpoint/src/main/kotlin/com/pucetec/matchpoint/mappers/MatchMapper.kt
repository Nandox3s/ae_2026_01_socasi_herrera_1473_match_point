package com.pucetec.matchpoint.mappers

import com.pucetec.matchpoint.dto.MatchResponse
import com.pucetec.matchpoint.entities.Match
import org.springframework.stereotype.Component

@Component
class MatchMapper {
    fun toResponse(match: Match): MatchResponse =
        MatchResponse(
            id = match.id,
            tournamentId = match.tournament.id,
            roundNumber = match.roundNumber,
            positionInRound = match.positionInRound,
            homeTeamName = match.homeTeam?.externalInfo?.name,
            awayTeamName = match.awayTeam?.externalInfo?.name,
            homeScore = match.homeScore,
            awayScore = match.awayScore,
            winnerTeamName = match.winnerTeam?.externalInfo?.name,
            status = match.status,
            scheduledAt = match.scheduledAt
        )

    fun toResponseList(matches: List<Match>): List<MatchResponse> = matches.map { toResponse(it) }
}
