package com.gerd.domain.meal.service

import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.service.FoodCategoryReader
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.global.fixture.MealRecordFixture
import com.gerd.global.fixture.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
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

    private val converter by lazy {
        MealRecordConverter(foodRepository, foodCategoryReader, ObjectMapper())
    }

    @Nested
    inner class `끼니 상세 변환` {

        @Test
        fun `증상 기록은 내부 PK가 아니라 externalId UUID를 반환한다`() {
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

            val result = converter.toGroupDetail(MealRecordFixture.mealRecord(), listOf(symptom))

            assertThat(result.stateRecords?.stateRecordId).isEqualTo(symptomExternalId.toString())
            assertThat(result.stateRecords?.timingMinutes).isEqualTo(90)
        }
    }
}
