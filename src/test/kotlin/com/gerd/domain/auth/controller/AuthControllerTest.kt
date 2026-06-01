package com.gerd.domain.auth.controller

import tools.jackson.databind.ObjectMapper
import com.gerd.domain.auth.dto.OidcLoginRequestDTO
import com.gerd.domain.auth.dto.RefreshTokenRequestDTO
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.service.AuthService
import com.gerd.domain.auth.service.OAuthService
import com.gerd.domain.auth.service.WithdrawService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.global.fixture.AuthTokenFixture
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import com.gerd.global.security.WithCustomUser

@WebMvcTest(controllers = [AuthController::class])
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var oAuthService: OAuthService

    @MockitoBean
    private lateinit var withdrawService: WithdrawService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @Nested
    inner class `POST social-login` {

        @Nested
        inner class `성공` {

            @Test
            fun `지원하는 provider와 유효한 요청이면 토큰을 반환한다`() {
                val request = OidcLoginRequestDTO(idToken = "id-token")
                val response = AuthTokenFixture.userTokenResponse(
                    accessToken = "social.access.token",
                    refreshToken = "social.refresh.token",
                )
                whenever(oAuthService.socialLogin(AuthProvider.KAKAO, "id-token"))
                    .thenReturn(response)

                mockMvc.post("/api/v1/auth/kakao/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.accessToken") { value("social.access.token") }
                    jsonPath("$.result.refreshToken") { value("social.refresh.token") }
                }
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `지원하지 않는 provider면 400을 반환한다`() {
                val request = OidcLoginRequestDTO(idToken = "id-token")

                mockMvc.post("/api/v1/auth/line/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("AUTH400_2") }
                }
            }
        }
    }

    @Nested
    inner class `POST refresh` {

        @Nested
        inner class `성공` {

            @Test
            fun `유효한 리프레시 토큰이면 토큰을 재발급한다`() {
                val request = RefreshTokenRequestDTO(refreshToken = "refresh.token")
                val response = AuthTokenFixture.userTokenResponse(
                    accessToken = "new.access.token",
                    refreshToken = "new.refresh.token",
                )
                whenever(authService.refresh("refresh.token")).thenReturn(response)

                mockMvc.post("/api/v1/auth/refresh") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.accessToken") { value("new.access.token") }
                }
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `리프레시 토큰이 비어있으면 400을 반환한다`() {
                mockMvc.post("/api/v1/auth/refresh") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(mapOf("refreshToken" to ""))
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("COMMON400_1") }
                    jsonPath("$.result.refreshToken") { value("리프레시 토큰은 필수입니다.") }
                }
            }

            @Test
            fun `유효하지 않은 리프레시 토큰이면 401을 반환한다`() {
                whenever(authService.refresh("invalid.token"))
                    .thenThrow(GeneralException(AuthErrorCode.INVALID_TOKEN))

                mockMvc.post("/api/v1/auth/refresh") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(RefreshTokenRequestDTO(refreshToken = "invalid.token"))
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("AUTH401_1") }
                }
            }
        }
    }

    @Nested
    inner class `DELETE logout` {

        @Test
        @WithCustomUser
        fun `인증된 사용자가 요청하면 로그아웃에 성공한다`() {
            val request = RefreshTokenRequestDTO(refreshToken = "refresh.token")

            mockMvc.delete("/api/v1/auth/logout") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
            }
        }
    }

    @Nested
    inner class `DELETE withdraw` {

        @Test
        @WithCustomUser(userId = 7L)
        fun `인증된 사용자가 요청하면 회원 탈퇴에 성공한다`() {
            mockMvc.delete("/api/v1/auth/withdraw")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                }
        }
    }
}
