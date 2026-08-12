package com.pucetec.users.services

import com.pucetec.users.audit.AuditAction
import com.pucetec.users.audit.AuditService
import com.pucetec.users.dto.UserRequest
import com.pucetec.users.entities.User
import com.pucetec.users.exceptions.BlankNameException
import com.pucetec.users.exceptions.DuplicateCognitoIdException
import com.pucetec.users.exceptions.UserNotFoundException
import com.pucetec.users.mappers.UserMapper
import com.pucetec.users.repositories.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class UserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var auditService: AuditService
    private lateinit var service: UserService

    private val cognitoId = "a1b2c3d4-sub-de-cognito"
    private val username = "player_fernando"

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        auditService = mock()
        service = UserService(userRepository, UserMapper(), auditService)
    }

    private fun user(
        id: Long = 1L,
        name: String = "Fernando Socasi",
        email: String? = "fernando.socasi@puce.edu.ec",
        phone: String? = "0999555666"
    ) = User(cognitoId = cognitoId, username = username, name = name, email = email, phone = phone, id = id)

    @Test
    fun `createUser links the cognitoId and returns the created profile`() {
        whenever(userRepository.existsByCognitoId(cognitoId)).thenReturn(false)
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] as User }

        val response = service.createUser(
            cognitoId,
            username,
            UserRequest(name = "Fernando Socasi", email = "fernando.socasi@puce.edu.ec", phone = "0999555666")
        )

        assertEquals(cognitoId, response.cognitoId)
        assertEquals(username, response.username)
        assertEquals("Fernando Socasi", response.name)
        assertEquals("fernando.socasi@puce.edu.ec", response.email)
        verify(auditService).record(eq("users"), any(), eq(AuditAction.INSERT), anyOrNull(), anyOrNull())
    }

    @Test
    fun `createUser rejects a blank name`() {
        assertThrows<BlankNameException> {
            service.createUser(cognitoId, username, UserRequest(name = "   ", email = null, phone = null))
        }
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `createUser rejects a cognitoId that already has a profile`() {
        whenever(userRepository.existsByCognitoId(cognitoId)).thenReturn(true)

        assertThrows<DuplicateCognitoIdException> {
            service.createUser(cognitoId, username, UserRequest(name = "Fernando", email = null, phone = null))
        }
    }

    @Test
    fun `getAllUsers returns every profile`() {
        whenever(userRepository.findAll()).thenReturn(
            listOf(user(id = 1L), user(id = 2L, name = "Luis Cabrera", email = null, phone = null))
        )

        val responses = service.getAllUsers()

        assertEquals(2, responses.size)
        assertEquals("Fernando Socasi", responses[0].name)
        assertNull(responses[1].email)
    }

    @Test
    fun `getUserById returns the profile when it exists`() {
        whenever(userRepository.findById(5L)).thenReturn(Optional.of(user(id = 5L)))

        assertEquals(5L, service.getUserById(5L).id)
    }

    @Test
    fun `getUserById fails when the id does not exist`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<UserNotFoundException> { service.getUserById(99L) }
    }

    @Test
    fun `getUserByCognitoId returns the linked profile`() {
        whenever(userRepository.findByCognitoId(cognitoId)).thenReturn(Optional.of(user(id = 7L)))

        assertEquals(7L, service.getUserByCognitoId(cognitoId).id)
    }

    @Test
    fun `getUserByCognitoId fails when there is no linked profile`() {
        whenever(userRepository.findByCognitoId(cognitoId)).thenReturn(Optional.empty())

        assertThrows<UserNotFoundException> { service.getUserByCognitoId(cognitoId) }
    }

    @Test
    fun `updateUser overwrites the profile data`() {
        whenever(userRepository.findByCognitoId(cognitoId)).thenReturn(Optional.of(user(id = 7L)))
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] as User }

        val response = service.updateUser(
            cognitoId,
            "player_fernando_v2",
            UserRequest(name = "Fernando S.", email = "nueva@puce.edu.ec", phone = "0988888888")
        )

        assertEquals("Fernando S.", response.name)
        assertEquals("nueva@puce.edu.ec", response.email)
        assertEquals("player_fernando_v2", response.username)
        verify(auditService).record(eq("users"), eq(7L), eq(AuditAction.UPDATE), anyOrNull(), anyOrNull())
    }

    @Test
    fun `updateUser rejects a blank name`() {
        whenever(userRepository.findByCognitoId(cognitoId)).thenReturn(Optional.of(user()))

        assertThrows<BlankNameException> {
            service.updateUser(cognitoId, username, UserRequest(name = " ", email = null, phone = null))
        }
    }

    @Test
    fun `updateUser fails when there is no linked profile`() {
        whenever(userRepository.findByCognitoId(cognitoId)).thenReturn(Optional.empty())

        assertThrows<UserNotFoundException> {
            service.updateUser(cognitoId, username, UserRequest(name = "Fernando", email = null, phone = null))
        }
    }

    @Test
    fun `deleteUser removes the profile and records the audit entry`() {
        val existing = user(id = 4L)
        whenever(userRepository.findById(4L)).thenReturn(Optional.of(existing))

        service.deleteUser(4L)

        verify(userRepository).delete(existing)
        verify(auditService).record(eq("users"), eq(4L), eq(AuditAction.DELETE), anyOrNull(), anyOrNull())
    }

    @Test
    fun `deleteUser fails when the id does not exist`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<UserNotFoundException> { service.deleteUser(99L) }
        verify(userRepository, never()).delete(any<User>())
    }
}
