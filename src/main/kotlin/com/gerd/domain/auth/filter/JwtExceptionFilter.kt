package com.gerd.domain.auth.filter

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.security.SecurityErrorResponseWriter
import com.gerd.global.apiPayload.GeneralException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 예외처리 filter
 */
@Component
class JwtExceptionFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: GeneralException) {
            SecurityErrorResponseWriter.write(response, e.errorCode)
        } catch (e: ExpiredJwtException) {
            SecurityErrorResponseWriter.write(response, AuthErrorCode.EXPIRED_TOKEN)
        } catch (e: JwtException) {
            SecurityErrorResponseWriter.write(response, AuthErrorCode.INVALID_TOKEN)
        }
    }
}
