package com.pucetec.users.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

/**
 * La regla que decide el rol de cada usuario a partir del token de Cognito.
 */
class CognitoGroupsConverterTest {

    private val converter = CognitoGroupsConverter()

    private fun tokenWithGroups(groups: List<String>?): Jwt {
        val builder = Jwt.withTokenValue("fake-token")
            .header("alg", "none")
            .subject("a1b2c3d4-5e6f-7890-abcd-ef1234567890")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
        if (groups != null) builder.claim("cognito:groups", groups)
        return builder.build()
    }

    @Test
    fun `a cognito group becomes a spring security role`() {
        val authorities = converter.convert(tokenWithGroups(listOf("MANAGER")))

        assertEquals(listOf("ROLE_MANAGER"), authorities.map { it.authority })
    }

    @Test
    fun `the group name is uppercased before building the role`() {
        val authorities = converter.convert(tokenWithGroups(listOf("player")))

        assertEquals(listOf("ROLE_PLAYER"), authorities.map { it.authority })
    }

    @Test
    fun `a user in several groups gets one role per group`() {
        val authorities = converter.convert(tokenWithGroups(listOf("MANAGER", "PLAYER")))

        assertEquals(listOf("ROLE_MANAGER", "ROLE_PLAYER"), authorities.map { it.authority })
    }

    @Test
    fun `a token without the groups claim gets no roles at all`() {
        assertTrue(converter.convert(tokenWithGroups(null)).isEmpty())
    }
}
