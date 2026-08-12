package com.pucetec.matchpoint.logging

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler

class LoggingSecurityHandlersTest {
    private class RecordingEntryPoint : AuthenticationEntryPoint {
        var called = false
        override fun commence(
            request: jakarta.servlet.http.HttpServletRequest,
            response: jakarta.servlet.http.HttpServletResponse,
            authException: AuthenticationException
        ) {
            called = true
            response.status = 401
        }
    }

    private class RecordingAccessDeniedHandler : AccessDeniedHandler {
        var called = false
        override fun handle(
            request: jakarta.servlet.http.HttpServletRequest,
            response: jakarta.servlet.http.HttpServletResponse,
            accessDeniedException: AccessDeniedException
        ) {
            called = true
            response.status = 403
        }
    }

    @Test
    fun `an expired token that never reached the trace filter still leaves entry and exit lines`() {
        val delegate = RecordingEntryPoint()
        val request = MockHttpServletRequest("GET", "/matchpoint/me")
        val response = MockHttpServletResponse()

        LoggingAuthenticationEntryPoint(delegate)
            .commence(request, response, BadCredentialsException("expired token"))

        assertTrue(delegate.called)
        assertTrue(response.status == 401)
    }

    @Test
    fun `a request already traced by the filter is not logged twice`() {
        val delegate = RecordingEntryPoint()
        val request = MockHttpServletRequest("GET", "/matchpoint/me")
        request.setAttribute(ApiLoggingFilter.TRACED_ATTRIBUTE, true)
        val response = MockHttpServletResponse()

        LoggingAuthenticationEntryPoint(delegate)
            .commence(request, response, BadCredentialsException("no token"))

        assertTrue(delegate.called)
    }

    @Test
    fun `the default entry point answers 401 with the Bearer challenge`() {
        val request = MockHttpServletRequest("GET", "/matchpoint/courts")
        val response = MockHttpServletResponse()

        LoggingAuthenticationEntryPoint().commence(request, response, BadCredentialsException("no token"))

        assertTrue(response.status == 401)
    }

    @Test
    fun `a role rejection logs the authz denied event`() {
        val delegate = RecordingAccessDeniedHandler()
        val request = MockHttpServletRequest("DELETE", "/matchpoint/courts/1")
        val response = MockHttpServletResponse()

        LoggingAccessDeniedHandler(delegate).handle(request, response, AccessDeniedException("denied"))

        assertTrue(delegate.called)
        assertTrue(response.status == 403)
    }

    @Test
    fun `the default access denied handler answers 403`() {
        val request = MockHttpServletRequest("DELETE", "/matchpoint/courts/1")
        val response = MockHttpServletResponse()

        LoggingAccessDeniedHandler().handle(request, response, AccessDeniedException("denied"))

        assertTrue(response.status == 403)
    }
}
