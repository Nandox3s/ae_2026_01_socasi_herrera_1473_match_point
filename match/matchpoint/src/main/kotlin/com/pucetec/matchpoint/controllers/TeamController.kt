package com.pucetec.matchpoint.controllers

import com.pucetec.matchpoint.dto.RegisterTeamRequest
import com.pucetec.matchpoint.dto.TeamResponse
import com.pucetec.matchpoint.services.TournamentService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tournaments/{tournamentId}/teams")
class TeamController(private val tournamentService: TournamentService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @PathVariable tournamentId: Long,
        @RequestBody request: RegisterTeamRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): TeamResponse =
        tournamentService.registerTeam(tournamentId, request, jwt.username())

    @GetMapping
    fun list(@PathVariable tournamentId: Long): List<TeamResponse> =
        tournamentService.listTeams(tournamentId)

    @GetMapping("/{teamId}")
    fun one(@PathVariable tournamentId: Long, @PathVariable teamId: Long): TeamResponse =
        tournamentService.getTeam(tournamentId, teamId)

    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(
        @PathVariable tournamentId: Long,
        @PathVariable teamId: Long,
        @AuthenticationPrincipal jwt: Jwt
    ) = tournamentService.withdrawTeam(tournamentId, teamId, jwt.username())

    private fun Jwt.username(): String = getClaimAsString("username")
}
