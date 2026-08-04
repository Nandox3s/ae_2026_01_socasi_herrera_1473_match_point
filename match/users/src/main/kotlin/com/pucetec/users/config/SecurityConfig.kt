package com.pucetec.users.config

import com.pucetec.users.logging.ApiLoggingFilter
import com.pucetec.users.logging.LoggingAccessDeniedHandler
import com.pucetec.users.logging.LoggingAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF no aplica a una API stateless que se autentica con Bearer token.
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                // Sonda de salud para el healthcheck de docker compose.
                auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                // Endpoints administrativos: solo MANAGER.
                auth.requestMatchers(HttpMethod.GET, "/users").hasRole(MANAGER)
                auth.requestMatchers(HttpMethod.GET, "/users/{id:[0-9]+}").hasRole(MANAGER)
                auth.requestMatchers(HttpMethod.DELETE, "/users/{id:[0-9]+}").hasRole(MANAGER)

                // El perfil propio y la consulta por cognitoId: cualquier usuario autenticado.
                auth.anyRequest().authenticated()
            }
            // Convierte la app en un Resource Server: valida el JWT usando el issuer-uri
            // configurado (descarga el JWKS de Cognito y verifica firma, expiracion e issuer).
            // El converter traduce cognito:groups -> ROLE_MANAGER / ROLE_PLAYER.
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { it.jwtAuthenticationConverter(jwtAuthenticationConverter()) }
                oauth2.authenticationEntryPoint(LoggingAuthenticationEntryPoint())
                oauth2.accessDeniedHandler(LoggingAccessDeniedHandler())
            }
            .exceptionHandling {
                it.authenticationEntryPoint(LoggingAuthenticationEntryPoint())
                it.accessDeniedHandler(LoggingAccessDeniedHandler())
            }
            // Traza de entrada/salida de cada peticion, ya con el sub del token disponible.
            .addFilterAfter(ApiLoggingFilter(), BearerTokenAuthenticationFilter::class.java)

        return http.build()
    }

    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter(CognitoGroupsConverter())
        return converter
    }

    private companion object {
        const val MANAGER = "MANAGER"
    }
}
