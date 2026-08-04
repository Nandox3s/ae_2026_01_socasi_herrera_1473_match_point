package com.pucetec.matchpoint.clients

import com.pucetec.matchpoint.exceptions.ProfileNotRegisteredException
import com.pucetec.matchpoint.exceptions.UsersServiceUnavailableException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Instant

/**
 * Estos tests demuestran lo que exige el Criterio 1: matchpoint pide el dato ajeno por
 * HTTP al microservicio `users`, propagando el MISMO token del usuario.
 */
class UsersClientTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var client: UsersClient

    private val baseUrl = "http://users:8686"

    private val profileJson = """
        {"id":3,"cognitoId":"seed-sub-player-fernando","username":"player_fernando",
         "name":"Fernando Socasi","email":"fernando.socasi@puce.edu.ec","phone":"0999555666",
         "createdAt":"2026-07-02T10:00:00"}
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        client = UsersClient(builder, baseUrl)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun authenticate(tokenValue: String = "fake-access-token") {
        val jwt = Jwt.withTokenValue(tokenValue)
            .header("alg", "none")
            .subject("seed-sub-player-fernando")
            .claim("username", "player_fernando")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build()
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(jwt, null, emptyList())
    }

    @Test
    fun `the profile arrives over HTTP with the caller token propagated`() {
        authenticate()
        server.expect(requestTo("$baseUrl/users/me"))
            .andExpect(header("Authorization", "Bearer fake-access-token"))
            .andRespond(withSuccess(profileJson, MediaType.APPLICATION_JSON))

        val profile = client.fetchCurrentProfile()

        assertEquals("player_fernando", profile.username)
        assertEquals("Fernando Socasi", profile.name)
        server.verify()
    }

    @Test
    fun `without a token in the security context the call is not even attempted`() {
        assertThrows<UsersServiceUnavailableException> { client.fetchCurrentProfile() }
    }

    @Test
    fun `a 404 from users means the profile is not registered yet`() {
        authenticate()
        server.expect(requestTo("$baseUrl/users/me")).andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertThrows<ProfileNotRegisteredException> { client.fetchCurrentProfile() }
    }

    @Test
    fun `an empty body is treated as a missing profile`() {
        authenticate()
        server.expect(requestTo("$baseUrl/users/me")).andRespond(withStatus(HttpStatus.NO_CONTENT))

        assertThrows<ProfileNotRegisteredException> { client.fetchCurrentProfile() }
    }

    @Test
    fun `users being down is a recoverable failure`() {
        authenticate()
        server.expect(requestTo("$baseUrl/users/me")).andRespond(withStatus(HttpStatus.BAD_GATEWAY))

        assertThrows<UsersServiceUnavailableException> { client.fetchCurrentProfile() }
    }

    @Test
    fun `fetchCurrentProfileOrNull swallows the failure`() {
        authenticate()
        server.expect(requestTo("$baseUrl/users/me")).andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertNull(client.fetchCurrentProfileOrNull())
    }

    @Test
    fun `fetchCurrentProfileOrNull returns the profile on the happy path`() {
        authenticate()
        server.expect(requestTo("$baseUrl/users/me"))
            .andRespond(withSuccess(profileJson, MediaType.APPLICATION_JSON))

        assertEquals("player_fernando", client.fetchCurrentProfileOrNull()?.username)
    }
}
