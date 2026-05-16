package com.gerd.global.security

import com.gerd.domain.auth.exception.AuthErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

// 인증 정보 없이 인증이 필요한 리소스에 접근할 때 (401)
@Component
class CustomAuthenticationEntryPoint : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        SecurityErrorResponseWriter.write(response, AuthErrorCode.UNAUTHORIZED)
    }
}
