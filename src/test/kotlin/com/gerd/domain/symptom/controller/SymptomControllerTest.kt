package com.gerd.domain.symptom.controller

import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.symptom.dto.SymptomResponseDTO
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.domain.symptom.exception.SymptomErrorCode
import com.gerd.domain.symptom.service.SymptomService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.MealRecordFixture
import com.gerd.global.fixture.SymptomFixture
import com.gerd.global.security.WithCustomUser
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@WebMvcTest(controllers = [SymptomController::class])
@AutoConfigureMockMvc(addFilters = false)
class SymptomControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var symptomService: SymptomService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @Nested
    inner class `POST symptoms` {

        @Test
        @WithCustomUser
        fun `증상 기록을 생성하면 상세를 반환한다`() {
            whenever(symptomService.create(any(), any())).thenReturn(symptomResponse())

            mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "comfortable",
                      "symptomTypes": [],
                      "occurredAt": "2026-05-12T19:30:00+09:00",
                      "mealRecordId": "${MealRecordFixture.MEAL_RECORD_EXTERNAL_ID}",
                      "memo": "속이 편했어요"
                    }
                """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
                jsonPath("$.result.symptomId") { value(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString()) }
                jsonPath("$.result.linkedMeal.mealRecordId") { value(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString()) }
                jsonPath("$.result.linkedMeal.foods[0].mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
                jsonPath("$.result.memo") { doesNotExist() }
            }
        }

        @Test
        @WithCustomUser
        fun `끼니 식별자가 UUID 형식이 아니면 COMMON400_1`() {
            mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "comfortable",
                      "symptomTypes": [],
                      "occurredAt": "2026-05-12T19:30:00+09:00",
                      "mealRecordId": "bad"
                    }
                """.trimIndent()
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("COMMON400_1") }
            }

            verify(symptomService, never()).create(any(), any())
        }

        @Test
        @WithCustomUser
        fun `필수 값이 누락되면 validation 메시지를 반환한다`() {
            mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomTypes": [],
                      "occurredAt": "2026-05-12T19:30:00+09:00",
                      "mealRecordId": ""
                    }
                """.trimIndent()
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("COMMON400_1") }
                jsonPath("$.result.symptomState") { value("증상 상태는 필수입니다.") }
                jsonPath("$.result.mealRecordId") { exists() }
            }

            verify(symptomService, never()).create(any(), any())
        }

        @Test
        @WithCustomUser
        fun `증상 발생 시각 형식이 올바르지 않으면 COMMON400_1`() {
            mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "comfortable",
                      "symptomTypes": [],
                      "occurredAt": "2026-05-12 19:30:00",
                      "mealRecordId": "${MealRecordFixture.MEAL_RECORD_EXTERNAL_ID}"
                    }
                """.trimIndent()
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("COMMON400_1") }
                jsonPath("$.result.occurredAt") { value("증상 발생 시각은 ISO-8601 offset 형식이어야 합니다.") }
            }

            verify(symptomService, never()).create(any(), any())
        }
    }

    @Nested
    inner class `GET symptoms` {

        @Test
        @WithCustomUser
        fun `증상 기록 상세를 조회한다`() {
            whenever(symptomService.getDetail(any(), any())).thenReturn(symptomResponse())

            mockMvc.get("/api/v1/symptoms/${SymptomFixture.SYMPTOM_EXTERNAL_ID}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.symptomId") { value(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString()) }
                    jsonPath("$.result.analysis.title") { value("유진 님을 위한 맞춤 분석이에요") }
                    jsonPath("$.result.linkedMeal.foods[0].name") { value("된장찌개") }
                }
        }

        @Test
        @WithCustomUser
        fun `증상 기록이 없으면 SYMPTOM_NOT_FOUND`() {
            whenever(symptomService.getDetail(any(), any()))
                .thenThrow(GeneralException(SymptomErrorCode.SYMPTOM_NOT_FOUND))

            mockMvc.get("/api/v1/symptoms/${SymptomFixture.SYMPTOM_EXTERNAL_ID}")
                .andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("SYMPTOM404_1") }
                }
        }
    }

    @Nested
    inner class `PUT PATCH DELETE symptoms` {

        @Test
        @WithCustomUser
        fun `증상 기록을 수정하면 result 없이 성공을 반환한다`() {
            mockMvc.put("/api/v1/symptoms/${SymptomFixture.SYMPTOM_EXTERNAL_ID}") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "uncomfortable",
                      "symptomTypes": ["acid_reflux"],
                      "occurredAt": "2026-05-12T19:30:00+09:00",
                      "mealRecordId": "${MealRecordFixture.MEAL_RECORD_EXTERNAL_ID}",
                      "memo": "신물이 올라왔어요"
                    }
                """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
                jsonPath("$.result") { value(nullValue()) }
            }
        }

        @Test
        @WithCustomUser
        fun `메모를 수정하면 result 없이 성공을 반환한다`() {
            mockMvc.patch("/api/v1/symptoms/${SymptomFixture.SYMPTOM_EXTERNAL_ID}/memo") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"memo":"조금 답답했어요"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.result") { value(nullValue()) }
            }
        }

        @Test
        @WithCustomUser
        fun `메모가 200자를 초과하면 COMMON400_1`() {
            mockMvc.patch("/api/v1/symptoms/${SymptomFixture.SYMPTOM_EXTERNAL_ID}/memo") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"memo":"${"가".repeat(201)}"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("COMMON400_1") }
                jsonPath("$.result.memo") { value("메모는 200자 이하로 입력해 주세요.") }
            }

            verify(symptomService, never()).updateMemo(any(), any(), any())
        }

        @Test
        @WithCustomUser
        fun `증상 기록을 삭제하면 result 없이 성공을 반환한다`() {
            mockMvc.delete("/api/v1/symptoms/${SymptomFixture.SYMPTOM_EXTERNAL_ID}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result") { value(nullValue()) }
                }
        }
    }

    private fun symptomResponse() = SymptomResponseDTO(
        symptomId = SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(),
        symptomState = SymptomState.COMFORTABLE,
        stateTitle = "comfortable",
        symptomTypes = listOf(SymptomType.ACID_REFLUX),
        occurredAt = "2026-05-12T19:30+09:00",
        linkedMeal = SymptomResponseDTO.LinkedMealDTO(
            mealRecordId = MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
            foods = listOf(
                SymptomResponseDTO.LinkedFoodDTO(
                    mealFoodId = MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString(),
                    name = "된장찌개",
                    category = "soup_stew",
                ),
            ),
        ),
        analysis = SymptomResponseDTO.AnalysisDTO(
            title = "유진 님을 위한 맞춤 분석이에요",
            items = listOf(
                SymptomResponseDTO.Item(
                    emphasis = "편안한 식사 패턴이에요",
                    body = "저녁 식사량 조절을 이어가 보세요.",
                ),
            ),
        ),
    )
}
