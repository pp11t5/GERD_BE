package com.gerd.domain.auth.service

import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.repository.RefreshTokenRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.auth.util.HashUtils
import com.gerd.global.config.properties.JwtProperties
import com.gerd.global.fixture.RefreshTokenFixture
import com.gerd.global.fixture.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class RefreshTokenRotationServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @Mock
    private lateinit var jwtProperties: JwtProperties

    @InjectMocks
    private lateinit var refreshTokenRotationService: RefreshTokenRotationService

    @Nested
    inner class `rotate` {

        @Test
        fun `현재 사용자 토큰 행을 잠근 뒤 일치하는 토큰을 회전한다`() {
            val user = UserFixture.user()
            val storedToken = RefreshTokenFixture.storedToken()
            val newRefreshToken = JwtProvider.JwtToken("new.refresh.token", "new-jti")

            whenever(refreshTokenRepository.findByUserIdForUpdate(1L)).thenReturn(storedToken)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
            whenever(jwtProvider.createAccessToken(user)).thenReturn("new.access.token")
            whenever(jwtProvider.createRefreshToken(user)).thenReturn(newRefreshToken)
            whenever(jwtProperties.refreshExpirationMs).thenReturn(86_400_000)

            val result = refreshTokenRotationService.rotate(1L, "refresh.token")

            assertThat(result.accessToken).isEqualTo("new.access.token")
            assertThat(result.refreshToken).isEqualTo("new.refresh.token")
            assertThat(result.role).isEqualTo(UserRole.USER)
            assertThat(storedToken.jti).isEqualTo("new-jti")
            assertThat(storedToken.tokenHash).isEqualTo(HashUtils.sha256("new.refresh.token"))
            verify(refreshTokenRepository).findByUserIdForUpdate(1L)
        }

        @Test
        fun `다른 토큰이 제출되면 잠긴 현재 세션을 삭제하고 재사용 예외를 던진다`() {
            val storedToken = RefreshTokenFixture.storedToken()
            whenever(refreshTokenRepository.findByUserIdForUpdate(1L)).thenReturn(storedToken)

            assertThatThrownBy { refreshTokenRotationService.rotate(1L, "reused.refresh.token") }
                .isInstanceOf(RefreshTokenReuseException::class.java)

            verify(refreshTokenRepository).delete(storedToken)
        }

        @Test
        fun `현재 사용자 토큰 행이 없으면 재사용 예외를 던진다`() {
            whenever(refreshTokenRepository.findByUserIdForUpdate(1L)).thenReturn(null)

            assertThatThrownBy { refreshTokenRotationService.rotate(1L, "refresh.token") }
                .isInstanceOf(RefreshTokenReuseException::class.java)
        }
    }
}
