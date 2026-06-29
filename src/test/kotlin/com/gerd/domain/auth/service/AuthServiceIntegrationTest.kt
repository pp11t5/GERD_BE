package com.gerd.domain.auth.service

import com.gerd.domain.auth.entity.RefreshToken
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.RefreshTokenRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.auth.util.HashUtils
import com.gerd.global.apiPayload.GeneralException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
class AuthServiceIntegrationTest @Autowired constructor(
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
) {

    @AfterEach
    fun tearDown() {
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Nested
    inner class `refresh` {

        @Test
        fun `저장되지 않은 유효한 리프레시 토큰이면 전체 세션 삭제가 먼저 커밋된다`() {
            val user = userRepository.save(
                User(email = "refresh-user@test.com", nickname = "refresh-user", role = UserRole.USER),
            )
            val storedToken = jwtProvider.createRefreshToken(user)
            refreshTokenRepository.save(
                RefreshToken(
                    userId = user.id!!,
                    jti = storedToken.jti,
                    tokenHash = HashUtils.sha256(storedToken.value),
                    expiresAt = LocalDateTime.now().plusDays(1),
                ),
            )
            val unknownButValidToken = jwtProvider.createRefreshToken(user)

            assertThatThrownBy { authService.refresh(unknownButValidToken.value) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN)

            assertThat(refreshTokenRepository.findById(user.id!!)).isEmpty
        }
    }
}
