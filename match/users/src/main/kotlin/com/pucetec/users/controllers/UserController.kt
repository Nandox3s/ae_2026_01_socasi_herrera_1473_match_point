package com.pucetec.users.controllers

import com.pucetec.users.dto.UserRequest
import com.pucetec.users.dto.UserResponse
import com.pucetec.users.services.UserService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * El controlador no tiene logica: solo saca la identidad del token ya validado y delega
 * en el servicio. Las rutas ya nacen bajo `/users`, que es lo que enruta nginx.
 */
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {

    // ============================================================
    // Endpoints "del propio usuario": el cognitoId sale del token,
    // NO del cliente. Esto demuestra la razon de ser del micro.
    // @AuthenticationPrincipal Jwt jwt -> Spring ya valido el token
    // y nos entrega sus claims. jwt.subject es el "sub" (el cognitoId).
    // ============================================================

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMyProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: UserRequest
    ): UserResponse = userService.createUser(jwt.subject, jwt.username(), request)

    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal jwt: Jwt
    ): UserResponse = userService.getUserByCognitoId(jwt.subject)

    @PutMapping("/me")
    fun updateMyProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: UserRequest
    ): UserResponse = userService.updateUser(jwt.subject, jwt.username(), request)

    // ============================================================
    // Endpoints administrativos: solo para el rol MANAGER.
    // ============================================================

    @GetMapping
    fun getAllUsers(): List<UserResponse> = userService.getAllUsers()

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): UserResponse = userService.getUserById(id)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable id: Long) = userService.deleteUser(id)

    // Consulta directa por cognitoId: la usa matchpoint cuando necesita el perfil de
    // alguien distinto al portador del token.
    @GetMapping("/cognito/{cognitoId}")
    fun getUserByCognitoId(@PathVariable cognitoId: String): UserResponse =
        userService.getUserByCognitoId(cognitoId)

    private fun Jwt.username(): String = getClaimAsString("username") ?: subject
}
