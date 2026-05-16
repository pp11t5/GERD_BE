package com.gerd.global.security

import com.gerd.domain.auth.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
    val user: User,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
    override fun getPassword(): String = ""
    override fun getUsername(): String = user.id.toString()
}
