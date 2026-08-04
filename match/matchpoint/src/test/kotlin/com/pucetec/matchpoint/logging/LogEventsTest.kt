package com.pucetec.matchpoint.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LogEventsTest {

    @Test
    fun `a line without extra fields only carries event and msg`() {
        assertEquals(
            "event=user.created | msg=Profile created",
            logLine("user.created", "Profile created")
        )
    }

    @Test
    fun `extra fields are appended as key=value separated by spaces`() {
        assertEquals(
            "event=user.created | msg=Profile created | userId=17 username=player_fernando",
            logLine("user.created", "Profile created", "userId" to 17, "username" to "player_fernando")
        )
    }

    @Test
    fun `null fields are dropped`() {
        assertEquals(
            "event=user.updated | msg=Profile updated | userId=3",
            logLine("user.updated", "Profile updated", "userId" to 3, "email" to null)
        )
    }

    @Test
    fun `a value with spaces is quoted`() {
        assertEquals(
            """event=team.registered | msg=Team registered | title="my first entry"""",
            logLine("team.registered", "Team registered", "title" to "my first entry")
        )
    }

    @Test
    fun `the pipe character is forbidden inside a value and is replaced by a slash`() {
        assertEquals(
            "event=user.created | msg=Profile created | name=a/b",
            logLine("user.created", "Profile created", "name" to "a|b")
        )
    }

    @Test
    fun `line breaks never leak into the message`() {
        assertEquals(
            "event=user.create.failed | msg=first second",
            logLine("user.create.failed", "first\nsecond")
        )
        assertEquals("carriage return", sanitize("carriage\rreturn"))
    }

    @Test
    fun `emails are masked`() {
        assertEquals("j***@puce.edu.ec", maskEmail("juan.perez@puce.edu.ec"))
        assertEquals("***", maskEmail("sin-arroba"))
        assertNull(maskEmail(null))
        assertEquals("", maskEmail(""))
    }

    @Test
    fun `phones are masked`() {
        assertEquals("****5666", maskPhone("0999555666"))
        assertEquals("****", maskPhone("099"))
        assertNull(maskPhone(null))
        assertEquals("", maskPhone(""))
    }

    @Test
    fun `renderValue turns nulls into text without quotes`() {
        assertEquals("null", renderValue(null))
        assertEquals("42", renderValue(42))
    }
}
