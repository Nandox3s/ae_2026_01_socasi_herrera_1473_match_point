package com.pucetec.users.mappers

import com.pucetec.users.dto.UserRequest
import com.pucetec.users.dto.UserResponse
import com.pucetec.users.entities.User
import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun toEntity(request: UserRequest, cognitoId: String, username: String): User =
        User(
            cognitoId = cognitoId,
            username = username,
            name = request.name,
            email = request.email,
            phone = request.phone
        )

    fun toResponse(user: User): UserResponse =
        UserResponse(
            id = user.id,
            cognitoId = user.cognitoId,
            username = user.username,
            name = user.name,
            email = user.email,
            phone = user.phone,
            createdAt = user.createdAt
        )

    fun toResponseList(users: List<User>): List<UserResponse> = users.map { toResponse(it) }
}
