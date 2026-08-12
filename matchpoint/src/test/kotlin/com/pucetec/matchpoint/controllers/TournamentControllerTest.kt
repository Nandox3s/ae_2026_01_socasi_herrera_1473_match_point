package com.pucetec.matchpoint.controllers

import com.pucetec.matchpoint.config.SecurityConfig
import com.pucetec.matchpoint.dto.MatchResponse
import com.pucetec.matchpoint.dto.RoundResponse
import com.pucetec.matchpoint.dto.TeamResponse
import com.pucetec.matchpoint.dto.TeamStatsResponse
import com.pucetec.matchpoint.dto.TournamentProgressResponse
import com.pucetec.matchpoint.dto.TournamentResponse
import com.pucetec.matchpoint.enums.MatchStatus
import com.pucetec.matchpoint.enums.SportType
import com.pucetec.matchpoint.enums.TournamentStatus
import com.pucetec.matchpoint.exceptions.GlobalExceptionHandler
import com.pucetec.matchpoint.exceptions.RegistrationClosedException
import com.pucetec.matchpoint.exceptions.TieNotAllowedException
import com.pucetec.matchpoint.exceptions.TournamentNotFoundException
import com.pucetec.matchpoint.services.TournamentService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(TournamentController::class, TeamController::class, MatchController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class TournamentControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var tournamentService: TournamentService

    private fun manager() = jwt()
        .authorities(SimpleGrantedAuthority("ROLE_MANAGER"))
        .jwt { it.subject("sub-manager").claim("username", "manager_josue") }

    private fun player() = jwt()
        .authorities(SimpleGrantedAuthority("ROLE_PLAYER"))
        .jwt { it.subject("sub-player").claim("username", "player_fernando") }

    private val tournamentBody = """{"name":"MatchPoint Cup 2026","sportType":"BASKET","maxTeams":4}"""
    private val teamBody =
        """{"name":"Falcons","contactName":"Fernando Socasi","contactEmail":"falcons@puce.edu.ec","contactPhone":"0999555666"}"""

    private fun sampleTournament() = TournamentResponse(
        id = 1, name = "MatchPoint Cup 2026", sportType = SportType.BASKET, maxTeams = 4,
        registeredTeams = 0, prize = "Trophy", status = TournamentStatus.REGISTRATION,
        managerUser = "manager_josue", courtName = null, championTeamName = null,
        createdAt = LocalDateTime.of(2026, 7, 6, 9, 0)
    )

    private fun sampleTeam() = TeamResponse(
        id = 1, tournamentId = 1, name = "Falcons", contactName = "Fernando Socasi",
        contactEmail = "falcons@puce.edu.ec", contactPhone = "0999555666",
        registeredByUser = "player_fernando", stats = TeamStatsResponse(false, 0, 0, 0, 0, 0, 0),
        createdAt = LocalDateTime.of(2026, 7, 7, 10, 0)
    )

    private fun sampleMatch() = MatchResponse(
        id = 1, tournamentId = 1, roundNumber = 1, positionInRound = 0,
        homeTeamName = "Falcons", awayTeamName = "Eagles", homeScore = 30, awayScore = 24,
        winnerTeamName = "Falcons", status = MatchStatus.PLAYED, scheduledAt = null
    )

    private fun sampleProgress() = TournamentProgressResponse(
        tournament = sampleTournament(),
        rounds = listOf(RoundResponse(1, "Final", listOf(sampleMatch()))),
        champion = null
    )

    @Test
    fun `GET tournaments is public`() {
        whenever(tournamentService.listTournaments()).thenReturn(listOf(sampleTournament()))

        mockMvc.perform(get("/tournaments"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("MatchPoint Cup 2026"))
    }

    @Test
    fun `GET tournament progress is public`() {
        whenever(tournamentService.getProgress(1L)).thenReturn(sampleProgress())

        mockMvc.perform(get("/tournaments/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rounds[0].roundName").value("Final"))
    }

    @Test
    fun `POST tournaments without a token returns 401`() {
        mockMvc.perform(post("/tournaments").contentType(MediaType.APPLICATION_JSON).content(tournamentBody))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST tournaments with PLAYER returns 403`() {
        mockMvc.perform(
            post("/tournaments").with(player()).contentType(MediaType.APPLICATION_JSON).content(tournamentBody)
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST tournaments with MANAGER returns 201`() {
        whenever(tournamentService.createTournament(any(), any())).thenReturn(sampleTournament())

        mockMvc.perform(
            post("/tournaments").with(manager()).contentType(MediaType.APPLICATION_JSON).content(tournamentBody)
        ).andExpect(status().isCreated)

        verify(tournamentService).createTournament(any(), eq("manager_josue"))
    }

    @Test
    fun `POST start with MANAGER returns 200`() {
        whenever(tournamentService.startTournament(1L, "manager_josue")).thenReturn(sampleProgress())

        mockMvc.perform(post("/tournaments/1/start").with(manager())).andExpect(status().isOk)
    }

    @Test
    fun `POST start with PLAYER returns 403`() {
        mockMvc.perform(post("/tournaments/1/start").with(player())).andExpect(status().isForbidden)
    }

    @Test
    fun `GET teams is public`() {
        whenever(tournamentService.listTeams(1L)).thenReturn(listOf(sampleTeam()))

        mockMvc.perform(get("/tournaments/1/teams")).andExpect(status().isOk)
    }

    @Test
    fun `GET team detail is public`() {
        whenever(tournamentService.getTeam(1L, 1L)).thenReturn(sampleTeam())

        mockMvc.perform(get("/tournaments/1/teams/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Falcons"))
    }

    @Test
    fun `POST team with MANAGER returns 403`() {
        mockMvc.perform(
            post("/tournaments/1/teams").with(manager()).contentType(MediaType.APPLICATION_JSON).content(teamBody)
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST team with PLAYER returns 201`() {
        whenever(tournamentService.registerTeam(any(), any(), any())).thenReturn(sampleTeam())

        mockMvc.perform(
            post("/tournaments/1/teams").with(player()).contentType(MediaType.APPLICATION_JSON).content(teamBody)
        ).andExpect(status().isCreated)

        verify(tournamentService).registerTeam(eq(1L), any(), eq("player_fernando"))
    }

    @Test
    fun `DELETE team with MANAGER returns 403`() {
        mockMvc.perform(delete("/tournaments/1/teams/1").with(manager())).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE team with PLAYER returns 204`() {
        mockMvc.perform(delete("/tournaments/1/teams/1").with(player())).andExpect(status().isNoContent)

        verify(tournamentService).withdrawTeam(1L, 1L, "player_fernando")
    }

    @Test
    fun `GET the bracket is public`() {
        whenever(tournamentService.listMatches(1L)).thenReturn(listOf(sampleMatch()))

        mockMvc.perform(get("/tournaments/1/matches")).andExpect(status().isOk)
    }

    @Test
    fun `GET match detail is public`() {
        whenever(tournamentService.getMatch(1L)).thenReturn(sampleMatch())

        mockMvc.perform(get("/matches/1")).andExpect(status().isOk)
    }

    @Test
    fun `PATCH schedule with MANAGER returns 200`() {
        whenever(tournamentService.scheduleMatch(any(), any(), any())).thenReturn(sampleMatch())

        mockMvc.perform(
            patch("/matches/1/schedule").with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"scheduledAt":"2026-08-20T19:00:00"}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `PATCH schedule with PLAYER returns 403`() {
        mockMvc.perform(
            patch("/matches/1/schedule").with(player()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"scheduledAt":"2026-08-20T19:00:00"}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `PATCH score with MANAGER returns 200`() {
        whenever(tournamentService.registerScore(any(), any(), any())).thenReturn(sampleMatch())

        mockMvc.perform(
            patch("/matches/1/score").with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"homeScore":30,"awayScore":24}""")
        ).andExpect(status().isOk)

        verify(tournamentService).registerScore(eq(1L), any(), eq("manager_josue"))
    }

    @Test
    fun `PATCH score with PLAYER returns 403`() {
        mockMvc.perform(
            patch("/matches/1/score").with(player()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"homeScore":30,"awayScore":24}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `a missing tournament is answered with 404`() {
        whenever(tournamentService.getProgress(9L)).thenThrow(TournamentNotFoundException("Tournament 9 was not found"))

        mockMvc.perform(get("/tournaments/9"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Tournament 9 was not found"))
    }

    @Test
    fun `a closed registration is answered with 409`() {
        whenever(tournamentService.registerTeam(any(), any(), any()))
            .thenThrow(RegistrationClosedException("Registration for tournament 1 is closed"))

        mockMvc.perform(
            post("/tournaments/1/teams").with(player()).contentType(MediaType.APPLICATION_JSON).content(teamBody)
        ).andExpect(status().isConflict)
    }

    @Test
    fun `a tie is answered with 400`() {
        whenever(tournamentService.registerScore(any(), any(), any()))
            .thenThrow(TieNotAllowedException("Ties are not allowed in a single elimination bracket"))

        mockMvc.perform(
            patch("/matches/1/score").with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"homeScore":20,"awayScore":20}""")
        ).andExpect(status().isBadRequest)
    }
}
