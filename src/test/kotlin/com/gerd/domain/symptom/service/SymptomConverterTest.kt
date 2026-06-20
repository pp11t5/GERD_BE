package com.gerd.domain.symptom.service

import com.gerd.domain.symptom.dto.SymptomResponseDTO
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.global.fixture.MealRecordFixture
import com.gerd.global.fixture.SymptomFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

class SymptomConverterTest {

    private val converter = SymptomConverter(ObjectMapper())

    @Nested
    inner class `상세 응답 변환` {

        @Test
        fun `증상 외부 식별자와 연결 음식 목록을 반환한다`() {
            val linkedMeal = linkedMeal()

            val result = converter.toResponse(SymptomFixture.symptom(), linkedMeal)

            assertThat(result.symptomId).isEqualTo(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString())
            assertThat(result.symptomState).isEqualTo(SymptomState.COMFORTABLE)
            assertThat(result.stateTitle).isEqualTo("comfortable")
            assertThat(result.occurredAt).isEqualTo("2026-05-12T19:30+09:00")
            assertThat(result.linkedMeal.mealRecordId).isEqualTo(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString())
            assertThat(result.linkedMeal.foods[0].mealFoodId).isEqualTo(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString())
            assertThat(result.linkedMeal.foods[0].name).isEqualTo("된장찌개")
        }

        @Test
        fun `분석이 있으면 사용자 닉네임 기반 제목으로 반환한다`() {
            val symptom = SymptomFixture.symptom(
                analysisJson = """
                    {"label":"유지 권장","pattern":"편안한 식사 패턴이에요","advice":"저녁 식사량 조절을 이어가 보세요."}
                """.trimIndent(),
            )

            val result = converter.toResponse(symptom, linkedMeal())

            assertThat(result.analysis?.title).isEqualTo("유진 님을 위한 맞춤 분석이에요")
            assertThat(result.analysis?.items?.first()?.emphasis).isEqualTo("편안한 식사 패턴이에요")
            assertThat(result.analysis?.items?.first()?.body).isEqualTo("저녁 식사량 조절을 이어가 보세요.")
        }
    }

    private fun linkedMeal() = SymptomResponseDTO.LinkedMealDTO(
        mealRecordId = MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
        foods = listOf(
            SymptomResponseDTO.LinkedFoodDTO(
                mealFoodId = MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString(),
                name = "된장찌개",
                category = "soup_stew",
            ),
        ),
    )
}
