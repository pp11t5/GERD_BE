package com.gerd.domain.meal.service

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.dto.MealAnalysisSnapshotDTO
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.service.FoodCategoryReader
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.global.fixture.FoodFixture
import com.gerd.global.fixture.MealRecordFixture
import com.gerd.global.fixture.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MealRecordConverterTest {

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var foodCategoryReader: FoodCategoryReader

    private val objectMapper = ObjectMapper()

    private val converter by lazy {
        MealRecordConverter(foodRepository, foodCategoryReader, objectMapper)
    }

    @Nested
    inner class `음식 기록 상세 변환` {

        @Test
        fun `food 안의 mealRecordExternalId는 개별 음식 ID가 아니라 부모 끼니 ID를 반환한다`() {
            val mealFood = MealRecordFixture.mealFood()
            val food = FoodFixture.food(id = mealFood.foodId)
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(mealFood.foodId)))
                .thenReturn(mapOf(mealFood.foodId to "soup_stew"))

            val result = converter.toSummary(mealFood, food)

            assertThat(result.mealFoodId).isEqualTo(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString())
            assertThat(result.food.mealRecordExternalId).isEqualTo(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString())
        }

        @Test
        fun `저장된 분석 스냅샷은 신호등과 트리거 알레르기 분석으로 복원한다`() {
            val mealFood = MealRecordFixture.mealFood(
                analysisJson = objectMapper.writeValueAsString(mealAnalysis()),
            )
            val food = FoodFixture.food(id = mealFood.foodId)
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(mealFood.foodId)))
                .thenReturn(mapOf(mealFood.foodId to "soup_stew"))

            val result = converter.toSummary(mealFood, food)

            assertThat(result.analysis?.judgmentGrade).isEqualTo(JudgmentGrade.CAUTION)
            assertThat(result.analysis?.triggerAnalysis?.ment).isEqualTo("맵고 짤 수 있어요")
            assertThat(result.analysis?.triggerAnalysis?.content).isEqualTo("천천히 드세요")
            assertThat(result.analysis?.allergyAnalysis?.ment).isEqualTo("알레르기 성분을 확인해 주세요")
            assertThat(result.analysis?.allergyAnalysis?.content).isEqualTo("성분표 확인이 필요해요")
        }
    }

    @Nested
    inner class `끼니 상세 변환` {

        @Test
        fun `증상 기록은 내부 PK가 아니라 externalId UUID를 반환한다`() {
            val mealFood = MealRecordFixture.mealFood()
            val food = FoodFixture.food(id = mealFood.foodId)
            val symptomExternalId = UUID.fromString("9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
            val symptom = Symptom(
                user = UserFixture.user(),
                symptomState = SymptomState.UNCOMFORTABLE,
                symptomTypes = setOf(SymptomType.ACID_REFLUX),
                occurredAt = LocalDateTime.of(2026, 6, 11, 14, 0),
                mealRecordId = MealRecordFixture.MEAL_RECORD_ID,
            ).apply {
                ReflectionTestUtils.setField(this, "id", 99L)
                externalId = symptomExternalId
            }
            whenever(foodRepository.findAllByIdsIncludingDeleted(listOf(mealFood.foodId))).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(mealFood.foodId)))
                .thenReturn(mapOf(mealFood.foodId to "soup_stew"))

            val result = converter.toGroupDetail(MealRecordFixture.mealRecord(), listOf(mealFood), listOf(symptom))

            assertThat(result.meals).hasSize(1)
            assertThat(result.meals[0].mealFoodId).isEqualTo(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString())
            assertThat(result.meals[0].name).isEqualTo("된장찌개")
            assertThat(result.meals[0].category).isEqualTo("soup_stew")
            assertThat(result.stateRecords?.stateRecordId).isEqualTo(symptomExternalId.toString())
            assertThat(result.stateRecords?.label).isEqualTo(SymptomState.UNCOMFORTABLE)
            assertThat(result.stateRecords?.date).isEqualTo("2026-06-11")
            assertThat(result.stateRecords?.timingMinutes).isEqualTo(90)
        }
    }

    private fun mealAnalysis() = MealAnalysisSnapshotDTO(
        judgmentGrade = JudgmentGrade.CAUTION,
        triggerAnalysis = MealAnalysisSnapshotDTO.AnalysisItemDTO(
            ment = "맵고 짤 수 있어요",
            content = "천천히 드세요",
        ),
        allergyAnalysis = MealAnalysisSnapshotDTO.AnalysisItemDTO(
            ment = "알레르기 성분을 확인해 주세요",
            content = "성분표 확인이 필요해요",
        ),
    )
}
