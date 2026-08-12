package com.pucetec.users.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

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
