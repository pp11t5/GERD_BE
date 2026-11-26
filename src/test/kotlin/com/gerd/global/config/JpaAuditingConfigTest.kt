package com.gerd.global.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

class JpaAuditingConfigTest {

    private val auditorAware = JpaAuditingConfig().auditorAware()

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `CustomUserDetails가 아닌 주체여도 예외 없이 auditor 없음으로 처리한다`() {
        val principal = User("admin@gerd.com", "pw", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)

        assertThat(auditorAware.currentAuditor).isEmpty
    }
}
