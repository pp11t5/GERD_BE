package com.gerd.global.filter

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.CustomUserDetails
import com.gerd.global.security.JwtProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        extractToken(request)
            ?.let { token ->
                val user = userRepository.findById(jwtProvider.extractUserId(token))
                    .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
                val userDetails = CustomUserDetails(user)
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
