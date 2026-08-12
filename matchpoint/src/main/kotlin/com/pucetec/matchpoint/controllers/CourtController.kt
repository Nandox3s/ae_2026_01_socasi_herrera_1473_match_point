package com.pucetec.matchpoint.controllers

import com.pucetec.matchpoint.dto.CourtResponse
import com.pucetec.matchpoint.dto.CreateCourtRequest
import com.pucetec.matchpoint.dto.UpdateCourtRequest
import com.pucetec.matchpoint.enums.SportType
import com.pucetec.matchpoint.services.CourtService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/courts")
class CourtController(private val courtService: CourtService) {
    @GetMapping
    fun list(
        @RequestParam(required = false) sector: String?,
        @RequestParam(required = false) sport: SportType?
    ): List<CourtResponse> = courtService.listCourts(sector, sport)

    @GetMapping("/{id}")
    fun one(@PathVariable id: Long): CourtResponse = courtService.getCourt(id)

    @GetMapping("/available")
    fun available(
        @RequestParam(required = false) sector: String?,
        @RequestParam(required = false) sport: SportType?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsAt: LocalDateTime?,
        @RequestParam(required = false) durationMinutes: Int?
    ): List<CourtResponse> = courtService.availableCourts(sector, sport, startsAt, durationMinutes)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateCourtRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): CourtResponse = courtService.createCourt(request, jwt.username())

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateCourtRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): CourtResponse = courtService.updateCourt(id, request, jwt.username())

    private fun Jwt.username(): String = getClaimAsString("username")
}
