package com.gerd.domain.onboarding.controller

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.dto.TermResponseDTO
import com.gerd.domain.onboarding.entity.Term
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

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

    private fun term(id: Long, code: String, required: Boolean = true) = Term(
        id = id, code = code, version = "1.0",
        title = "${code} 약관", content = "내용",
        required = required, effectiveDate = LocalDate.of(2026, 1, 1),
    )

    @Nested
    inner class `GET terms` {

        @Test
        fun `약관 목록을 반환한다`() {
            whenever(consentService.getTerms()).thenReturn(
                listOf(
                    term(1L, "tos"),
                    term(2L, "privacy"),
                    term(3L, "health_sensitive"),
                    term(4L, "marketing", required = false),
                ),
            )

            mockMvc.get("/api/v1/consent/terms")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.result.length()") { value(4) }
                    jsonPath("$.result[0].code") { value("tos") }
                    jsonPath("$.result[0].required") { value(true) }
                    jsonPath("$.result[3].code") { value("marketing") }
                    jsonPath("$.result[3].required") { value(false) }
                }
        }
    }

    @Nested
    inner class `PATCH marketing toggle` {

        @Test
        @WithCustomUser
        fun `마케팅 동의를 토글하고 변경된 상태를 반환한다`() {
            whenever(consentService.toggleMarketing(any())).thenReturn(true)

            mockMvc.patch("/api/v1/consent/marketing/toggle")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.result") { value(true) }
                }
        }

        @Test
        @WithCustomUser
        fun `마케팅 동의 내역이 없으면 404를 반환한다`() {
            doThrow(GeneralException(OnboardingErrorCode.MARKETING_CONSENT_NOT_FOUND))
                .whenever(consentService).toggleMarketing(any())

            mockMvc.patch("/api/v1/consent/marketing/toggle")
                .andExpect {
                    status { isNotFound() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("ONBOARD404_1") }
                }
        }
    }

    @Nested
    inner class `POST consent` {

        @Test
        @WithCustomUser
        fun `필수 동의를 포함하면 저장에 성공한다`() {
            val request = ConsentRequestDTO(
                consents = listOf(
                    ConsentRequestDTO.ConsentItem(1L, true),
                    ConsentRequestDTO.ConsentItem(2L, true),
                    ConsentRequestDTO.ConsentItem(3L, true),
                    ConsentRequestDTO.ConsentItem(4L, false),
                ),
            )

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
            val request = ConsentRequestDTO(
                consents = listOf(
                    ConsentRequestDTO.ConsentItem(1L, false), // tos 미동의
                    ConsentRequestDTO.ConsentItem(2L, true),
                    ConsentRequestDTO.ConsentItem(3L, true),
                    ConsentRequestDTO.ConsentItem(4L, false),
                ),
            )
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
