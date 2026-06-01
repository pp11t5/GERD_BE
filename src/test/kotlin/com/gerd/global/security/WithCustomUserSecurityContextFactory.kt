package com.gerd.global.security

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.auth.entity.enums.UserRole
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory

class WithCustomUserSecurityContextFactory : WithSecurityContextFactory<WithCustomUser> {

    override fun createSecurityContext(annotation: WithCustomUser): SecurityContext {
        val principal = CustomUserDetails(
            userId = annotation.userId,
            email = annotation.email,
            nickname = annotation.nickname,
            role = UserRole.valueOf(annotation.role),
        )
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)

        return SecurityContextHolder.createEmptyContext().also { it.authentication = auth }
    }
}
