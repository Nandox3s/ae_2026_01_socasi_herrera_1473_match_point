package com.pucetec.users.audit

import com.pucetec.users.logging.MDC_SUB
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.MDC
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class AuditServiceTest {

    private lateinit var repository: AuditLogRepository
    private lateinit var service: AuditService

    @BeforeEach
    fun setUp() {
        repository = mock()
        whenever(repository.save(org.mockito.kotlin.any<AuditLog>())).thenAnswer { it.arguments[0] as AuditLog }
        service = AuditService(repository)
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
        SecurityContextHolder.clearContext()
    }

    private fun authenticateWith(subject: String, username: String?) {
        val builder = Jwt.withTokenValue("fake-token")
            .header("alg", "none")
            .subject(subject)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
        if (username != null) builder.claim("username", username)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(builder.build(), null, emptyList())
    }

    @Test
    fun `the sub is taken from the MDC when the trace filter already set it`() {
        MDC.put(MDC_SUB, "sub-from-mdc")
        authenticateWith("sub-from-token", "manager_josue")

        service.record("users", 1L, AuditAction.INSERT, newValues = "name=Ana")

        val captor = argumentCaptor<AuditLog>()
        verify(repository).save(captor.capture())
        assertEquals("users", captor.firstValue.entityName)
        assertEquals(1L, captor.firstValue.entityId)
        assertEquals(0L, captor.firstValue.id)
        assertNotNull(captor.firstValue.createdAt)
        assertEquals("sub-from-mdc", captor.firstValue.userSub)
        assertEquals("manager_josue", captor.firstValue.userName)
        assertEquals(AuditAction.INSERT, captor.firstValue.action)
        assertEquals("name=Ana", captor.firstValue.newValues)
    }

    @Test
    fun `without MDC the sub falls back to the token`() {
        authenticateWith("sub-from-token", "player_fernando")

        service.record("users", 2L, AuditAction.UPDATE, oldValues = "name=Ana", newValues = "name=Ana Lopez")

        val captor = argumentCaptor<AuditLog>()
        verify(repository).save(captor.capture())
        assertEquals("sub-from-token", captor.firstValue.userSub)
        assertEquals("player_fernando", captor.firstValue.userName)
        assertEquals("name=Ana", captor.firstValue.oldValues)
    }

    @Test
    fun `a token without username claim is audited as anonymous`() {
        authenticateWith("sub-from-token", null)

        service.record("users", 3L, AuditAction.DELETE, oldValues = "name=Ana")

        val captor = argumentCaptor<AuditLog>()
        verify(repository).save(captor.capture())
        assertEquals("anonimo", captor.firstValue.userName)
    }

    @Test
    fun `without authentication everything is audited as anonymous`() {
        service.record("users", 4L, AuditAction.DELETE)

        val captor = argumentCaptor<AuditLog>()
        verify(repository).save(captor.capture())
        assertEquals("anonimo", captor.firstValue.userSub)
        assertEquals("anonimo", captor.firstValue.userName)
    }

    @Test
    fun `long values are truncated so the audit table never explodes`() {
        val huge = "x".repeat(2500)

        service.record("users", 5L, AuditAction.UPDATE, oldValues = huge, newValues = huge)

        val captor = argumentCaptor<AuditLog>()
        verify(repository).save(captor.capture())
        assertEquals(2000, captor.firstValue.oldValues?.length)
        assertEquals(2000, captor.firstValue.newValues?.length)
    }
}
