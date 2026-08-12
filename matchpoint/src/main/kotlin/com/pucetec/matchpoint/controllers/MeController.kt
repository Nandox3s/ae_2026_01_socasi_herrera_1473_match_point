package com.pucetec.matchpoint.controllers

import com.pucetec.matchpoint.clients.UsersClient
import com.pucetec.matchpoint.dto.MeResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MeController(private val usersClient: UsersClient) {
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): MeResponse =
        MeResponse(
            username = jwt.getClaimAsString("username") ?: jwt.subject,
            sub = jwt.subject,
            email = jwt.getClaimAsString("email"),
            groups = jwt.getClaimAsStringList("cognito:groups") ?: emptyList(),
            profile = usersClient.fetchCurrentProfileOrNull()
        )
}
