package com.gerd.domain.judgment.controller

import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.JudgmentItemDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.judgment.service.FoodJudgmentQueryService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.FoodFixture
import com.gerd.global.security.WithCustomUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(controllers = [JudgmentController::class])
@AutoConfigureMockMvc(addFilters = false)
class JudgmentControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean private lateinit var foodJudgmentQueryService: FoodJudgmentQueryService
    @MockitoBean private lateinit var jwtProvider: JwtProvider

    private val foodExternalId = FoodFixture.EXTERNAL_ID.toString()

    private val response = JudgmentResponseDTO(
        foodExternalId = foodExternalId,
        foodName = "아메리카노",
        grade = JudgmentGrade.CAUTION,
        personalTitle = "속이 편안할 수 있도록 천천히 드세요!",
        items = listOf(
            JudgmentItemDTO("카페인이 들어 있어요", "등록하신 커피류 트리거에 해당해요."),
            JudgmentItemDTO("알레르기 해당 없어요", "알레르기 성분이 포함되지 않았어요."),
        ),
        stateRecords = emptyList(),
        substitutes = listOf(SubstituteDTO(foodExternalId, "디카페인 아메리카노")),
        disclaimer = "본 앱은 진단·치료 서비스가 아닙니다.",
        cached = true,
    )

    @Nested
    inner class `GET judgment` {

        @Nested
        inner class 성공 {

            @Test
            @WithCustomUser
            fun `신호등 판정을 반환한다`() {
                whenever(foodJudgmentQueryService.getJudgment(any(), any())).thenReturn(response)

                mockMvc.get("/api/v1/foods/$foodExternalId/judgment").andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.foodExternalId") { value(foodExternalId) }
                    jsonPath("$.result.grade") { value("CAUTION") }
                    jsonPath("$.result.items.length()") { value(2) }
                    jsonPath("$.result.substitutes[0].name") { value("디카페인 아메리카노") }
                    jsonPath("$.result.stateRecords.length()") { value(0) }
                    jsonPath("$.result.cached") { value(true) }
                }
            }
        }

        @Nested
        inner class 실패 {

            @Test
            @WithCustomUser
            fun `음식이 없으면 FOOD404_1을 반환한다`() {
                whenever(foodJudgmentQueryService.getJudgment(any(), any()))
                    .thenThrow(GeneralException(FoodErrorCode.FOOD_NOT_FOUND))

                mockMvc.get("/api/v1/foods/$foodExternalId/judgment").andExpect {
                    status { isNotFound() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("FOOD404_1") }
                }
            }
        }
    }
}
