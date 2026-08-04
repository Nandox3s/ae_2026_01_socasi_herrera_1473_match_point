package com.pucetec.matchpoint.clients

import com.pucetec.matchpoint.exceptions.ProfileNotRegisteredException
import com.pucetec.matchpoint.exceptions.UsersServiceUnavailableException
import com.pucetec.matchpoint.logging.logLine
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * Comunicacion SINCRONA con el otro microservicio (Criterio 1).
 *
 *  - La URL base sale de una variable de entorno y apunta al NOMBRE DE SERVICIO de
 *    Compose (`http://users:8686`), nunca a localhost ni a una IP.
 *  - La identidad viaja en el propio token: se reenvia la cabecera `Authorization` del
 *    usuario que hizo la peticion, asi que `users` aplica sus mismas reglas.
 *  - Si `users` esta caido devolvemos 503 y el sistema se recupera solo; por eso en el
 *    compose la dependencia entre microservicios es `service_started`, no `service_healthy`.
 */
@Component
class UsersClient(
    builder: RestClient.Builder,
    @Value("\${services.users.base-url}") baseUrl: String
) {

    private val logger = LoggerFactory.getLogger(UsersClient::class.java)

    private val restClient: RestClient = builder.baseUrl(baseUrl).build()

    /** Perfil del usuario que hizo la peticion. Lanza si no existe o si `users` no responde. */
    fun fetchCurrentProfile(): UserProfile {
        val token = currentTokenValue()
            ?: throw UsersServiceUnavailableException("No bearer token available to call the users microservice")

        logger.info(logLine("users.profile.requested", "Calling users microservice", "endpoint" to "GET /users/me"))

        val profile = try {
            restClient.get()
                .uri("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .body(UserProfile::class.java)
        } catch (exception: HttpClientErrorException.NotFound) {
            logger.warn(
                logLine(
                    "users.profile.missing",
                    "The users microservice has no profile for this account",
                    "status" to exception.statusCode.value()
                )
            )
            throw ProfileNotRegisteredException(
                "Register your profile in the users microservice before using this endpoint"
            )
        } catch (exception: RestClientException) {
            logger.error(
                logLine("users.profile.failed", "The users microservice is not reachable"),
                exception
            )
            throw UsersServiceUnavailableException("The users microservice is not reachable right now")
        }

        if (profile == null) {
            logger.warn(logLine("users.profile.missing", "The users microservice returned an empty body"))
            throw ProfileNotRegisteredException(
                "Register your profile in the users microservice before using this endpoint"
            )
        }

        logger.info(
            logLine(
                "users.profile.received",
                "Profile resolved from the users microservice",
                "username" to profile.username
            )
        )
        return profile
    }

    /** Igual que [fetchCurrentProfile] pero devuelve null en vez de fallar. Para `/me`. */
    fun fetchCurrentProfileOrNull(): UserProfile? = try {
        fetchCurrentProfile()
    } catch (exception: RuntimeException) {
        logger.warn(logLine("users.profile.skipped", "Continuing without profile", "reason" to exception.message))
        null
    }

    private fun currentTokenValue(): String? =
        (SecurityContextHolder.getContext().authentication?.principal as? Jwt)?.tokenValue
}
