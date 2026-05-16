package com.gerd.global.filter

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.SecurityErrorResponseWriter
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class JwtExceptionFilter : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: JwtException) {
            setErrorResponse(request, response, e)
        } catch (e: GeneralException) {
            throw e
        }
    }

    fun setErrorResponse(
        request: HttpServletRequest,
        response: HttpServletResponse,
        throwable: Throwable,
    ) {
        if (throwable is ExpiredJwtException) {
            SecurityErrorResponseWriter.write(response, AuthErrorCode.EXPIRED_TOKEN)
            return
        }

        SecurityErrorResponseWriter.write(response, AuthErrorCode.INVALID_TOKEN)
    }
}
