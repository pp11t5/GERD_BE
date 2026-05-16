package com.gerd.domain.auth.service

import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.fixture.UserFixture
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.JwtProvider
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
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var authService: AuthService

    @Nested
    inner class `devLogin` {

        @Nested
        inner class `성공` {

            @Test
            fun `이메일로 사용자를 조회해 토큰을 발급한다`() {
                val user = UserFixture.user()
                whenever(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user))
                whenever(passwordEncoder.matches(eq("dev1234!"), any())).thenReturn(true)
                whenever(jwtProvider.createAccessToken(user)).thenReturn("access.token")
                whenever(jwtProvider.createRefreshToken(user)).thenReturn("refresh.token")

                val result = authService.devLogin("user@test.com", "dev1234!")

                assertThat(result.accessToken).isEqualTo("access.token")
                assertThat(result.refreshToken).isEqualTo("refresh.token")
                assertThat(result.userId).isEqualTo(1L)
                assertThat(result.email).isEqualTo("user@test.com")
                assertThat(result.role).isEqualTo(UserRole.USER)
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `이메일에 해당하는 사용자가 없으면 USER_NOT_FOUND를 던진다`() {
                whenever(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty())

                assertThatThrownBy { authService.devLogin("notfound@test.com", "dev1234!") }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
            }

            @Test
            fun `비밀번호가 일치하지 않으면 INVALID_PASSWORD를 던진다`() {
                val user = UserFixture.user()
                whenever(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user))
                whenever(passwordEncoder.matches("wrong-password", user.password)).thenReturn(false)

                assertThatThrownBy { authService.devLogin("user@test.com", "wrong-password") }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.INVALID_PASSWORD)
            }
        }
    }
}
