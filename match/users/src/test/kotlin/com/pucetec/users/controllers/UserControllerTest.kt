package com.pucetec.users.controllers

import com.pucetec.users.config.SecurityConfig
import com.pucetec.users.dto.UserResponse
import com.pucetec.users.exceptions.BlankNameException
import com.pucetec.users.exceptions.DuplicateCognitoIdException
import com.pucetec.users.exceptions.GlobalExceptionHandler
import com.pucetec.users.exceptions.UserNotFoundException
import com.pucetec.users.services.UserService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class UserControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var userService: UserService

    private val sub = "a1b2c3d4-5e6f-7890-abcd-ef1234567890"

    private fun manager() = jwt()
        .authorities(SimpleGrantedAuthority("ROLE_MANAGER"))
        .jwt { it.subject(sub).claim("username", "manager_josue") }

    private fun player() = jwt()
        .authorities(SimpleGrantedAuthority("ROLE_PLAYER"))
        .jwt { it.subject(sub).claim("username", "player_fernando") }

    /** Token sin el claim `username`: el controlador cae al `sub`. */
    private fun playerWithoutUsername() = jwt()
        .authorities(SimpleGrantedAuthority("ROLE_PLAYER"))
        .jwt { it.subject(sub) }

    private fun profile(id: Long = 1L, username: String = "player_fernando") = UserResponse(
        id = id,
        cognitoId = sub,
        username = username,
        name = "Fernando Socasi",
        email = "fernando.socasi@puce.edu.ec",
        phone = "0999555666",
        createdAt = LocalDateTime.of(2026, 7, 2, 10, 0)
    )

    private val body = """{"name":"Fernando Socasi","email":"fernando.socasi@puce.edu.ec","phone":"0999555666"}"""

    // ------------------------------------------------------------------ perfil propio

    @Test
    fun `POST users me without a token returns 401`() {
        mockMvc.perform(post("/users/me").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST users me returns 201 and takes the identity from the token`() {
        whenever(userService.createUser(any(), any(), any())).thenReturn(profile())

        mockMvc.perform(post("/users/me").with(player()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.cognitoId").value(sub))

        verify(userService).createUser(eq(sub), eq("player_fernando"), any())
    }

    @Test
    fun `POST users me falls back to the sub when the token has no username claim`() {
        whenever(userService.createUser(any(), any(), any())).thenReturn(profile(username = sub))

        mockMvc.perform(
            post("/users/me").with(playerWithoutUsername())
                .contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isCreated)

        verify(userService).createUser(eq(sub), eq(sub), any())
    }

    @Test
    fun `GET users me returns the own profile`() {
        whenever(userService.getUserByCognitoId(sub)).thenReturn(profile())

        mockMvc.perform(get("/users/me").with(player()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("player_fernando"))
    }

    @Test
    fun `PUT users me updates the own profile`() {
        whenever(userService.updateUser(any(), any(), any())).thenReturn(profile())

        mockMvc.perform(put("/users/me").with(player()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Fernando Socasi"))
    }

    // ---------------------------------------------------------- endpoints de MANAGER

    @Test
    fun `GET users with MANAGER returns the whole list`() {
        whenever(userService.getAllUsers()).thenReturn(listOf(profile(), profile(id = 2L)))

        mockMvc.perform(get("/users").with(manager()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `GET users with PLAYER returns 403`() {
        mockMvc.perform(get("/users").with(player())).andExpect(status().isForbidden)
    }

    @Test
    fun `GET users without a token returns 401`() {
        mockMvc.perform(get("/users")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET users by id with MANAGER returns 200`() {
        whenever(userService.getUserById(1L)).thenReturn(profile())

        mockMvc.perform(get("/users/1").with(manager()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `GET users by id with PLAYER returns 403`() {
        mockMvc.perform(get("/users/1").with(player())).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE users by id with MANAGER returns 204`() {
        mockMvc.perform(delete("/users/1").with(manager())).andExpect(status().isNoContent)

        verify(userService).deleteUser(1L)
    }

    @Test
    fun `DELETE users by id with PLAYER returns 403`() {
        mockMvc.perform(delete("/users/1").with(player())).andExpect(status().isForbidden)
    }

    @Test
    fun `GET users by cognitoId is open to any authenticated user`() {
        whenever(userService.getUserByCognitoId("otro-sub")).thenReturn(profile())

        mockMvc.perform(get("/users/cognito/otro-sub").with(player()))
            .andExpect(status().isOk)
    }

    // -------------------------------------------------- traduccion de errores a HTTP

    @Test
    fun `a missing profile is answered with 404`() {
        whenever(userService.getUserByCognitoId(sub)).thenThrow(UserNotFoundException("There is no profile for this Cognito user"))

        mockMvc.perform(get("/users/me").with(player()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("There is no profile for this Cognito user"))
            .andExpect(jsonPath("$.source").value("UserService"))
    }

    @Test
    fun `a duplicated profile is answered with 409`() {
        whenever(userService.createUser(any(), any(), any()))
            .thenThrow(DuplicateCognitoIdException("Profile already exists for this Cognito user"))

        mockMvc.perform(post("/users/me").with(player()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Profile already exists for this Cognito user"))
    }

    @Test
    fun `a blank name is answered with 400`() {
        whenever(userService.createUser(any(), any(), any())).thenThrow(BlankNameException("Name cannot be blank"))

        mockMvc.perform(post("/users/me").with(player()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Name cannot be blank"))
    }
}
