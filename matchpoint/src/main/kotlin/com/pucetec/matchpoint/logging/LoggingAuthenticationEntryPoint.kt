package com.pucetec.matchpoint.logging

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.AuthenticationEntryPoint

class LoggingAuthenticationEntryPoint(
    private val delegate: AuthenticationEntryPoint = BearerTokenAuthenticationEntryPoint()
) : AuthenticationEntryPoint {
    private val log = LoggerFactory.getLogger(LoggingAuthenticationEntryPoint::class.java)

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val alreadyTraced = request.getAttribute(ApiLoggingFilter.TRACED_ATTRIBUTE) != null
        val method = request.method
        val path = request.requestURI

        if (!alreadyTraced) {
            log.info(logLine("http.request", "$method $path"))
        }

        log.warn(logLine("auth.rejected", "Token missing, invalid or expired", "reason" to authException.message))

        delegate.commence(request, response, authException)

        if (!alreadyTraced) {
            log.warn(logLine("http.response", "${response.status} $method $path"))
        }
    }
}
