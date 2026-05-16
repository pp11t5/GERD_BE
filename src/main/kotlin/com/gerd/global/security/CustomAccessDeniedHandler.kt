package com.gerd.global.security

import com.gerd.domain.auth.exception.AuthErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

// 인증은 됐으나 해당 리소스에 대한 권한이 없을 때 (403)
@Component
class CustomAccessDeniedHandler : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        SecurityErrorResponseWriter.write(response, AuthErrorCode.FORBIDDEN)
    }
}
