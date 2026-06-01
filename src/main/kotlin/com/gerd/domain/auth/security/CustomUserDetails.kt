package com.gerd.domain.auth.security

import com.gerd.domain.auth.entity.enums.UserRole
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

// JWT claims(sub, email, role)에서 재구성한 일회성 인증 객체
class CustomUserDetails(
    val userId: Long,
    val email: String,
    val nickname: String?,
    val role: UserRole,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
    override fun getPassword(): String = ""
    override fun getUsername(): String = userId.toString()
}
