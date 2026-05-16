package com.gerd.global.annotation

import com.gerd.domain.auth.exception.AuthErrorCode

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ApiErrorExample(
    vararg val value: AuthErrorCode,
)
