package com.gerd.domain.auth.controller

import tools.jackson.databind.ObjectMapper
import com.gerd.domain.auth.dto.DevLoginRequestDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.auth.service.AuthService
import com.gerd.global.apiPayload.GeneralException
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
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [DevAuthController::class])
@AutoConfigureMockMvc(addFilters = false)
class DevAuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @Nested
    inner class `POST dev-login` {

        @Nested
        inner class `성공` {

            @Test
            fun `유효한 닉네임으로 요청하면 토큰을 반환한다`() {
                val request = DevLoginRequestDTO(nickname = "dev-user")
                val response = AuthTokenFixture.userTokenResponse()
                whenever(authService.devLogin("dev-user")).thenReturn(response)

                mockMvc.post("/api/v1/auth/dev-login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.accessToken") { value("access.token") }
                    jsonPath("$.result.userId") { value("1") }
                    jsonPath("$.result.role") { value("USER") }
                }
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `존재하지 않는 닉네임이면 404를 반환한다`() {
                val request = DevLoginRequestDTO(nickname = "unknown")
                whenever(authService.devLogin("unknown"))
                    .thenThrow(GeneralException(AuthErrorCode.USER_NOT_FOUND))

                mockMvc.post("/api/v1/auth/dev-login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isNotFound() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("AUTH404_1") }
                }
            }

            @Test
            fun `닉네임이 비어있으면 400을 반환한다`() {
                mockMvc.post("/api/v1/auth/dev-login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(mapOf("nickname" to ""))
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("COMMON400_1") }
                }
            }
        }
    }
}
