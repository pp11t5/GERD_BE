package com.gerd.domain.auth.filter

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.global.apiPayload.GeneralException
import io.jsonwebtoken.Claims
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @InjectMocks
    private lateinit var filter: JwtAuthenticationFilter

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    private fun requestWithBearer(token: String) =
        MockHttpServletRequest().apply {
            addHeader(JwtProvider.AUTHORIZATION_HEADER, "${JwtProvider.BEARER}$token")
        }

    @Nested
    inner class `유효한 토큰` {

        @Test
        fun `access token이면 인증 컨텍스트가 설정된다`() {
            val claims = mock<Claims>()
            whenever(jwtProvider.validateAccessToken("access.token")).thenReturn(claims)
            whenever(jwtProvider.extractUserId(claims)).thenReturn(1L)
            whenever(claims["email"]).thenReturn("user@test.com")
            whenever(claims["nickname"]).thenReturn("tester")
            whenever(claims["role"]).thenReturn("USER")

            filter.doFilter(requestWithBearer("access.token"), MockHttpServletResponse(), MockFilterChain())

            val authentication = SecurityContextHolder.getContext().authentication

            assertThat(authentication).isNotNull
            assertThat(authentication!!.principal)
                .isInstanceOf(com.gerd.domain.auth.security.CustomUserDetails::class.java)
            val principal = authentication.principal
                as com.gerd.domain.auth.security.CustomUserDetails
            assertThat(principal.nickname).isEqualTo("tester")
        }
    }

    @Nested
    inner class `토큰 없음` {

        @Test
        fun `Authorization 헤더가 없으면 인증 컨텍스트를 비워둔 채 통과한다`() {
            filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), MockFilterChain())

            assertThat(SecurityContextHolder.getContext().authentication).isNull()
        }
    }

    @Nested
    inner class `토큰 검증 실패` {

        @Test
        fun `access token 검증 중 예외가 발생하면 그대로 전파한다`() {
            whenever(jwtProvider.validateAccessToken("access.token"))
                .thenThrow(GeneralException(AuthErrorCode.INVALID_TOKEN))

            assertThatThrownBy {
                filter.doFilter(requestWithBearer("access.token"), MockHttpServletResponse(), MockFilterChain())
            }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_TOKEN)
        }
    }
}
