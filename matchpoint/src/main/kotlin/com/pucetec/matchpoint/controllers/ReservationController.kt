package com.pucetec.matchpoint.controllers

import com.pucetec.matchpoint.dto.CreateReservationRequest
import com.pucetec.matchpoint.dto.ReservationResponse
import com.pucetec.matchpoint.services.ReservationService
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
@RequestMapping("/reservations")
class ReservationController(private val reservationService: ReservationService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateReservationRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ReservationResponse = reservationService.createReservation(request, jwt.username())

    @GetMapping("/me")
    fun mine(@AuthenticationPrincipal jwt: Jwt): List<ReservationResponse> =
        reservationService.listMine(jwt.username())

    @GetMapping("/{id}")
    fun one(@PathVariable id: Long, @AuthenticationPrincipal jwt: Jwt): ReservationResponse =
        reservationService.getMine(id, jwt.username())

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancel(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ) = reservationService.cancel(id, jwt.username())

    private fun Jwt.username(): String = getClaimAsString("username")
}
