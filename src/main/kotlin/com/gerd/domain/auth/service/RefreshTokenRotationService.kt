package com.gerd.domain.auth.service

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.RefreshTokenRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.auth.util.HashUtils
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.config.properties.JwtProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RefreshTokenRotationService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
) {

    // 재사용 토큰이면 삭제를 커밋한 뒤 호출자에게 무효 토큰으로 변환을 맡긴다.
    @Transactional(noRollbackFor = [RefreshTokenReuseException::class])
    fun rotate(userId: Long, refreshToken: String): AuthTokenResponseDTO {
        val storedToken = refreshTokenRepository.findByUserIdForUpdate(userId)
            ?: throw RefreshTokenReuseException()

        if (storedToken.tokenHash != HashUtils.sha256(refreshToken)) {
            refreshTokenRepository.delete(storedToken)
            throw RefreshTokenReuseException()
        }

        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        val accessToken = jwtProvider.createAccessToken(user)
        val newRefreshToken = jwtProvider.createRefreshToken(user)

        storedToken.rotate(
            jti = newRefreshToken.jti,
            tokenHash = HashUtils.sha256(newRefreshToken.value),
            expiresAt = LocalDateTime.now().plusSeconds(jwtProperties.refreshExpirationMs / 1000),
        )

        return AuthTokenResponseDTO(
            accessToken = accessToken,
            refreshToken = newRefreshToken.value,
            userId = userId.toString(),
            role = user.role,
        )
    }
}
