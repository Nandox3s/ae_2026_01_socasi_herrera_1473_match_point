package com.pucetec.matchpoint.logging

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler
import org.springframework.security.web.access.AccessDeniedHandler

/**
 * El 403 por rol lo decide Spring Security. La linea `http.response` la escribe igual el
 * [ApiLoggingFilter]; aqui se agrega el evento de negocio que explica POR QUE se nego.
 */
class LoggingAccessDeniedHandler(
    private val delegate: AccessDeniedHandler = BearerTokenAccessDeniedHandler()
) : AccessDeniedHandler {

    private val log = LoggerFactory.getLogger(LoggingAccessDeniedHandler::class.java)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        log.warn(
            logLine(
                "authz.denied",
                "Role is not allowed on this endpoint",
                "method" to request.method,
                "path" to request.requestURI
            )
        )
        delegate.handle(request, response, accessDeniedException)
    }
}
