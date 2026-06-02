package com.gerd.domain.auth.filter

import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.global.apiPayload.GeneralException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// JWT claims에서 바로 인증 객체 구성 — DB 조회 없이 stateless하게 처리
@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        extractToken(request)
            ?.let { token ->
                val claims = jwtProvider.validateAccessToken(token)

                val email = claims["email"] as? String
                    ?: throw GeneralException(AuthErrorCode.INVALID_TOKEN)
                val role = (claims["role"] as? String)?.let { UserRole.valueOf(it) }
                    ?: throw GeneralException(AuthErrorCode.INVALID_TOKEN)

                val userDetails = CustomUserDetails(
                    userId = jwtProvider.extractUserId(claims),
                    email = email,
                    nickname = claims["nickname"] as? String,
                    role = role,
                )

                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
            }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? =
        request.getHeader(JwtProvider.AUTHORIZATION_HEADER)
            ?.takeIf { it.startsWith(JwtProvider.BEARER) }
            ?.substring(JwtProvider.BEARER.length)
}
