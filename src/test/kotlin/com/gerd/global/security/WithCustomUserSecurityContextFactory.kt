package com.gerd.global.security

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory
import org.springframework.test.util.ReflectionTestUtils

class WithCustomUserSecurityContextFactory : WithSecurityContextFactory<WithCustomUser> {

    override fun createSecurityContext(annotation: WithCustomUser): SecurityContext {
        val user = User(email = annotation.email, role = UserRole.valueOf(annotation.role))
        ReflectionTestUtils.setField(user, "id", annotation.userId)

        val principal = CustomUserDetails(user)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)

        return SecurityContextHolder.createEmptyContext().also { it.authentication = auth }
    }
}
