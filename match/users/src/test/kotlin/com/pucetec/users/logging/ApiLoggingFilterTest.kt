package com.pucetec.users.logging

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class ApiLoggingFilterTest {

    private val filter = ApiLoggingFilter()

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        MDC.clear()
    }

    private fun jwt(subject: String) = Jwt.withTokenValue("fake-token")
        .header("alg", "none")
        .subject(subject)
        .claim("username", "player_fernando")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(600))
        .build()

    private fun chainThatReturns(status: Int, capturedSub: MutableList<String?>) = FilterChain { _, response ->
        capturedSub.add(MDC.get(MDC_SUB))
        (response as MockHttpServletResponse).status = status
    }

    @Test
    fun `an authenticated request carries the cognito sub in the MDC`() {
        val principal = jwt("sub-123")
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, emptyList())

        val request = MockHttpServletRequest("POST", "/users/me")
        val response = MockHttpServletResponse()
        val seen = mutableListOf<String?>()

        filter.doFilter(request, response, chainThatReturns(201, seen))

        assertEquals(listOf("sub-123"), seen)
        assertTrue(request.getAttribute(ApiLoggingFilter.TRACED_ATTRIBUTE) as Boolean)
        // El MDC se limpia al salir para no contaminar el siguiente request del hilo.
        assertNull(MDC.get(MDC_SUB))
    }

    @Test
    fun `a request without a token is traced as anonymous`() {
        val request = MockHttpServletRequest("GET", "/users")
        val response = MockHttpServletResponse()
        val seen = mutableListOf<String?>()

        filter.doFilter(request, response, chainThatReturns(401, seen))

        assertEquals(listOf<String?>(null), seen)
        assertNotNull(request.getAttribute(ApiLoggingFilter.TRACED_ATTRIBUTE))
    }

    @Test
    fun `a 403 is also traced`() {
        val request = MockHttpServletRequest("DELETE", "/users/1")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chainThatReturns(403, mutableListOf()))

        assertEquals(403, response.status)
    }

    @Test
    fun `a 500 is traced at ERROR level`() {
        val request = MockHttpServletRequest("GET", "/users/1")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chainThatReturns(500, mutableListOf()))

        assertEquals(500, response.status)
    }
}
