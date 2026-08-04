package com.pucetec.matchpoint.services

import com.pucetec.matchpoint.audit.AuditAction
import com.pucetec.matchpoint.audit.AuditService
import com.pucetec.matchpoint.dto.CreateTournamentRequest
import com.pucetec.matchpoint.dto.MatchResponse
import com.pucetec.matchpoint.dto.RegisterScoreRequest
import com.pucetec.matchpoint.dto.RegisterTeamRequest
import com.pucetec.matchpoint.dto.RoundResponse
import com.pucetec.matchpoint.dto.ScheduleMatchRequest
import com.pucetec.matchpoint.dto.TeamResponse
import com.pucetec.matchpoint.dto.TournamentProgressResponse
import com.pucetec.matchpoint.dto.TournamentResponse
import com.pucetec.matchpoint.entities.Court
import com.pucetec.matchpoint.entities.Match
import com.pucetec.matchpoint.entities.Team
import com.pucetec.matchpoint.entities.Tournament
import com.pucetec.matchpoint.enums.MatchStatus
import com.pucetec.matchpoint.enums.TournamentStatus
import com.pucetec.matchpoint.exceptions.CourtNotFoundException
import com.pucetec.matchpoint.exceptions.DuplicateTeamNameException
import com.pucetec.matchpoint.exceptions.InvalidTeamException
import com.pucetec.matchpoint.exceptions.InvalidTournamentException
import com.pucetec.matchpoint.exceptions.MatchAlreadyPlayedException
import com.pucetec.matchpoint.exceptions.MatchNotFoundException
import com.pucetec.matchpoint.exceptions.MatchNotReadyException
import com.pucetec.matchpoint.exceptions.NotYourCourtException
import com.pucetec.matchpoint.exceptions.NotYourTeamException
import com.pucetec.matchpoint.exceptions.NotYourTournamentException
import com.pucetec.matchpoint.exceptions.RegistrationClosedException
import com.pucetec.matchpoint.exceptions.TeamNotFoundException
import com.pucetec.matchpoint.exceptions.TieNotAllowedException
import com.pucetec.matchpoint.exceptions.TournamentFullException
import com.pucetec.matchpoint.exceptions.TournamentNotFoundException
import com.pucetec.matchpoint.exceptions.TournamentNotReadyException
import com.pucetec.matchpoint.logging.logLine
import com.pucetec.matchpoint.logging.maskEmail
import com.pucetec.matchpoint.mappers.MatchMapper
import com.pucetec.matchpoint.mappers.TeamMapper
import com.pucetec.matchpoint.mappers.TournamentMapper
import com.pucetec.matchpoint.repositories.CourtRepository
import com.pucetec.matchpoint.repositories.MatchRepository
import com.pucetec.matchpoint.repositories.TeamRepository
import com.pucetec.matchpoint.repositories.TournamentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TournamentService(
    private val tournamentRepository: TournamentRepository,
    private val teamRepository: TeamRepository,
    private val matchRepository: MatchRepository,
    private val courtRepository: CourtRepository,
    private val tournamentMapper: TournamentMapper,
    private val teamMapper: TeamMapper,
    private val matchMapper: MatchMapper,
    private val auditService: AuditService
) {

    private val logger = LoggerFactory.getLogger(TournamentService::class.java)

    private val maxTeamsLimit = 32

    fun createTournament(request: CreateTournamentRequest, managerUser: String): TournamentResponse {
        if (request.name.isBlank()) {
            throw InvalidTournamentException("Tournament name is required")
        }
        if (!isPowerOfTwo(request.maxTeams) || request.maxTeams < 2 || request.maxTeams > maxTeamsLimit) {
            throw InvalidTournamentException(
                "maxTeams must be a power of two between 2 and $maxTeamsLimit (2, 4, 8, 16, 32); received ${request.maxTeams}"
            )
        }
        val court = resolveCourt(request.courtId, managerUser)
        val saved = tournamentRepository.save(tournamentMapper.toEntity(request, managerUser, court))
        auditService.record(TOURNAMENTS, saved.id, AuditAction.INSERT, newValues = describe(saved))
        logger.info(
            logLine(
                "tournament.created",
                "Tournament created",
                "tournamentId" to saved.id,
                "maxTeams" to saved.maxTeams,
                "managerUser" to managerUser
            )
        )
        return tournamentMapper.toResponse(saved, registeredTeams = 0)
    }

    private fun resolveCourt(courtId: Long?, managerUser: String): Court? {
        if (courtId == null) return null
        val court = courtRepository.findById(courtId)
            .orElseThrow { CourtNotFoundException("Court $courtId was not found") }
        if (court.managerUser != managerUser) {
            throw NotYourCourtException("Court $courtId does not belong to you")
        }
        return court
    }

    fun listTournaments(): List<TournamentResponse> {
        val tournaments = tournamentRepository.findAll()
        logger.debug(logLine("tournament.listed", "Tournaments listed", "total" to tournaments.size))
        return tournaments.map { tournamentMapper.toResponse(it, countTeams(it.id)) }
    }

    fun getProgress(tournamentId: Long): TournamentProgressResponse {
        val tournament = findTournament(tournamentId)
        val matches = matchRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(tournamentId)
        val rounds = buildRounds(matches, totalRounds(tournament.maxTeams))
        val champion = tournament.championTeam?.let { teamMapper.toResponse(it) }
        return tournamentMapper.toProgressResponse(tournament, countTeams(tournamentId), rounds, champion)
    }

    fun registerTeam(tournamentId: Long, request: RegisterTeamRequest, playerUser: String): TeamResponse {
        val tournament = findTournament(tournamentId)

        if (tournament.status != TournamentStatus.REGISTRATION) {
            throw RegistrationClosedException("Registration for tournament $tournamentId is closed")
        }
        if (request.name.isBlank() || request.contactName.isBlank() ||
            request.contactEmail.isBlank() || request.contactPhone.isBlank()
        ) {
            throw InvalidTeamException("Team name and contact details are required")
        }
        if (teamRepository.countByTournamentId(tournamentId) >= tournament.maxTeams) {
            throw TournamentFullException(
                "Tournament $tournamentId already reached its cap of ${tournament.maxTeams} teams"
            )
        }
        if (teamRepository.existsByTournamentIdAndExternalInfoName(tournamentId, request.name)) {
            throw DuplicateTeamNameException("Team '${request.name}' already exists in tournament $tournamentId")
        }

        val saved = teamRepository.save(teamMapper.toEntity(request, tournament, playerUser))
        auditService.record(TEAMS, saved.id, AuditAction.INSERT, newValues = describe(saved))
        logger.info(
            logLine(
                "team.registered",
                "Team registered",
                "teamId" to saved.id,
                "tournamentId" to tournamentId,
                "name" to saved.externalInfo.name,
                "contactEmail" to maskEmail(saved.externalInfo.contactEmail)
            )
        )
        return teamMapper.toResponse(saved)
    }

    fun listTeams(tournamentId: Long): List<TeamResponse> {
        findTournament(tournamentId)
        val teams = teamRepository.findByTournamentId(tournamentId)
        logger.debug(logLine("team.listed", "Teams listed", "tournamentId" to tournamentId, "total" to teams.size))
        return teamMapper.toResponseList(teams)
    }

    fun getTeam(tournamentId: Long, teamId: Long): TeamResponse =
        teamMapper.toResponse(findTeamInTournament(tournamentId, teamId))

    fun withdrawTeam(tournamentId: Long, teamId: Long, playerUser: String) {
        val tournament = findTournament(tournamentId)
        if (tournament.status != TournamentStatus.REGISTRATION) {
            throw RegistrationClosedException("Teams cannot be withdrawn once tournament $tournamentId started")
        }
        val team = findTeamInTournament(tournamentId, teamId)
        if (team.registeredByUser != playerUser) {
            throw NotYourTeamException("Team $teamId was not registered by you")
        }
        val previous = describe(team)
        teamRepository.delete(team)
        auditService.record(TEAMS, teamId, AuditAction.DELETE, oldValues = previous)
        logger.info(
            logLine(
                "team.withdrawn",
                "Team withdrawn",
                "teamId" to teamId,
                "tournamentId" to tournamentId
            )
        )
    }

    fun startTournament(tournamentId: Long, managerUser: String): TournamentProgressResponse {
        val tournament = findTournamentOwnedBy(tournamentId, managerUser)

        if (tournament.status != TournamentStatus.REGISTRATION) {
            throw TournamentNotReadyException("Tournament $tournamentId was already started or finished")
        }
        val teams = teamRepository.findByTournamentId(tournamentId).sortedBy { it.id }
        if (teams.size != tournament.maxTeams) {
            throw TournamentNotReadyException(
                "Tournament needs exactly ${tournament.maxTeams} teams to start (registered: ${teams.size})"
            )
        }

        val total = totalRounds(tournament.maxTeams)
        val matches = mutableListOf<Match>()

        for (position in 0 until tournament.maxTeams / 2) {
            val home = teams[position * 2]
            val away = teams[position * 2 + 1]
            home.internalStats.currentRound = 1
            away.internalStats.currentRound = 1
            matches.add(
                Match(
                    tournament = tournament,
                    roundNumber = 1,
                    positionInRound = position,
                    homeTeam = home,
                    awayTeam = away,
                    status = MatchStatus.READY
                )
            )
        }

        for (round in 2..total) {
            for (position in 0 until tournament.maxTeams / (1 shl round)) {
                matches.add(
                    Match(
                        tournament = tournament,
                        roundNumber = round,
                        positionInRound = position,
                        status = MatchStatus.PENDING
                    )
                )
            }
        }

        teamRepository.saveAll(teams)
        matchRepository.saveAll(matches)
        val previous = describe(tournament)
        tournament.status = TournamentStatus.IN_PROGRESS
        tournamentRepository.save(tournament)
        auditService.record(
            TOURNAMENTS,
            tournamentId,
            AuditAction.UPDATE,
            oldValues = previous,
            newValues = describe(tournament)
        )

        logger.info(
            logLine(
                "tournament.started",
                "Bracket generated",
                "tournamentId" to tournamentId,
                "rounds" to total,
                "matches" to matches.size
            )
        )
        return getProgress(tournamentId)
    }

    fun listMatches(tournamentId: Long): List<MatchResponse> {
        findTournament(tournamentId)
        val matches = matchRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(tournamentId)
        logger.debug(logLine("match.listed", "Matches listed", "tournamentId" to tournamentId, "total" to matches.size))
        return matchMapper.toResponseList(matches)
    }

    fun getMatch(matchId: Long): MatchResponse = matchMapper.toResponse(findMatch(matchId))

    fun scheduleMatch(matchId: Long, request: ScheduleMatchRequest, managerUser: String): MatchResponse {
        val match = findMatch(matchId)
        if (match.tournament.managerUser != managerUser) {
            throw NotYourTournamentException("Tournament ${match.tournament.id} does not belong to you")
        }
        if (match.status == MatchStatus.PLAYED) {
            throw MatchAlreadyPlayedException("Match $matchId was already played and cannot be rescheduled")
        }
        val previous = describe(match)
        match.scheduledAt = request.scheduledAt
        val saved = matchRepository.save(match)
        auditService.record(
            MATCHES,
            saved.id,
            AuditAction.UPDATE,
            oldValues = previous,
            newValues = describe(saved)
        )
        logger.info(
            logLine(
                "match.scheduled",
                "Match scheduled",
                "matchId" to matchId,
                "scheduledAt" to request.scheduledAt
            )
        )
        return matchMapper.toResponse(saved)
    }

    fun registerScore(matchId: Long, request: RegisterScoreRequest, managerUser: String): MatchResponse {
        val match = findMatch(matchId)

        val tournament = match.tournament
        if (tournament.managerUser != managerUser) {
            throw NotYourTournamentException("Tournament ${tournament.id} does not belong to you")
        }
        when (match.status) {
            MatchStatus.PENDING -> throw MatchNotReadyException("Match $matchId does not have both teams yet")
            MatchStatus.PLAYED -> throw MatchAlreadyPlayedException("Match $matchId already has a score")
            MatchStatus.READY -> Unit
        }
        if (request.homeScore < 0 || request.awayScore < 0) {
            throw InvalidTournamentException("Scores cannot be negative")
        }
        if (request.homeScore == request.awayScore) {
            throw TieNotAllowedException("Ties are not allowed in a single elimination bracket")
        }

        val previous = describe(match)
        val home = match.homeTeam!!
        val away = match.awayTeam!!
        match.homeScore = request.homeScore
        match.awayScore = request.awayScore

        val homeWins = request.homeScore > request.awayScore
        val winner = if (homeWins) home else away
        val loser = if (homeWins) away else home

        applyStats(home, pointsFor = request.homeScore, pointsAgainst = request.awayScore)
        applyStats(away, pointsFor = request.awayScore, pointsAgainst = request.homeScore)
        winner.internalStats.matchesWon += 1
        loser.internalStats.matchesLost += 1
        loser.internalStats.eliminated = true

        match.winnerTeam = winner
        match.status = MatchStatus.PLAYED

        if (match.roundNumber == totalRounds(tournament.maxTeams)) {
            tournament.championTeam = winner
            tournament.status = TournamentStatus.FINISHED
            tournamentRepository.save(tournament)
            logger.info(
                logLine(
                    "tournament.finished",
                    "Tournament finished",
                    "tournamentId" to tournament.id,
                    "champion" to winner.externalInfo.name
                )
            )
        } else {
            advanceWinner(tournament, match.roundNumber, match.positionInRound, winner)
        }

        teamRepository.saveAll(listOf(home, away))
        val saved = matchRepository.save(match)
        auditService.record(
            MATCHES,
            saved.id,
            AuditAction.UPDATE,
            oldValues = previous,
            newValues = describe(saved)
        )
        logger.info(
            logLine(
                "match.scored",
                "Score registered",
                "matchId" to matchId,
                "homeScore" to request.homeScore,
                "awayScore" to request.awayScore,
                "winner" to winner.externalInfo.name
            )
        )
        return matchMapper.toResponse(saved)
    }

    private fun findTournament(tournamentId: Long): Tournament =
        tournamentRepository.findById(tournamentId)
            .orElseThrow { TournamentNotFoundException("Tournament $tournamentId was not found") }

    private fun findMatch(matchId: Long): Match =
        matchRepository.findById(matchId)
            .orElseThrow { MatchNotFoundException("Match $matchId was not found") }

    private fun findTeamInTournament(tournamentId: Long, teamId: Long): Team {
        val team = teamRepository.findById(teamId)
            .orElseThrow { TeamNotFoundException("Team $teamId was not found") }
        if (team.tournament.id != tournamentId) {
            throw TeamNotFoundException("Team $teamId does not belong to tournament $tournamentId")
        }
        return team
    }

    private fun findTournamentOwnedBy(tournamentId: Long, managerUser: String): Tournament {
        val tournament = findTournament(tournamentId)
        if (tournament.managerUser != managerUser) {
            throw NotYourTournamentException("Tournament $tournamentId does not belong to you")
        }
        return tournament
    }

    private fun advanceWinner(tournament: Tournament, round: Int, position: Int, winner: Team) {
        val parentRound = round + 1
        val parentPosition = position / 2
        val goesHome = position % 2 == 0

        val parent = matchRepository.findByTournamentIdAndRoundNumberAndPositionInRound(
            tournament.id, parentRound, parentPosition
        ) ?: throw MatchNotFoundException(
            "The next bracket slot (round $parentRound, position $parentPosition) of tournament ${tournament.id} does not exist"
        )

        if (goesHome) parent.homeTeam = winner else parent.awayTeam = winner
        winner.internalStats.currentRound = parentRound
        if (parent.homeTeam != null && parent.awayTeam != null) {
            parent.status = MatchStatus.READY
        }
        matchRepository.save(parent)
        logger.info(
            logLine(
                "match.advanced",
                "Winner moved to the next round",
                "matchId" to parent.id,
                "roundNumber" to parentRound,
                "team" to winner.externalInfo.name
            )
        )
    }

    private fun buildRounds(matches: List<Match>, total: Int): List<RoundResponse> =
        matches.groupBy { it.roundNumber }
            .toSortedMap()
            .map { (roundNumber, roundMatches) ->
                RoundResponse(
                    roundNumber = roundNumber,
                    roundName = roundName(roundNumber, total),
                    matches = matchMapper.toResponseList(roundMatches.sortedBy { it.positionInRound })
                )
            }

    private fun roundName(roundNumber: Int, total: Int): String = when (total - roundNumber) {
        0 -> "Final"
        1 -> "Semifinals"
        2 -> "Quarterfinals"
        3 -> "Round of 16"
        4 -> "Round of 32"
        else -> "Round $roundNumber"
    }

    private fun applyStats(team: Team, pointsFor: Int, pointsAgainst: Int) {
        team.internalStats.matchesPlayed += 1
        team.internalStats.pointsFor += pointsFor
        team.internalStats.pointsAgainst += pointsAgainst
    }

    private fun countTeams(tournamentId: Long): Int =
        teamRepository.countByTournamentId(tournamentId).toInt()

    private fun totalRounds(maxTeams: Int): Int = Integer.numberOfTrailingZeros(maxTeams)

    private fun isPowerOfTwo(value: Int): Boolean = value > 0 && (value and (value - 1)) == 0

    private fun describe(tournament: Tournament): String =
        "name=${tournament.name} maxTeams=${tournament.maxTeams} status=${tournament.status} " +
            "managerUser=${tournament.managerUser}"

    private fun describe(team: Team): String =
        "name=${team.externalInfo.name} registeredByUser=${team.registeredByUser} " +
            "contactEmail=${maskEmail(team.externalInfo.contactEmail)} " +
            "eliminated=${team.internalStats.eliminated}"

    private fun describe(match: Match): String =
        "roundNumber=${match.roundNumber} positionInRound=${match.positionInRound} " +
            "homeScore=${match.homeScore} awayScore=${match.awayScore} status=${match.status} " +
            "scheduledAt=${match.scheduledAt}"

    private companion object {
        const val TOURNAMENTS = "tournaments"
        const val TEAMS = "teams"
        const val MATCHES = "matches"
    }
}
