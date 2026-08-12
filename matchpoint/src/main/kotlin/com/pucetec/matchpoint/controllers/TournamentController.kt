package com.pucetec.matchpoint.controllers

import com.pucetec.matchpoint.dto.CreateTournamentRequest
import com.pucetec.matchpoint.dto.TournamentProgressResponse
import com.pucetec.matchpoint.dto.TournamentResponse
import com.pucetec.matchpoint.services.TournamentService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tournaments")
class TournamentController(private val tournamentService: TournamentService) {
    @GetMapping
    fun list(): List<TournamentResponse> = tournamentService.listTournaments()

    @GetMapping("/{id}")
    fun progress(@PathVariable id: Long): TournamentProgressResponse =
        tournamentService.getProgress(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateTournamentRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): TournamentResponse =
        tournamentService.createTournament(request, jwt.username())

    @PostMapping("/{id}/start")
    fun start(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): TournamentProgressResponse =
        tournamentService.startTournament(id, jwt.username())

    private fun Jwt.username(): String = getClaimAsString("username")
}
