package com.pucetec.matchpoint.config

import com.pucetec.matchpoint.logging.ApiLoggingFilter
import com.pucetec.matchpoint.logging.LoggingAccessDeniedHandler
import com.pucetec.matchpoint.logging.LoggingAuthenticationEntryPoint
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
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->

                // Sonda de salud para el healthcheck de docker compose.
                auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                auth.requestMatchers(HttpMethod.GET, "/courts", "/courts/*").permitAll()

                auth.requestMatchers(
                    HttpMethod.GET,
                    "/tournaments",
                    "/tournaments/*",
                    "/tournaments/*/teams",
                    "/tournaments/*/teams/*",
                    "/tournaments/*/matches"
                ).permitAll()

                auth.requestMatchers(HttpMethod.GET, "/matches/*").permitAll()

                auth.requestMatchers(HttpMethod.POST, "/courts").hasRole(MANAGER)
                auth.requestMatchers(HttpMethod.PATCH, "/courts/*").hasRole(MANAGER)

                auth.requestMatchers(HttpMethod.POST, "/tournaments").hasRole(MANAGER)
                auth.requestMatchers(HttpMethod.POST, "/tournaments/*/start").hasRole(MANAGER)
                auth.requestMatchers(HttpMethod.PATCH, "/matches/*/schedule").hasRole(MANAGER)
                auth.requestMatchers(HttpMethod.PATCH, "/matches/*/score").hasRole(MANAGER)

                auth.requestMatchers(HttpMethod.POST, "/reservations").hasRole(PLAYER)
                auth.requestMatchers(HttpMethod.GET, "/reservations/me", "/reservations/*").hasRole(PLAYER)
                auth.requestMatchers(HttpMethod.DELETE, "/reservations/*").hasRole(PLAYER)

                auth.requestMatchers(HttpMethod.POST, "/tournaments/*/teams").hasRole(PLAYER)
                auth.requestMatchers(HttpMethod.DELETE, "/tournaments/*/teams/*").hasRole(PLAYER)

                auth.anyRequest().authenticated()
            }
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
        const val PLAYER = "PLAYER"
    }
}
