package com.gerd.domain.onboarding.controller

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.entity.enums.TriggerCode
import com.gerd.domain.onboarding.dto.OnboardingRequestDTO
import com.gerd.domain.onboarding.dto.OnboardingStatusResponseDTO
import com.gerd.domain.onboarding.entity.enums.SymptomCode
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.service.OnboardingService
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(controllers = [OnboardingController::class])
@AutoConfigureMockMvc(addFilters = false)
class OnboardingControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockitoBean
    private lateinit var onboardingService: OnboardingService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @Nested
    inner class `GET onboarding status` {

        @Test
        @WithCustomUser
        fun `온보딩 완료 사용자면 onboarded true를 반환한다`() {
            whenever(onboardingService.getStatus(1L)).thenReturn(OnboardingStatusResponseDTO(onboarded = true))

            mockMvc.get("/api/v1/onboarding/status")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.onboarded") { value(true) }
                }
        }
    }

    @Nested
    inner class `POST onboarding` {

        @Test
        @WithCustomUser
        fun `유효한 요청이면 201을 반환한다`() {
            val request = OnboardingRequestDTO(
                symptoms = setOf(SymptomCode.HEARTBURN_REFLUX),
                triggers = listOf(TriggerCode.CAFFEINE),
                medications = listOf("PPI"),
                customTriggerText = "오렌지주스",
            )

            mockMvc.post("/api/v1/onboarding") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON201") }
            }
        }

        @Test
        @WithCustomUser
        fun `이미 온보딩한 사용자면 409를 반환한다`() {
            doThrow(GeneralException(OnboardingErrorCode.ALREADY_ONBOARDED))
                .whenever(onboardingService).submit(any(), any())

            mockMvc.post("/api/v1/onboarding") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(OnboardingRequestDTO())
            }.andExpect {
                status { isConflict() }
                jsonPath("$.isSuccess") { value(false) }
                jsonPath("$.code") { value("ONBOARD409_1") }
            }
        }

        @Test
        @WithCustomUser
        fun `허용되지 않은 code 문자열이면 400을 반환한다`() {
            // enum 역직렬화 실패 → 공통 400, 서비스 진입 전 차단
            val rawBody = """{ "triggers": ["not_a_real_code"] }"""

            mockMvc.post("/api/v1/onboarding") {
                contentType = MediaType.APPLICATION_JSON
                content = rawBody
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.isSuccess") { value(false) }
                jsonPath("$.code") { value("COMMON400_2") }
            }
        }
    }
}
