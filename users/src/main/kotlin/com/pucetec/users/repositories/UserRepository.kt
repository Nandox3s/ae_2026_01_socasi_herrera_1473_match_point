package com.pucetec.users.repositories

import com.pucetec.users.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByCognitoId(cognitoId: String): Optional<User>

    fun existsByCognitoId(cognitoId: String): Boolean
}
