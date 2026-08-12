package com.pucetec.users.exceptions

import com.pucetec.users.logging.logLine
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BlankNameException::class)
    fun handleBlankName(exception: BlankNameException): ResponseEntity<ExceptionResponse> =
        respond(HttpStatus.BAD_REQUEST, "user.rejected", exception)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(exception: UserNotFoundException): ResponseEntity<ExceptionResponse> =
        respond(HttpStatus.NOT_FOUND, "user.not_found", exception)

    @ExceptionHandler(DuplicateCognitoIdException::class)
    fun handleDuplicateCognitoId(exception: DuplicateCognitoIdException): ResponseEntity<ExceptionResponse> =
        respond(HttpStatus.CONFLICT, "user.create.failed", exception)

    private fun respond(
        status: HttpStatus,
        event: String,
        exception: RuntimeException
    ): ResponseEntity<ExceptionResponse> {
        val message = exception.message ?: status.reasonPhrase
        logger.warn(logLine(event, message, "status" to status.value()))
        return ResponseEntity.status(status).body(ExceptionResponse(message, "UserService"))
    }
}

data class ExceptionResponse(
    val message: String,
    val source: String,
)
