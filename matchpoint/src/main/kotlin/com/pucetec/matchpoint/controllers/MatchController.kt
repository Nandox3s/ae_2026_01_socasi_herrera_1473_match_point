package com.pucetec.matchpoint.controllers

import com.pucetec.matchpoint.dto.MatchResponse
import com.pucetec.matchpoint.dto.RegisterScoreRequest
import com.pucetec.matchpoint.dto.ScheduleMatchRequest
import com.pucetec.matchpoint.services.TournamentService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MatchController(private val tournamentService: TournamentService) {

    @GetMapping("/tournaments/{tournamentId}/matches")
    fun list(@PathVariable tournamentId: Long): List<MatchResponse> =
        tournamentService.listMatches(tournamentId)

    @GetMapping("/matches/{matchId}")
    fun one(@PathVariable matchId: Long): MatchResponse =
        tournamentService.getMatch(matchId)

    @PatchMapping("/matches/{matchId}/schedule")
    fun schedule(
        @PathVariable matchId: Long,
        @RequestBody request: ScheduleMatchRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): MatchResponse =
        tournamentService.scheduleMatch(matchId, request, jwt.username())

    @PatchMapping("/matches/{matchId}/score")
    fun score(
        @PathVariable matchId: Long,
        @RequestBody request: RegisterScoreRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): MatchResponse =
        tournamentService.registerScore(matchId, request, jwt.username())

    private fun Jwt.username(): String = getClaimAsString("username")
}
