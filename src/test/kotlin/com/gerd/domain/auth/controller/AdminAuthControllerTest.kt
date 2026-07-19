package com.gerd.domain.auth.controller

import tools.jackson.databind.ObjectMapper
import com.gerd.domain.auth.dto.AdminLoginRequestDTO
import com.gerd.domain.auth.dto.AdminLoginResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.auth.service.AdminAuthService
import com.gerd.global.apiPayload.GeneralException
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

@WebMvcTest(controllers = [AdminAuthController::class])
@AutoConfigureMockMvc(addFilters = false)
class AdminAuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockitoBean
    private lateinit var adminAuthService: AdminAuthService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @Nested
    inner class `POST admin-login` {

        @Nested
        inner class `성공` {

            @Test
            fun `올바른 이메일과 시크릿이면 액세스 토큰을 반환한다`() {
                val request = AdminLoginRequestDTO(email = "admin@test.com", secret = "secret123")
                whenever(adminAuthService.login("admin@test.com", "secret123"))
                    .thenReturn(AdminLoginResponseDTO(accessToken = "admin.access.token"))

                mockMvc.post("/api/v1/auth/admin/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.accessToken") { value("admin.access.token") }
                }
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `이메일이 비어있으면 400을 반환한다`() {
                mockMvc.post("/api/v1/auth/admin/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(mapOf("email" to "", "secret" to "secret123"))
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("COMMON400_1") }
                }
            }

            @Test
            fun `시크릿이 비어있으면 400을 반환한다`() {
                mockMvc.post("/api/v1/auth/admin/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(mapOf("email" to "admin@test.com", "secret" to ""))
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("COMMON400_1") }
                }
            }

            @Test
            fun `자격증명이 올바르지 않으면 401을 반환한다`() {
                val request = AdminLoginRequestDTO(email = "admin@test.com", secret = "wrong-secret")
                whenever(adminAuthService.login("admin@test.com", "wrong-secret"))
                    .thenThrow(GeneralException(AuthErrorCode.INVALID_ADMIN_CREDENTIALS))

                mockMvc.post("/api/v1/auth/admin/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("AUTH401_6") }
                }
            }
        }
    }
}
