package com.pucetec.matchpoint.mappers

import com.pucetec.matchpoint.dto.CreateTournamentRequest
import com.pucetec.matchpoint.dto.RoundResponse
import com.pucetec.matchpoint.dto.TeamResponse
import com.pucetec.matchpoint.dto.TournamentProgressResponse
import com.pucetec.matchpoint.dto.TournamentResponse
import com.pucetec.matchpoint.entities.Court
import com.pucetec.matchpoint.entities.Tournament
import com.pucetec.matchpoint.enums.TournamentStatus
import org.springframework.stereotype.Component

@Component
class TournamentMapper {
    fun toEntity(request: CreateTournamentRequest, managerUser: String, court: Court?): Tournament =
        Tournament(
            name = request.name,
            sportType = request.sportType,
            maxTeams = request.maxTeams,
            prize = request.prize,
            managerUser = managerUser,
            status = TournamentStatus.REGISTRATION,
            court = court
        )

    fun toResponse(tournament: Tournament, registeredTeams: Int): TournamentResponse =
        TournamentResponse(
            id = tournament.id,
            name = tournament.name,
            sportType = tournament.sportType,
            maxTeams = tournament.maxTeams,
            registeredTeams = registeredTeams,
            prize = tournament.prize,
            status = tournament.status,
            managerUser = tournament.managerUser,
            courtName = tournament.court?.name,
            championTeamName = tournament.championTeam?.externalInfo?.name,
            createdAt = tournament.createdAt
        )

    fun toProgressResponse(
        tournament: Tournament,
        registeredTeams: Int,
        rounds: List<RoundResponse>,
        champion: TeamResponse?
    ): TournamentProgressResponse =
        TournamentProgressResponse(
            tournament = toResponse(tournament, registeredTeams),
            rounds = rounds,
            champion = champion
        )
}
