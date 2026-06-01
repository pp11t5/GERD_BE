package com.gerd.domain.auth.service

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.dto.UserMeResponseDTO
import com.gerd.domain.auth.entity.RefreshToken
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserStatus
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.RefreshTokenRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.auth.util.HashUtils
import com.gerd.global.config.properties.JwtProperties
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
) {

    @Transactional
    fun devLogin(nickname: String): AuthTokenResponseDTO {
        val user = userRepository.findByNickname(nickname)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        when (user.status) {
            UserStatus.INACTIVE -> throw GeneralException(AuthErrorCode.ACCOUNT_INACTIVE)
            UserStatus.DELETED  -> throw GeneralException(AuthErrorCode.ACCOUNT_RECOVERABLE)
            else -> {}
        }
        user.updateLastLoginAt()
        return issueTokens(user)
    }

    @Transactional(propagation = Propagation.REQUIRED)
    fun issueTokens(user: User): AuthTokenResponseDTO {
        val accessToken = jwtProvider.createAccessToken(user)
        val refreshToken = jwtProvider.createRefreshToken(user)
        val userId = user.id ?: throw GeneralException(AuthErrorCode.USER_NOT_FOUND)

        // userId가 PK — save()가 upsert로 동작 (새 로그인 시 기존 세션 교체)
        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                jti = refreshToken.jti,
                tokenHash = HashUtils.sha256(refreshToken.value),
                expiresAt = LocalDateTime.now().plusSeconds(jwtProperties.refreshExpirationMs / 1000),
            )
        )

        return AuthTokenResponseDTO(
            accessToken = accessToken,
            refreshToken = refreshToken.value,
            userId = userId.toString(),
            email = user.email,
            role = user.role,
        )
    }

    // JWT 형식 검증 → DB 조회 (없으면 전체 로그아웃) → 상태 확인 → 토큰 로테이션
    @Transactional
    fun refresh(refreshToken: String): AuthTokenResponseDTO {
        val claims = jwtProvider.validateRefreshToken(refreshToken)
        val userId = jwtProvider.extractUserId(claims)

        findStoredTokenOrDeleteAll(HashUtils.sha256(refreshToken), userId)

        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        when (user.status) {
            UserStatus.DELETED -> throw GeneralException(AuthErrorCode.ACCOUNT_RECOVERABLE)
            else -> {}
        }

        return issueTokens(user)
    }

    // DB에 없는 refresh token → 탈취 의심 → 해당 유저 전체 로그아웃
    private fun findStoredTokenOrDeleteAll(tokenHash: String, userId: Long): RefreshToken {
        return refreshTokenRepository.findByTokenHash(tokenHash)
            ?: run {
                refreshTokenRepository.deleteAllByUserId(userId)
                throw GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN)
            }
    }

    fun getMe(userId: Long): UserMeResponseDTO {
        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        return UserMeResponseDTO(
            userId = user.id!!.toString(),
            nickname = user.nickname,
            email = user.email,
            profileImage = user.profileImage,
        )
    }

    // 만료 여부에 상관없이 subject 확인 — JwtException(완전히 위조된 토큰)이면 무시
    @Transactional
    fun logout(currentUserId: Long, refreshToken: String) {
        val refreshSubjectId: Long? = try {
            jwtProvider.extractUserId(jwtProvider.validateRefreshToken(refreshToken))
        } catch (e: ExpiredJwtException) {
            e.claims.subject.toLong()
        } catch (e: JwtException) {
            null
        }

        if (refreshSubjectId == null) return
        if (refreshSubjectId != currentUserId) throw GeneralException(AuthErrorCode.FORBIDDEN)

        // hash 일치 시에만 삭제 — 만료된 이전 토큰 제출 시 무시
        refreshTokenRepository.findByTokenHash(HashUtils.sha256(refreshToken))
            ?.let { refreshTokenRepository.deleteById(currentUserId) }
    }

}
