package com.pucetec.matchpoint.services

import com.pucetec.matchpoint.audit.AuditAction
import com.pucetec.matchpoint.audit.AuditService
import com.pucetec.matchpoint.dto.CreateTournamentRequest
import com.pucetec.matchpoint.dto.RegisterScoreRequest
import com.pucetec.matchpoint.dto.RegisterTeamRequest
import com.pucetec.matchpoint.dto.ScheduleMatchRequest
import com.pucetec.matchpoint.entities.Court
import com.pucetec.matchpoint.entities.Match
import com.pucetec.matchpoint.entities.Team
import com.pucetec.matchpoint.entities.TeamExternalInfo
import com.pucetec.matchpoint.entities.Tournament
import com.pucetec.matchpoint.enums.MatchStatus
import com.pucetec.matchpoint.enums.SportType
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
import com.pucetec.matchpoint.mappers.MatchMapper
import com.pucetec.matchpoint.mappers.TeamMapper
import com.pucetec.matchpoint.mappers.TournamentMapper
import com.pucetec.matchpoint.repositories.CourtRepository
import com.pucetec.matchpoint.repositories.MatchRepository
import com.pucetec.matchpoint.repositories.TeamRepository
import com.pucetec.matchpoint.repositories.TournamentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class TournamentServiceTest {

    private lateinit var tournamentRepository: TournamentRepository
    private lateinit var teamRepository: TeamRepository
    private lateinit var matchRepository: MatchRepository
    private lateinit var courtRepository: CourtRepository
    private lateinit var auditService: AuditService
    private lateinit var service: TournamentService

    private val manager = "manager_josue"
    private val player = "player_fernando"

    @BeforeEach
    fun setUp() {
        tournamentRepository = mock()
        teamRepository = mock()
        matchRepository = mock()
        courtRepository = mock()
        auditService = mock()
        service = TournamentService(
            tournamentRepository,
            teamRepository,
            matchRepository,
            courtRepository,
            TournamentMapper(),
            TeamMapper(),
            MatchMapper(),
            auditService
        )
    }

    // ----------------------------------------------------------------------- helpers

    private fun court(id: Long = 1L, managerUser: String = manager) = Court(
        name = "North Court 1",
        sector = "North",
        hasParking = true,
        sportType = SportType.BASKET,
        floorType = "Concrete",
        pricePerHour = BigDecimal("12.50"),
        managerUser = managerUser,
        id = id
    )

    private fun tournament(
        id: Long = 1L,
        maxTeams: Int = 4,
        status: TournamentStatus = TournamentStatus.REGISTRATION,
        managerUser: String = manager
    ) = Tournament(
        name = "MatchPoint Cup 2026",
        sportType = SportType.BASKET,
        maxTeams = maxTeams,
        prize = "Trophy",
        managerUser = managerUser,
        status = status,
        id = id
    )

    private fun team(id: Long, name: String, tournament: Tournament, registeredBy: String = player) = Team(
        tournament = tournament,
        registeredByUser = registeredBy,
        externalInfo = TeamExternalInfo(name, "Fernando Socasi", "$name@puce.edu.ec", "0999555666"),
        id = id
    )

    private fun teamRequest(
        name: String = "Falcons",
        contactName: String = "Fernando Socasi",
        contactEmail: String = "falcons@puce.edu.ec",
        contactPhone: String = "0999555666"
    ) = RegisterTeamRequest(name, contactName, contactEmail, contactPhone)

    // ------------------------------------------------------------- crear torneo -----

    @Test
    fun `createTournament stores the tournament without a venue`() {
        whenever(tournamentRepository.save(any<Tournament>())).thenAnswer { it.arguments[0] as Tournament }

        val response = service.createTournament(
            CreateTournamentRequest("MatchPoint Cup 2026", SportType.BASKET, 4, "Trophy"), manager
        )

        assertEquals("MatchPoint Cup 2026", response.name)
        assertEquals(manager, response.managerUser)
        assertNull(response.courtName)
        verify(auditService).record(eq("tournaments"), any(), eq(AuditAction.INSERT), anyOrNull(), anyOrNull())
    }

    @Test
    fun `createTournament links an own court as the venue`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court()))
        whenever(tournamentRepository.save(any<Tournament>())).thenAnswer { it.arguments[0] as Tournament }

        val response = service.createTournament(
            CreateTournamentRequest("MatchPoint Cup 2026", SportType.BASKET, 4, "Trophy", courtId = 1L), manager
        )

        assertEquals("North Court 1", response.courtName)
    }

    @Test
    fun `createTournament fails when the venue does not exist`() {
        whenever(courtRepository.findById(9L)).thenReturn(Optional.empty())

        assertThrows<CourtNotFoundException> {
            service.createTournament(
                CreateTournamentRequest("Cup", SportType.BASKET, 4, courtId = 9L), manager
            )
        }
    }

    @Test
    fun `createTournament refuses a venue owned by another manager`() {
        whenever(courtRepository.findById(1L)).thenReturn(Optional.of(court(managerUser = "manager_ana")))

        assertThrows<NotYourCourtException> {
            service.createTournament(
                CreateTournamentRequest("Cup", SportType.BASKET, 4, courtId = 1L), manager
            )
        }
    }

    @Test
    fun `createTournament rejects a blank name`() {
        assertThrows<InvalidTournamentException> {
            service.createTournament(CreateTournamentRequest("  ", SportType.BASKET, 4), manager)
        }
    }

    @Test
    fun `createTournament rejects a cap that is not a power of two`() {
        assertThrows<InvalidTournamentException> {
            service.createTournament(CreateTournamentRequest("Cup", SportType.BASKET, 6), manager)
        }
    }

    @Test
    fun `createTournament rejects a cap below two`() {
        assertThrows<InvalidTournamentException> {
            service.createTournament(CreateTournamentRequest("Cup", SportType.BASKET, 1), manager)
        }
    }

    @Test
    fun `createTournament rejects a cap above thirty two`() {
        assertThrows<InvalidTournamentException> {
            service.createTournament(CreateTournamentRequest("Cup", SportType.BASKET, 64), manager)
        }
    }

    // ------------------------------------------------------------------ listar y ver

    @Test
    fun `listTournaments counts the registered teams of each one`() {
        whenever(tournamentRepository.findAll()).thenReturn(listOf(tournament(), tournament(id = 2L)))
        whenever(teamRepository.countByTournamentId(any())).thenReturn(3L)

        val list = service.listTournaments()

        assertEquals(2, list.size)
        assertEquals(3, list[0].registeredTeams)
    }

    @Test
    fun `getProgress fails when the tournament does not exist`() {
        whenever(tournamentRepository.findById(9L)).thenReturn(Optional.empty())

        assertThrows<TournamentNotFoundException> { service.getProgress(9L) }
    }

    @Test
    fun `getProgress names the rounds of a bracket of thirty two`() {
        val cup = tournament(maxTeams = 32)
        val matches = (1..5).map { round ->
            Match(tournament = cup, roundNumber = round, positionInRound = 0, id = round.toLong())
        }
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(matchRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(1L)).thenReturn(matches)
        whenever(teamRepository.countByTournamentId(1L)).thenReturn(32L)

        val rounds = service.getProgress(1L).rounds

        assertEquals(listOf("Round of 32", "Round of 16", "Quarterfinals", "Semifinals", "Final"), rounds.map { it.roundName })
    }

    @Test
    fun `getProgress falls back to a generic round name on a deeper bracket`() {
        val cup = tournament(maxTeams = 64)
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(matchRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(1L))
            .thenReturn(listOf(Match(tournament = cup, roundNumber = 1, positionInRound = 0, id = 1L)))
        whenever(teamRepository.countByTournamentId(1L)).thenReturn(64L)

        assertEquals("Round 1", service.getProgress(1L).rounds[0].roundName)
    }

    @Test
    fun `getProgress exposes the champion of a finished tournament`() {
        val cup = tournament(maxTeams = 2, status = TournamentStatus.FINISHED)
        val champion = team(6L, "Comets", cup)
        cup.championTeam = champion
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(matchRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(1L)).thenReturn(emptyList())
        whenever(teamRepository.countByTournamentId(1L)).thenReturn(2L)

        assertEquals("Comets", service.getProgress(1L).champion?.name)
    }

    // -------------------------------------------------------------- inscribir equipo

    @Test
    fun `registerTeam stores the team and takes the owner from the token`() {
        val cup = tournament()
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(teamRepository.countByTournamentId(1L)).thenReturn(1L)
        whenever(teamRepository.existsByTournamentIdAndExternalInfoName(1L, "Falcons")).thenReturn(false)
        whenever(teamRepository.save(any<Team>())).thenAnswer { it.arguments[0] as Team }

        val response = service.registerTeam(1L, teamRequest(), player)

        assertEquals("Falcons", response.name)
        assertEquals(player, response.registeredByUser)
        verify(auditService).record(eq("teams"), any(), eq(AuditAction.INSERT), anyOrNull(), anyOrNull())
    }

    @Test
    fun `registerTeam fails when the tournament does not exist`() {
        whenever(tournamentRepository.findById(9L)).thenReturn(Optional.empty())

        assertThrows<TournamentNotFoundException> { service.registerTeam(9L, teamRequest(), player) }
    }

    @Test
    fun `registerTeam is refused once registration is closed`() {
        whenever(tournamentRepository.findById(1L))
            .thenReturn(Optional.of(tournament(status = TournamentStatus.IN_PROGRESS)))

        assertThrows<RegistrationClosedException> { service.registerTeam(1L, teamRequest(), player) }
    }

    @Test
    fun `registerTeam rejects incomplete contact details`() {
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament()))

        assertThrows<InvalidTeamException> { service.registerTeam(1L, teamRequest(name = " "), player) }
        assertThrows<InvalidTeamException> { service.registerTeam(1L, teamRequest(contactName = " "), player) }
        assertThrows<InvalidTeamException> { service.registerTeam(1L, teamRequest(contactEmail = " "), player) }
        assertThrows<InvalidTeamException> { service.registerTeam(1L, teamRequest(contactPhone = " "), player) }
    }

    @Test
    fun `registerTeam is refused when the cap is already reached`() {
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament()))
        whenever(teamRepository.countByTournamentId(1L)).thenReturn(4L)

        assertThrows<TournamentFullException> { service.registerTeam(1L, teamRequest(), player) }
    }

    @Test
    fun `registerTeam refuses a duplicated team name`() {
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament()))
        whenever(teamRepository.countByTournamentId(1L)).thenReturn(1L)
        whenever(teamRepository.existsByTournamentIdAndExternalInfoName(1L, "Falcons")).thenReturn(true)

        assertThrows<DuplicateTeamNameException> { service.registerTeam(1L, teamRequest(), player) }
    }

    @Test
    fun `listTeams returns the teams of the tournament`() {
        val cup = tournament()
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(teamRepository.findByTournamentId(1L))
            .thenReturn(listOf(team(1L, "Falcons", cup), team(2L, "Eagles", cup)))

        assertEquals(2, service.listTeams(1L).size)
    }

    @Test
    fun `getTeam returns the detail of a team`() {
        val cup = tournament()
        whenever(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Falcons", cup)))

        assertEquals("Falcons", service.getTeam(1L, 1L).name)
    }

    @Test
    fun `getTeam fails when the team does not exist`() {
        whenever(teamRepository.findById(9L)).thenReturn(Optional.empty())

        assertThrows<TeamNotFoundException> { service.getTeam(1L, 9L) }
    }

    @Test
    fun `getTeam fails when the team belongs to another tournament`() {
        val other = tournament(id = 2L)
        whenever(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Falcons", other)))

        assertThrows<TeamNotFoundException> { service.getTeam(1L, 1L) }
    }

    // ----------------------------------------------------------------- retirar equipo

    @Test
    fun `withdrawTeam removes an own team while registration is open`() {
        val cup = tournament()
        val squad = team(1L, "Falcons", cup)
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(teamRepository.findById(1L)).thenReturn(Optional.of(squad))

        service.withdrawTeam(1L, 1L, player)

        verify(teamRepository).delete(squad)
        verify(auditService).record(eq("teams"), eq(1L), eq(AuditAction.DELETE), anyOrNull(), anyOrNull())
    }

    @Test
    fun `withdrawTeam is refused once the tournament started`() {
        whenever(tournamentRepository.findById(1L))
            .thenReturn(Optional.of(tournament(status = TournamentStatus.IN_PROGRESS)))

        assertThrows<RegistrationClosedException> { service.withdrawTeam(1L, 1L, player) }
    }

    @Test
    fun `withdrawTeam refuses a team registered by somebody else`() {
        val cup = tournament()
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(teamRepository.findById(1L))
            .thenReturn(Optional.of(team(1L, "Falcons", cup, registeredBy = "player_luis")))

        assertThrows<NotYourTeamException> { service.withdrawTeam(1L, 1L, player) }
    }

    // ---------------------------------------------------------------- arrancar cuadro

    @Test
    fun `startTournament builds the whole bracket`() {
        val cup = tournament(maxTeams = 4)
        val teams = listOf(
            team(1L, "Falcons", cup), team(2L, "Eagles", cup),
            team(3L, "Sharks", cup), team(4L, "Wolves", cup)
        )
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(teamRepository.findByTournamentId(1L)).thenReturn(teams)
        whenever(teamRepository.countByTournamentId(1L)).thenReturn(4L)
        whenever(matchRepository.saveAll(any<List<Match>>())).thenAnswer { it.arguments[0] }
        whenever(matchRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(1L))
            .thenReturn(emptyList())

        val progress = service.startTournament(1L, manager)

        assertEquals(TournamentStatus.IN_PROGRESS, progress.tournament.status)
        // 4 equipos -> 2 semifinales + 1 final
        verify(matchRepository).saveAll(argThatHasSize(3))
        assertEquals(1, teams[0].internalStats.currentRound)
    }

    private fun argThatHasSize(expected: Int): List<Match> =
        org.mockito.kotlin.argThat<List<Match>> { this.size == expected }

    @Test
    fun `startTournament refuses a tournament of another manager`() {
        whenever(tournamentRepository.findById(1L))
            .thenReturn(Optional.of(tournament(managerUser = "manager_ana")))

        assertThrows<NotYourTournamentException> { service.startTournament(1L, manager) }
    }

    @Test
    fun `startTournament refuses a tournament already started`() {
        whenever(tournamentRepository.findById(1L))
            .thenReturn(Optional.of(tournament(status = TournamentStatus.IN_PROGRESS)))

        assertThrows<TournamentNotReadyException> { service.startTournament(1L, manager) }
    }

    @Test
    fun `startTournament refuses an incomplete bracket`() {
        val cup = tournament(maxTeams = 4)
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(teamRepository.findByTournamentId(1L)).thenReturn(listOf(team(1L, "Falcons", cup)))

        assertThrows<TournamentNotReadyException> { service.startTournament(1L, manager) }
    }

    // ------------------------------------------------------------------------ matches

    @Test
    fun `listMatches returns the bracket`() {
        val cup = tournament()
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(matchRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(1L))
            .thenReturn(listOf(Match(tournament = cup, roundNumber = 1, positionInRound = 0, id = 1L)))

        assertEquals(1, service.listMatches(1L).size)
    }

    @Test
    fun `getMatch returns the detail`() {
        val cup = tournament()
        whenever(matchRepository.findById(1L))
            .thenReturn(Optional.of(Match(tournament = cup, roundNumber = 1, positionInRound = 0, id = 1L)))

        assertEquals(1L, service.getMatch(1L).id)
    }

    @Test
    fun `getMatch fails when the match does not exist`() {
        whenever(matchRepository.findById(9L)).thenReturn(Optional.empty())

        assertThrows<MatchNotFoundException> { service.getMatch(9L) }
    }

    @Test
    fun `scheduleMatch stores the date`() {
        val cup = tournament()
        val match = Match(tournament = cup, roundNumber = 1, positionInRound = 0, status = MatchStatus.READY, id = 1L)
        whenever(matchRepository.findById(1L)).thenReturn(Optional.of(match))
        whenever(matchRepository.save(any<Match>())).thenAnswer { it.arguments[0] as Match }

        val when1 = LocalDateTime.of(2026, 8, 20, 19, 0)
        assertEquals(when1, service.scheduleMatch(1L, ScheduleMatchRequest(when1), manager).scheduledAt)
        verify(auditService).record(eq("matches"), eq(1L), eq(AuditAction.UPDATE), anyOrNull(), anyOrNull())
    }

    @Test
    fun `scheduleMatch refuses a tournament of another manager`() {
        val cup = tournament(managerUser = "manager_ana")
        whenever(matchRepository.findById(1L))
            .thenReturn(Optional.of(Match(tournament = cup, roundNumber = 1, positionInRound = 0, id = 1L)))

        assertThrows<NotYourTournamentException> {
            service.scheduleMatch(1L, ScheduleMatchRequest(LocalDateTime.now()), manager)
        }
    }

    @Test
    fun `scheduleMatch refuses a match already played`() {
        val cup = tournament()
        whenever(matchRepository.findById(1L)).thenReturn(
            Optional.of(
                Match(tournament = cup, roundNumber = 1, positionInRound = 0, status = MatchStatus.PLAYED, id = 1L)
            )
        )

        assertThrows<MatchAlreadyPlayedException> {
            service.scheduleMatch(1L, ScheduleMatchRequest(LocalDateTime.now()), manager)
        }
    }

    // ------------------------------------------------------------------- marcadores

    private fun readyMatch(
        cup: Tournament,
        round: Int,
        position: Int,
        home: Team,
        away: Team,
        id: Long = 1L
    ) = Match(
        tournament = cup,
        roundNumber = round,
        positionInRound = position,
        homeTeam = home,
        awayTeam = away,
        status = MatchStatus.READY,
        id = id
    )

    @Test
    fun `registerScore declares the champion when the final is played`() {
        val cup = tournament(maxTeams = 2)
        val home = team(1L, "Comets", cup)
        val away = team(2L, "Meteors", cup)
        val final = readyMatch(cup, 1, 0, home, away)
        whenever(matchRepository.findById(1L)).thenReturn(Optional.of(final))
        whenever(matchRepository.save(any<Match>())).thenAnswer { it.arguments[0] as Match }

        val response = service.registerScore(1L, RegisterScoreRequest(30, 24), manager)

        assertEquals("Comets", response.winnerTeamName)
        assertEquals(MatchStatus.PLAYED, response.status)
        assertEquals(TournamentStatus.FINISHED, cup.status)
        assertEquals("Comets", cup.championTeam?.externalInfo?.name)
        assertTrue(away.internalStats.eliminated)
        assertEquals(1, home.internalStats.matchesWon)
        assertEquals(30, home.internalStats.pointsFor)
    }

    @Test
    fun `registerScore moves the winner of an even slot to the home side of the next round`() {
        val cup = tournament(maxTeams = 4)
        val home = team(1L, "Falcons", cup)
        val away = team(2L, "Eagles", cup)
        val semifinal = readyMatch(cup, 1, 0, home, away)
        val final = Match(tournament = cup, roundNumber = 2, positionInRound = 0, id = 3L)
        whenever(matchRepository.findById(1L)).thenReturn(Optional.of(semifinal))
        whenever(matchRepository.findByTournamentIdAndRoundNumberAndPositionInRound(1L, 2, 0)).thenReturn(final)
        whenever(matchRepository.save(any<Match>())).thenAnswer { it.arguments[0] as Match }

        service.registerScore(1L, RegisterScoreRequest(30, 24), manager)

        assertEquals("Falcons", final.homeTeam?.externalInfo?.name)
        assertNull(final.awayTeam)
        assertEquals(MatchStatus.PENDING, final.status)
        assertEquals(2, home.internalStats.currentRound)
    }

    @Test
    fun `registerScore moves the winner of an odd slot to the away side and readies the match`() {
        val cup = tournament(maxTeams = 4)
        val alreadyThere = team(1L, "Falcons", cup)
        val home = team(3L, "Sharks", cup)
        val away = team(4L, "Wolves", cup)
        val semifinal = readyMatch(cup, 1, 1, home, away, id = 2L)
        val final = Match(
            tournament = cup, roundNumber = 2, positionInRound = 0, homeTeam = alreadyThere, id = 3L
        )
        whenever(matchRepository.findById(2L)).thenReturn(Optional.of(semifinal))
        whenever(matchRepository.findByTournamentIdAndRoundNumberAndPositionInRound(1L, 2, 0)).thenReturn(final)
        whenever(matchRepository.save(any<Match>())).thenAnswer { it.arguments[0] as Match }

        service.registerScore(2L, RegisterScoreRequest(18, 25), manager)

        assertEquals("Wolves", final.awayTeam?.externalInfo?.name)
        assertEquals(MatchStatus.READY, final.status)
        assertTrue(home.internalStats.eliminated)
    }

    @Test
    fun `registerScore fails when the next bracket slot is missing`() {
        val cup = tournament(maxTeams = 4)
        val semifinal = readyMatch(cup, 1, 0, team(1L, "Falcons", cup), team(2L, "Eagles", cup))
        whenever(matchRepository.findById(1L)).thenReturn(Optional.of(semifinal))
        whenever(matchRepository.findByTournamentIdAndRoundNumberAndPositionInRound(1L, 2, 0)).thenReturn(null)

        assertThrows<MatchNotFoundException> { service.registerScore(1L, RegisterScoreRequest(30, 24), manager) }
    }

    @Test
    fun `registerScore refuses a tournament of another manager`() {
        val cup = tournament(maxTeams = 2, managerUser = "manager_ana")
        val match = readyMatch(cup, 1, 0, team(1L, "Comets", cup), team(2L, "Meteors", cup))
        whenever(matchRepository.findById(1L)).thenReturn(Optional.of(match))

        assertThrows<NotYourTournamentException> { service.registerScore(1L, RegisterScoreRequest(30, 24), manager) }
    }

    @Test
    fun `registerScore refuses a match without both teams`() {
        val cup = tournament(maxTeams = 4)
        whenever(matchRepository.findById(1L))
            .thenReturn(Optional.of(Match(tournament = cup, roundNumber = 2, positionInRound = 0, id = 1L)))

        assertThrows<MatchNotReadyException> { service.registerScore(1L, RegisterScoreRequest(30, 24), manager) }
    }

    @Test
    fun `registerScore refuses a match that already has a score`() {
        val cup = tournament(maxTeams = 2)
        val match = readyMatch(cup, 1, 0, team(1L, "Comets", cup), team(2L, "Meteors", cup))
        match.status = MatchStatus.PLAYED
        whenever(matchRepository.findById(1L)).thenReturn(Optional.of(match))

        assertThrows<MatchAlreadyPlayedException> { service.registerScore(1L, RegisterScoreRequest(30, 24), manager) }
    }

    @Test
    fun `registerScore rejects negative scores`() {
        val cup = tournament(maxTeams = 2)
        val match = readyMatch(cup, 1, 0, team(1L, "Comets", cup), team(2L, "Meteors", cup))
        whenever(matchRepository.findById(1L)).thenReturn(Optional.of(match))

        assertThrows<InvalidTournamentException> { service.registerScore(1L, RegisterScoreRequest(-1, 10), manager) }
        assertThrows<InvalidTournamentException> { service.registerScore(1L, RegisterScoreRequest(10, -1), manager) }
    }

    @Test
    fun `registerScore rejects a tie because the bracket is single elimination`() {
        val cup = tournament(maxTeams = 2)
        val match = readyMatch(cup, 1, 0, team(1L, "Comets", cup), team(2L, "Meteors", cup))
        whenever(matchRepository.findById(1L)).thenReturn(Optional.of(match))

        assertThrows<TieNotAllowedException> { service.registerScore(1L, RegisterScoreRequest(20, 20), manager) }
    }

    @Test
    fun `the mapper exposes the whole progress object`() {
        val cup = tournament(maxTeams = 2, status = TournamentStatus.FINISHED)
        whenever(tournamentRepository.findById(1L)).thenReturn(Optional.of(cup))
        whenever(matchRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(1L)).thenReturn(emptyList())
        whenever(teamRepository.countByTournamentId(1L)).thenReturn(2L)

        val progress = service.getProgress(1L)

        assertNotNull(progress.tournament)
        assertTrue(progress.rounds.isEmpty())
    }
}
