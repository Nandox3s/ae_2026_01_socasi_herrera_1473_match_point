package com.pucetec.matchpoint.exceptions

import com.pucetec.matchpoint.logging.logLine
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Un solo lugar traduce excepcion -> codigo HTTP y deja el evento de negocio en el log.
 * Los mensajes de la API van en ingles, igual que los del log.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(
        TournamentNotFoundException::class,
        TeamNotFoundException::class,
        MatchNotFoundException::class,
        CourtNotFoundException::class,
        ReservationNotFoundException::class
    )
    fun handleNotFound(exception: RuntimeException): ResponseEntity<Map<String, String>> =
        respond(HttpStatus.NOT_FOUND, "resource.not_found", exception)

    @ExceptionHandler(
        NotYourTournamentException::class,
        NotYourTeamException::class,
        NotYourCourtException::class,
        NotYourReservationException::class
    )
    fun handleForbidden(exception: RuntimeException): ResponseEntity<Map<String, String>> =
        respond(HttpStatus.FORBIDDEN, "ownership.denied", exception)

    @ExceptionHandler(
        InvalidTournamentException::class,
        InvalidTeamException::class,
        TieNotAllowedException::class,
        InvalidCourtException::class,
        InvalidReservationException::class
    )
    fun handleBadRequest(exception: RuntimeException): ResponseEntity<Map<String, String>> =
        respond(HttpStatus.BAD_REQUEST, "request.rejected", exception)

    @ExceptionHandler(
        TournamentFullException::class,
        DuplicateTeamNameException::class,
        RegistrationClosedException::class,
        TournamentNotReadyException::class,
        MatchNotReadyException::class,
        MatchAlreadyPlayedException::class,
        CourtNotAvailableException::class,
        ProfileNotRegisteredException::class
    )
    fun handleConflict(exception: RuntimeException): ResponseEntity<Map<String, String>> =
        respond(HttpStatus.CONFLICT, "state.conflict", exception)

    @ExceptionHandler(UsersServiceUnavailableException::class)
    fun handleServiceUnavailable(exception: RuntimeException): ResponseEntity<Map<String, String>> =
        respond(HttpStatus.SERVICE_UNAVAILABLE, "users.unavailable", exception)

    private fun respond(
        status: HttpStatus,
        event: String,
        exception: RuntimeException
    ): ResponseEntity<Map<String, String>> {
        val message = exception.message ?: status.reasonPhrase
        logger.warn(logLine(event, message, "status" to status.value()))
        return ResponseEntity.status(status).body(mapOf("error" to message))
    }
}
