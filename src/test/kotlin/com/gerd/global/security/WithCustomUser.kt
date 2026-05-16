package com.gerd.global.security

import org.springframework.security.test.context.support.WithSecurityContext

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithCustomUserSecurityContextFactory::class)
annotation class WithCustomUser(
    val userId: Long = 1L,
    val email: String = "user@test.com",
    val role: String = "USER",
)
