package com.gerd.domain.onboarding.controller

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.service.ConsentService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.WithCustomUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(controllers = [ConsentController::class])
@AutoConfigureMockMvc(addFilters = false)
class ConsentControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockitoBean
    private lateinit var consentService: ConsentService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @Nested
    inner class `POST consent` {

        @Test
        @WithCustomUser
        fun `필수 동의를 포함하면 저장에 성공한다`() {
            val request = ConsentRequestDTO(tos = true, privacy = true, healthSensitive = true, marketing = false)

            mockMvc.post("/api/v1/consent") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
            }
        }

        @Test
        @WithCustomUser
        fun `필수 약관에 미동의하면 400을 반환한다`() {
            val request = ConsentRequestDTO(tos = false, privacy = true, healthSensitive = true, marketing = false)
            doThrow(GeneralException(OnboardingErrorCode.REQUIRED_CONSENT_NOT_AGREED))
                .whenever(consentService).submitConsent(any(), any())

            mockMvc.post("/api/v1/consent") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.isSuccess") { value(false) }
                jsonPath("$.code") { value("ONBOARD400_1") }
            }
        }
    }
}
