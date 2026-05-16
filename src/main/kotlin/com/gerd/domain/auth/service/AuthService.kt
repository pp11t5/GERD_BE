package com.gerd.domain.auth.service

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.JwtProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional(readOnly = true)
    fun devLogin(email: String, password: String): AuthTokenResponseDTO {
        val user = userRepository.findByEmail(email)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        if (!passwordEncoder.matches(password, user.password)) {
            throw GeneralException(AuthErrorCode.INVALID_PASSWORD)
        }

        return AuthTokenResponseDTO(
            accessToken = jwtProvider.createAccessToken(user),
            refreshToken = jwtProvider.createRefreshToken(user),
            userId = user.id ?: throw GeneralException(AuthErrorCode.USER_NOT_FOUND),
            email = user.email,
            role = user.role,
        )
    }

    @Transactional(readOnly = true)
    fun refresh(refreshToken: String): AuthTokenResponseDTO {
        if (!jwtProvider.validateToken(refreshToken)) {
            val errorCode = if (jwtProvider.isExpiredToken(refreshToken)) {
                AuthErrorCode.EXPIRED_TOKEN
            } else {
                AuthErrorCode.INVALID_TOKEN
            }
            throw GeneralException(errorCode)
        }

        val userId = jwtProvider.extractUserId(refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        return AuthTokenResponseDTO(
            accessToken = jwtProvider.createAccessToken(user),
            refreshToken = jwtProvider.createRefreshToken(user),
            userId = user.id ?: throw GeneralException(AuthErrorCode.USER_NOT_FOUND),
            email = user.email,
            role = user.role,
        )
    }
}
