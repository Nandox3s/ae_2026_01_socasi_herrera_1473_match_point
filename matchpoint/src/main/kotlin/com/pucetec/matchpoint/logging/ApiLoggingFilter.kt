package com.pucetec.matchpoint.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.filter.OncePerRequestFilter

/**
 * "Toda peticion deja rastro" (Criterio 2, punto d).
 *
 * Se instala UNA sola vez dentro de la cadena de Spring Security, justo despues del
 * filtro que valida el Bearer token: asi el `sub` de Cognito ya esta disponible para la
 * linea de entrada, y las peticiones que terminan en 401 (sin token) o 403 (rol
 * equivocado) tambien pasan por aqui y quedan registradas.
 */
class ApiLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(ApiLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        if (principal is Jwt) {
            MDC.put(MDC_SUB, principal.subject)
        }
        request.setAttribute(TRACED_ATTRIBUTE, true)

        val method = request.method
        val path = request.requestURI

        try {
            log.info(logLine("http.request", "$method $path"))
            filterChain.doFilter(request, response)
        } finally {
            val status = response.status
            val line = logLine("http.response", "$status $method $path")
            when {
                status >= 500 -> log.error(line)
                status >= 400 -> log.warn(line)
                else -> log.info(line)
            }
            MDC.remove(MDC_SUB)
        }
    }

    companion object {
        /** Marca que la peticion ya dejo su par de lineas, para no duplicarlas. */
        const val TRACED_ATTRIBUTE = "com.pucetec.matchpoint.logging.traced"
    }
}
