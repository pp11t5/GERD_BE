package com.gerd.domain.judgment.controller

import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.JudgmentItemDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.StateRecordsDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.judgment.service.FoodJudgmentQueryService
import com.gerd.domain.symptom.dto.FoodSymptomResponseDTO
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.domain.symptom.service.FoodSymptomQueryService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.FoodFixture
import com.gerd.global.fixture.MealRecordFixture
import com.gerd.global.fixture.SymptomFixture
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
    @MockitoBean private lateinit var foodSymptomQueryService: FoodSymptomQueryService
    @MockitoBean private lateinit var jwtProvider: JwtProvider

    private val foodExternalId = FoodFixture.EXTERNAL_ID.toString()

    private val response = JudgmentResponseDTO(
        foodExternalId = foodExternalId,
        foodName = "아메리카노",
        category = "beverage",
        grade = JudgmentGrade.CAUTION,
        personalTitle = "속이 편안할 수 있도록 천천히 드세요!",
        items = listOf(
            JudgmentItemDTO("카페인이 들어 있어요", "등록하신 커피류 트리거에 해당해요."),
            JudgmentItemDTO("알레르기 해당 없어요", "알레르기 성분이 포함되지 않았어요."),
        ),
        stateRecords = StateRecordsDTO(total = 0, records = emptyList()),
        substitutes = listOf(SubstituteDTO(foodExternalId, "디카페인 아메리카노")),
    )

    @Nested
    inner class `GET judgment` {

        @Nested
        inner class 성공 {

            @Test
            @WithCustomUser
            fun `신호등 판정을 반환한다`() {
                whenever(foodJudgmentQueryService.getJudgment(any(), any())).thenReturn(response to true)

                mockMvc.get("/api/v1/foods/$foodExternalId/judgment").andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.foodExternalId") { value(foodExternalId) }
                    jsonPath("$.result.category") { value("beverage") }
                    jsonPath("$.result.grade") { value("CAUTION") }
                    jsonPath("$.result.items.length()") { value(2) }
                    jsonPath("$.result.substitutes[0].name") { value("디카페인 아메리카노") }
                    jsonPath("$.result.stateRecords.total") { value(0) }
                    jsonPath("$.result.stateRecords.records.length()") { value(0) }
                    header { string("X-Cache", "HIT") }
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

    @Nested
    inner class `GET food symptoms` {

        @Test
        @WithCustomUser
        fun `음식에 연결된 증상 목록을 반환한다`() {
            whenever(foodSymptomQueryService.getSymptoms(any(), any())).thenReturn(
                listOf(
                    FoodSymptomResponseDTO(
                        symptomId = SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(),
                        symptomState = SymptomState.UNCOMFORTABLE,
                        symptomTypes = listOf(SymptomType.ACID_REFLUX),
                        occurredAt = "2026-06-21T13:00:00",
                        mealRecordId = MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
                        afterMealMinutes = 70,
                    ),
                ),
            )

            mockMvc.get("/api/v1/foods/$foodExternalId/symptoms").andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.result[0].symptomId") { value(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString()) }
                jsonPath("$.result[0].symptomState") { value("uncomfortable") }
                jsonPath("$.result[0].symptomTypes[0]") { value("acid_reflux") }
                jsonPath("$.result[0].mealRecordId") { value(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString()) }
                jsonPath("$.result[0].afterMealMinutes") { value(70) }
            }
        }
    }
}
