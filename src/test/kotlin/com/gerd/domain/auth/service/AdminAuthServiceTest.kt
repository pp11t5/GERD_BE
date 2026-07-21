package com.gerd.domain.auth.service

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.config.properties.AdminProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AdminAuthServiceTest {

    @Mock
    private lateinit var adminProperties: AdminProperties

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @InjectMocks
    private lateinit var adminAuthService: AdminAuthService

    @Nested
    inner class `login` {

        @Nested
        inner class `성공` {

            @Test
            fun `올바른 이메일과 시크릿이면 액세스 토큰을 반환한다`() {
                whenever(adminProperties.email).thenReturn("admin@test.com")
                whenever(adminProperties.secret).thenReturn("secret123")
                whenever(jwtProvider.createAdminAccessToken("admin@test.com")).thenReturn("admin.access.token")

                val result = adminAuthService.login("admin@test.com", "secret123")

                assertThat(result.accessToken).isEqualTo("admin.access.token")
                verify(jwtProvider).createAdminAccessToken("admin@test.com")
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `이메일이 다르면 INVALID_ADMIN_CREDENTIALS를 던진다`() {
                whenever(adminProperties.email).thenReturn("admin@test.com")
                whenever(adminProperties.secret).thenReturn("secret123")

                assertThatThrownBy { adminAuthService.login("wrong@test.com", "secret123") }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.INVALID_ADMIN_CREDENTIALS)
            }

            @Test
            fun `시크릿이 다르면 INVALID_ADMIN_CREDENTIALS를 던진다`() {
                whenever(adminProperties.email).thenReturn("admin@test.com")
                whenever(adminProperties.secret).thenReturn("secret123")

                assertThatThrownBy { adminAuthService.login("admin@test.com", "wrong-secret") }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.INVALID_ADMIN_CREDENTIALS)
            }

            @Test
            fun `ADMIN_EMAIL 환경변수가 비어있으면 INVALID_ADMIN_CREDENTIALS를 던진다`() {
                whenever(adminProperties.email).thenReturn("")

                assertThatThrownBy { adminAuthService.login("admin@test.com", "secret123") }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.INVALID_ADMIN_CREDENTIALS)
            }

            @Test
            fun `ADMIN_SECRET 환경변수가 비어있으면 INVALID_ADMIN_CREDENTIALS를 던진다`() {
                whenever(adminProperties.email).thenReturn("admin@test.com")
                whenever(adminProperties.secret).thenReturn("")

                assertThatThrownBy { adminAuthService.login("admin@test.com", "secret123") }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.INVALID_ADMIN_CREDENTIALS)
            }
        }
    }
}
