package com.gerd.domain.auth.service

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.entity.enums.UserStatus
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.RefreshTokenRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.auth.util.HashUtils
import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.config.properties.JwtProperties
import com.gerd.global.fixture.RefreshTokenFixture
import com.gerd.global.fixture.UserFixture
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    private lateinit var refreshTokenRotationService: RefreshTokenRotationService

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @Mock
    private lateinit var jwtProperties: JwtProperties

    @Mock
    private lateinit var notificationSettingRepository: UserNotificationSettingRepository

    @InjectMocks
    private lateinit var authService: AuthService

    @Nested
    inner class `devLogin` {

        @Nested
        inner class `성공` {

            @Test
            fun `닉네임으로 사용자를 조회해 토큰을 발급한다`() {
                val user = UserFixture.user()
                whenever(userRepository.findByNickname("qa_f_8c3e")).thenReturn(Optional.of(user))
                whenever(jwtProvider.createAccessToken(user)).thenReturn("access.token")
                whenever(jwtProvider.createRefreshToken(user))
                    .thenReturn(JwtProvider.JwtToken("refresh.token", "refresh-jti"))

                val result = authService.devLogin("qa_f_8c3e")

                assertThat(result.accessToken).isEqualTo("access.token")
                assertThat(result.refreshToken).isEqualTo("refresh.token")
                assertThat(result.userId).isEqualTo("1")
                assertThat(user.lastLoginAt).isNotNull
            }
        }

        @Nested
        inner class `신규 생성` {

            @Test
            fun `닉네임에 해당하는 사용자가 없으면 개발용 사용자를 생성하고 알림 설정도 함께 생성한다`() {
                val user = UserFixture.user()
                whenever(userRepository.findByNickname("unknown")).thenReturn(Optional.empty())
                whenever(userRepository.save(any<User>())).thenReturn(user)
                whenever(jwtProvider.createAccessToken(user)).thenReturn("access.token")
                whenever(jwtProvider.createRefreshToken(user))
                    .thenReturn(JwtProvider.JwtToken("refresh.token", "refresh-jti"))

                val result = authService.devLogin("unknown")

                assertThat(result.accessToken).isEqualTo("access.token")
                assertThat(result.refreshToken).isEqualTo("refresh.token")
                assertThat(result.userId).isEqualTo("1")
                verify(notificationSettingRepository).save(any<UserNotificationSetting>())
            }
        }
    }

    @Nested
    inner class `refresh` {

        @Nested
        inner class `성공` {

            @Test
            fun `유효한 리프레시 토큰이면 회전 서비스의 결과를 반환한다`() {
                val claims = mock<Claims>()
                val response = AuthTokenResponseDTO(
                    accessToken = "new.access.token",
                    refreshToken = "new.refresh.token",
                    userId = "1",
                    role = UserRole.USER,
                )

                whenever(jwtProvider.validateRefreshToken("refresh.token")).thenReturn(claims)
                whenever(jwtProvider.extractUserId(claims)).thenReturn(1L)
                whenever(refreshTokenRotationService.rotate(1L, "refresh.token")).thenReturn(response)

                val result = authService.refresh("refresh.token")

                assertThat(result.accessToken).isEqualTo("new.access.token")
                assertThat(result.refreshToken).isEqualTo("new.refresh.token")
                assertThat(result.userId).isEqualTo("1")
                verify(refreshTokenRotationService).rotate(1L, "refresh.token")
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `재사용된 리프레시 토큰이면 INVALID_REFRESH_TOKEN을 던진다`() {
                val claims = mock<Claims>()
                whenever(jwtProvider.validateRefreshToken("refresh.token")).thenReturn(claims)
                whenever(jwtProvider.extractUserId(claims)).thenReturn(1L)
                whenever(refreshTokenRotationService.rotate(1L, "refresh.token"))
                    .thenThrow(RefreshTokenReuseException())

                assertThatThrownBy { authService.refresh("refresh.token") }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN)

                verify(refreshTokenRotationService).rotate(1L, "refresh.token")
            }

        }
    }

    @Nested
    inner class `logout` {

        @Nested
        inner class `성공` {

            @Test
            fun `유효한 refresh token이면 삭제한다`() {
                val refreshClaims = mock<Claims>()
                whenever(jwtProvider.validateRefreshToken("refresh.token")).thenReturn(refreshClaims)
                whenever(jwtProvider.extractUserId(refreshClaims)).thenReturn(1L)
                whenever(refreshTokenRepository.findByTokenHash(HashUtils.sha256("refresh.token")))
                    .thenReturn(RefreshTokenFixture.storedToken())

                authService.logout(1L, "refresh.token")

                verify(refreshTokenRepository).deleteById(eq(1L))
            }

            @Test
            fun `만료된 refresh token이면 subject 추출 후 삭제한다`() {
                val expiredClaims = mock<Claims>()
                val expiredEx = mock<ExpiredJwtException>()
                whenever(jwtProvider.validateRefreshToken("expired.refresh.token")).thenThrow(expiredEx)
                whenever(expiredEx.claims).thenReturn(expiredClaims)
                whenever(expiredClaims.subject).thenReturn("1")
                whenever(refreshTokenRepository.findByTokenHash(HashUtils.sha256("expired.refresh.token")))
                    .thenReturn(RefreshTokenFixture.storedToken())

                authService.logout(1L, "expired.refresh.token")

                verify(refreshTokenRepository).deleteById(eq(1L))
            }

            @Test
            fun `완전히 무효한 refresh token이면 아무것도 하지 않는다`() {
                whenever(jwtProvider.validateRefreshToken("invalid.token"))
                    .thenThrow(MalformedJwtException("bad token"))

                authService.logout(1L, "invalid.token")

                verify(refreshTokenRepository, never()).deleteById(any())
            }

            @Test
            fun `다른 사용자의 refresh token이면 FORBIDDEN을 던진다`() {
                val refreshClaims = mock<Claims>()
                whenever(jwtProvider.validateRefreshToken("other.refresh.token")).thenReturn(refreshClaims)
                whenever(jwtProvider.extractUserId(refreshClaims)).thenReturn(2L)

                assertThatThrownBy { authService.logout(1L, "other.refresh.token") }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.FORBIDDEN)

                verify(refreshTokenRepository, never()).deleteById(any())
            }
        }

        @Test
        fun `로그아웃 후 같은 리프레시 토큰으로 재발급 시도 시 INVALID_REFRESH_TOKEN을 던진다`() {
            val claims = mock<Claims>()
            whenever(jwtProvider.validateRefreshToken("refresh.token")).thenReturn(claims)
            whenever(jwtProvider.extractUserId(claims)).thenReturn(1L)
            whenever(refreshTokenRotationService.rotate(1L, "refresh.token"))
                .thenThrow(RefreshTokenReuseException())

            assertThatThrownBy { authService.refresh("refresh.token") }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN)

            verify(refreshTokenRotationService).rotate(1L, "refresh.token")
        }
    }
}
