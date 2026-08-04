package com.pucetec.users.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Traduce el claim `cognito:groups` del token a autoridades de Spring Security:
 * `MANAGER` -> `ROLE_MANAGER`, `PLAYER` -> `ROLE_PLAYER`.
 *
 * Vive aparte de [SecurityConfig] a proposito: no es cableado, es la REGLA que decide
 * el rol de cada usuario, y como tal se prueba sola.
 */
class CognitoGroupsConverter : Converter<Jwt, Collection<GrantedAuthority>> {

    override fun convert(source: Jwt): Collection<GrantedAuthority> {
        val groups = source.getClaimAsStringList(GROUPS_CLAIM) ?: emptyList()
        return groups.map { SimpleGrantedAuthority("$ROLE_PREFIX${it.uppercase()}") }
    }

    private companion object {
        const val GROUPS_CLAIM = "cognito:groups"
        const val ROLE_PREFIX = "ROLE_"
    }
}
