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

    @Nested
    inner class `최근 음식별 요약 변환` {

        @Test
        fun `증상이 연결된 끼니 음식은 상태를, 미연결 끼니 음식은 null을 반환한다`() {
            val user = UserFixture.user()
            val recordWithSymptom = MealRecordFixture.mealRecord(id = 10L, user = user)
            val recordWithoutSymptom = MealRecordFixture.mealRecord(id = 20L, user = user)
            val foodWithSymptom = MealRecordFixture.mealFood(
                id = 1L, user = user, foodId = 1L, mealRecord = recordWithSymptom,
            )
            val foodWithoutSymptom = MealRecordFixture.mealFood(
                id = 2L, user = user, foodId = 2L, mealRecord = recordWithoutSymptom,
                externalId = UUID.fromString("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d"),
            )
            val symptom = symptom(mealRecordId = 10L, state = SymptomState.UNCOMFORTABLE, occurredAt = LocalDateTime.of(2026, 6, 11, 14, 0))

            whenever(foodRepository.findAllByIdsIncludingDeleted(listOf(1L, 2L)))
                .thenReturn(listOf(FoodFixture.food(id = 1L, name = "된장찌개"), FoodFixture.food(id = 2L, name = "김치")))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(1L, 2L)))
                .thenReturn(mapOf(1L to "soup_stew"))

            val result = converter.toFoodSummaries(listOf(foodWithSymptom, foodWithoutSymptom), listOf(symptom))

            assertThat(result).hasSize(2)
            assertThat(result[0].foodName).isEqualTo("된장찌개")
            assertThat(result[0].category).isEqualTo("soup_stew")
            assertThat(result[0].symptomState).isEqualTo(SymptomState.UNCOMFORTABLE)
            // 카테고리 없는 음식은 빈 문자열, 증상 미연결 끼니는 null
            assertThat(result[1].foodName).isEqualTo("김치")
            assertThat(result[1].category).isEqualTo("")
            assertThat(result[1].symptomState).isNull()
        }

        @Test
        fun `한 끼니에 증상이 여러 개면 가장 최근 발생 증상을 대표로 사용한다`() {
            val user = UserFixture.user()
            val record = MealRecordFixture.mealRecord(id = 10L, user = user)
            val food = MealRecordFixture.mealFood(id = 1L, user = user, foodId = 1L, mealRecord = record)
            val older = symptom(mealRecordId = 10L, state = SymptomState.GOOD, occurredAt = LocalDateTime.of(2026, 6, 11, 13, 0))
            val latest = symptom(mealRecordId = 10L, state = SymptomState.SEVERE, occurredAt = LocalDateTime.of(2026, 6, 11, 15, 0))

            whenever(foodRepository.findAllByIdsIncludingDeleted(listOf(1L)))
                .thenReturn(listOf(FoodFixture.food(id = 1L)))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(1L)))
                .thenReturn(mapOf(1L to "soup_stew"))

            val result = converter.toFoodSummaries(listOf(food), listOf(older, latest))

            assertThat(result[0].symptomState).isEqualTo(SymptomState.SEVERE)
        }
    }

    private fun symptom(mealRecordId: Long, state: SymptomState, occurredAt: LocalDateTime): Symptom =
        Symptom(
            user = UserFixture.user(),
            symptomState = state,
            symptomTypes = setOf(SymptomType.ACID_REFLUX),
            occurredAt = occurredAt,
            mealRecordId = mealRecordId,
        )

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
