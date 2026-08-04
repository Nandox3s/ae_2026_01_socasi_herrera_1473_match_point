package com.pucetec.matchpoint.controllers

import com.pucetec.matchpoint.clients.UserProfile
import com.pucetec.matchpoint.clients.UsersClient
import com.pucetec.matchpoint.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(MeController::class)
@Import(SecurityConfig::class)
class MeControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var usersClient: UsersClient

    @Test
    fun `without a token, me returns 401`() {
        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `me returns the role from cognito groups and the profile from the users microservice`() {
        whenever(usersClient.fetchCurrentProfileOrNull()).thenReturn(
            UserProfile(1L, "a1b2c3d4-5e6f-7890-abcd-ef1234567890", "manager_josue", "Josué Herrera", null, null)
        )

        val accessToken = jwt().jwt {
            it.subject("a1b2c3d4-5e6f-7890-abcd-ef1234567890")
                .claim("username", "manager_josue")
                .claim("cognito:groups", listOf("MANAGER"))
                .claim("token_use", "access")
        }

        mockMvc.perform(get("/me").with(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("manager_josue"))
            .andExpect(jsonPath("$.sub").value("a1b2c3d4-5e6f-7890-abcd-ef1234567890"))
            .andExpect(jsonPath("$.groups[0]").value("MANAGER"))
            .andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.profile.name").value("Josué Herrera"))
    }

    @Test
    fun `me survives a token without username claim and without a profile`() {
        whenever(usersClient.fetchCurrentProfileOrNull()).thenReturn(null)

        val accessToken = jwt().jwt { it.subject("sub-only") }

        mockMvc.perform(get("/me").with(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("sub-only"))
            .andExpect(jsonPath("$.groups.length()").value(0))
            .andExpect(jsonPath("$.profile").doesNotExist())
    }
}
