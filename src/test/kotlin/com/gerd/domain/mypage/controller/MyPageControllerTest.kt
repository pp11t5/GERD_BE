package com.gerd.domain.mypage.controller

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.mypage.dto.MealCount
import com.gerd.domain.mypage.dto.MedicalInfoResponseDTO
import com.gerd.domain.mypage.dto.MedicalInfoUpdateRequestDTO
import com.gerd.domain.mypage.dto.MyPageSummaryResponseDTO
import com.gerd.domain.mypage.dto.WeeklyReportResponseDTO
import com.gerd.domain.mypage.service.MyPageService
import com.gerd.domain.onboarding.entity.enums.DiseaseType
import com.gerd.domain.report.service.ReportService
import com.gerd.global.security.WithCustomUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import tools.jackson.databind.ObjectMapper

@WebMvcTest(controllers = [MyPageController::class])
@AutoConfigureMockMvc(addFilters = false)
class MyPageControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockitoBean
    private lateinit var myPageService: MyPageService

    @MockitoBean
    private lateinit var reportService: ReportService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @Nested
    inner class `마이페이지 요약 조회` {

        @Test
        @WithCustomUser(userId = 1L)
        fun `인증 사용자의 요약 정보를 ApiResponse로 반환한다`() {
            whenever(myPageService.getProfileSummary(1L)).thenReturn(summaryResponse())

            mockMvc.get("/api/v1/my-page/summary")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.profile.nickName") { value("위장이") }
                    jsonPath("$.result.weeklySummary.mealCount.recommendCount") { value(3) }
                }
        }
    }

    @Nested
    inner class `건강 정보 수정` {

        @Test
        @WithCustomUser(userId = 1L)
        fun `유효한 요청이면 건강 정보를 교체하고 성공 응답을 반환한다`() {
            whenever(myPageService.updateHealthInfo(any(), any())).thenReturn(
                MedicalInfoResponseDTO(
                    allergies = listOf("우유"),
                    medications = listOf("PPI"),
                ),
            )
            val request = MedicalInfoUpdateRequestDTO(
                allergens = listOf(AllergenCode.MILK),
                medications = listOf("PPI"),
            )

            mockMvc.patch("/api/v1/my-page/health-info") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
            }

            verify(myPageService).updateHealthInfo(1L, request)
        }

        @Test
        @WithCustomUser(userId = 1L)
        fun `복용약 이름이 공백이면 400 응답을 반환한다`() {
            val request = MedicalInfoUpdateRequestDTO(
                allergens = emptyList(),
                medications = listOf("   "),
            )

            mockMvc.patch("/api/v1/my-page/health-info") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.isSuccess") { value(false) }
            }
        }

    }

    @Nested
    inner class `리포트 조회` {

        @Test
        @WithCustomUser(userId = 1L)
        fun `지난주 리포트가 없으면 result null로 성공 응답을 반환한다`() {
            whenever(reportService.getReport(1L)).thenReturn(null)

            mockMvc.get("/api/v1/my-page/reports")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result") { doesNotExist() }
                }
        }

        @Test
        @WithCustomUser(userId = 1L)
        fun `지난주 리포트가 있으면 상세 내용을 반환한다`() {
            whenever(reportService.getReport(1L)).thenReturn(reportResponse())

            mockMvc.get("/api/v1/my-page/reports")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.weekLabel") { value("2026년 6월 넷째주") }
                    jsonPath("$.result.mealCount.riskCount") { value(1) }
                }
        }
    }

    private fun summaryResponse() = MyPageSummaryResponseDTO(
        profile = MyPageSummaryResponseDTO.ProfileSummary(
            nickName = "위장이",
            profileImage = null,
            disease = DiseaseType.GERD,
        ),
        foodHistory = MyPageSummaryResponseDTO.FoodHistory(
            safeCount = 10,
            cautionCount = 2,
        ),
        weeklySummary = MyPageSummaryResponseDTO.WeeklySummary(
            mealRecordCount = 5,
            recentSymptomCount = 4,
            streakCount = 3,
            mealCount = MealCount(3, 1, 1),
        ),
    )

    private fun reportResponse() = WeeklyReportResponseDTO(
        startDate = "2026-06-21",
        endDate = "2026-06-27",
        weekLabel = "2026년 6월 넷째주",
        comfortableState = WeeklyReportResponseDTO.ComfortableState(
            streakCount = 2,
            recommendedMealCount = 4,
            percentage = 66.7,
        ),
        mealCount = MealCount(4, 2, 1),
    )
}
