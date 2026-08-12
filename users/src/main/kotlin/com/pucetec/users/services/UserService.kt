package com.pucetec.users.services

import com.pucetec.users.audit.AuditAction
import com.pucetec.users.audit.AuditService
import com.pucetec.users.dto.UserRequest
import com.pucetec.users.dto.UserResponse
import com.pucetec.users.entities.User
import com.pucetec.users.exceptions.BlankNameException
import com.pucetec.users.exceptions.DuplicateCognitoIdException
import com.pucetec.users.exceptions.UserNotFoundException
import com.pucetec.users.logging.logLine
import com.pucetec.users.logging.maskEmail
import com.pucetec.users.logging.maskPhone
import com.pucetec.users.mappers.UserMapper
import com.pucetec.users.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun createUser(cognitoId: String, username: String, request: UserRequest): UserResponse {
        if (request.name.isBlank()) {
            throw BlankNameException("Name cannot be blank")
        }

        if (userRepository.existsByCognitoId(cognitoId)) {
            throw DuplicateCognitoIdException("Profile already exists for this Cognito user")
        }

        val saved = userRepository.save(userMapper.toEntity(request, cognitoId, username))
        auditService.record(ENTITY_NAME, saved.id, AuditAction.INSERT, newValues = describe(saved))
        logger.info(
            logLine(
                "user.created",
                "Profile created",
                "userId" to saved.id,
                "username" to saved.username,
                "email" to maskEmail(saved.email)
            )
        )
        return userMapper.toResponse(saved)
    }

    fun getAllUsers(): List<UserResponse> {
        val users = userRepository.findAll()
        logger.debug(logLine("user.listed", "Profiles listed", "total" to users.size))
        return userMapper.toResponseList(users)
    }

    fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id).orElseThrow {
            UserNotFoundException("User $id was not found")
        }
        return userMapper.toResponse(user)
    }

    fun getUserByCognitoId(cognitoId: String): UserResponse {
        val user = findByCognitoIdOrThrow(cognitoId)
        return userMapper.toResponse(user)
    }

    fun updateUser(cognitoId: String, username: String, request: UserRequest): UserResponse {
        val user = findByCognitoIdOrThrow(cognitoId)
        if (request.name.isBlank()) {
            throw BlankNameException("Name cannot be blank")
        }

        val previous = describe(user)
        user.username = username
        user.name = request.name
        user.email = request.email
        user.phone = request.phone

        val saved = userRepository.save(user)
        auditService.record(
            ENTITY_NAME,
            saved.id,
            AuditAction.UPDATE,
            oldValues = previous,
            newValues = describe(saved)
        )
        logger.info(
            logLine(
                "user.updated",
                "Profile updated",
                "userId" to saved.id,
                "username" to saved.username
            )
        )
        return userMapper.toResponse(saved)
    }

    fun deleteUser(id: Long) {
        val user = userRepository.findById(id).orElseThrow {
            UserNotFoundException("User $id was not found")
        }
        userRepository.delete(user)
        auditService.record(ENTITY_NAME, id, AuditAction.DELETE, oldValues = describe(user))
        logger.info(logLine("user.deleted", "Profile deleted", "userId" to id))
    }

    private fun findByCognitoIdOrThrow(cognitoId: String): User =
        userRepository.findByCognitoId(cognitoId).orElseThrow {
            UserNotFoundException("There is no profile for this Cognito user")
        }

    private fun describe(user: User): String =
        "username=${user.username} name=${user.name} " +
            "email=${maskEmail(user.email)} phone=${maskPhone(user.phone)}"

    private companion object {
        const val ENTITY_NAME = "users"
    }
}
