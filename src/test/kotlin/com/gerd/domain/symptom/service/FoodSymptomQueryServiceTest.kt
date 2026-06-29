package com.gerd.domain.symptom.service

import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.FoodFixture
import com.gerd.global.fixture.MealRecordFixture
import com.gerd.global.fixture.SymptomFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class FoodSymptomQueryServiceTest {

    @Mock private lateinit var foodRepository: FoodRepository
    @Mock private lateinit var symptomRepository: SymptomRepository
    @Mock private lateinit var mealRecordRepository: MealRecordRepository

    private val service by lazy {
        FoodSymptomQueryService(foodRepository, symptomRepository, mealRecordRepository)
    }

    private val userId = 1L
    private val foodExternalId = FoodFixture.EXTERNAL_ID.toString()

    @Nested
    inner class `음식별 연결 증상 조회` {

        @Test
        fun `음식을 포함한 식사에 연결된 증상 목록을 반환한다`() {
            val food = FoodFixture.food(id = 1L)
            val mealRecord = MealRecordFixture.mealRecord(
                eatenAt = LocalDateTime.of(2026, 6, 17, 12, 0, 0),
            )
            val symptom = SymptomFixture.symptom(
                mealRecordId = MealRecordFixture.MEAL_RECORD_ID,
                occurredAt = LocalDateTime.of(2026, 6, 17, 13, 10, 0),
                symptomState = SymptomState.UNCOMFORTABLE,
                symptomTypes = setOf(SymptomType.ACID_REFLUX, SymptomType.CHEST_TIGHTNESS),
            )

            whenever(foodRepository.findByExternalId(FoodFixture.EXTERNAL_ID)).thenReturn(food)
            whenever(symptomRepository.findLinkedSymptomsByUserIdAndFoodId(userId, 1L)).thenReturn(listOf(symptom))
            whenever(mealRecordRepository.findAllById(listOf(MealRecordFixture.MEAL_RECORD_ID))).thenReturn(listOf(mealRecord))

            val result = service.getSymptoms(foodExternalId, userId)

            assertThat(result).hasSize(1)
            assertThat(result.first().symptomId).isEqualTo(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString())
            assertThat(result.first().symptomState).isEqualTo(SymptomState.UNCOMFORTABLE)
            assertThat(result.first().symptomTypes).containsExactlyInAnyOrder(SymptomType.ACID_REFLUX, SymptomType.CHEST_TIGHTNESS)
            assertThat(result.first().mealRecordId).isEqualTo(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString())
            assertThat(result.first().afterMealMinutes).isEqualTo(70)
        }

        @Test
        fun `연결 증상이 없으면 빈 리스트를 반환한다`() {
            whenever(foodRepository.findByExternalId(FoodFixture.EXTERNAL_ID)).thenReturn(FoodFixture.food(id = 1L))
            whenever(symptomRepository.findLinkedSymptomsByUserIdAndFoodId(userId, 1L)).thenReturn(emptyList())

            val result = service.getSymptoms(foodExternalId, userId)

            assertThat(result).isEmpty()
            verify(mealRecordRepository, never()).findAllById(any<Iterable<Long>>())
        }

        @Test
        fun `음식 UUID가 잘못되면 FOOD_NOT_FOUND 예외를 던진다`() {
            assertThatThrownBy { service.getSymptoms("not-uuid", userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
        }
    }
}
