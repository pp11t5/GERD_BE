package com.gerd.domain.auth.controller

import tools.jackson.databind.ObjectMapper
import com.gerd.domain.auth.dto.DevLoginRequestDTO
import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.service.AuthService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.JwtProvider
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

@WebMvcTest(controllers = [AuthController::class])
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @Nested
    inner class `POST dev-login` {

        @Nested
        inner class `성공` {

            @Test
            fun `유효한 이메일로 요청하면 토큰을 반환한다`() {
                val request = DevLoginRequestDTO(email = "user@test.com", password = "dev1234!")
                val response = AuthTokenResponseDTO(
                    accessToken = "access.token.value",
                    refreshToken = "refresh.token.value",
                    userId = 1L,
                    email = "user@test.com",
                    role = UserRole.USER,
                )
                whenever(authService.devLogin("user@test.com", "dev1234!")).thenReturn(response)

                mockMvc.post("/api/v1/auth/dev-login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.accessToken") { value("access.token.value") }
                    jsonPath("$.result.userId") { value(1) }
                    jsonPath("$.result.email") { value("user@test.com") }
                    jsonPath("$.result.role") { value("USER") }
                }
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `존재하지 않는 이메일이면 404를 반환한다`() {
                val request = DevLoginRequestDTO(email = "notfound@test.com", password = "dev1234!")
                whenever(authService.devLogin("notfound@test.com", "dev1234!"))
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
            fun `이메일이 비어있으면 400을 반환한다`() {
                val request = mapOf("email" to "", "password" to "dev1234!")

                mockMvc.post("/api/v1/auth/dev-login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("COMMON400_1") }
                }
            }

            @Test
            fun `이메일 형식이 올바르지 않으면 400을 반환한다`() {
                val request = mapOf("email" to "not-an-email", "password" to "dev1234!")

                mockMvc.post("/api/v1/auth/dev-login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("COMMON400_1") }
                }
            }

            @Test
            fun `비밀번호가 비어있으면 400을 반환한다`() {
                val request = mapOf("email" to "user@test.com", "password" to "")

                mockMvc.post("/api/v1/auth/dev-login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("COMMON400_1") }
                }
            }
        }
    }
}
