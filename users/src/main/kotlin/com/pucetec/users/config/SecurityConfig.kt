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

            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->

                auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                auth.requestMatchers(HttpMethod.GET, "/users").hasRole(MANAGER)
                auth.requestMatchers(HttpMethod.GET, "/users/{id:[0-9]+}").hasRole(MANAGER)
                auth.requestMatchers(HttpMethod.DELETE, "/users/{id:[0-9]+}").hasRole(MANAGER)

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
