package com.pucetec.users

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class UsersApplicationTests {

    // Reemplazamos el JwtDecoder real por un mock. Asi el contexto levanta
    // sin salir a la red a descargar las llaves publicas de Cognito.
    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `application context loads`() {
    }
}
