package com.gerd.domain.meal.service

import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.MealRecordFixture
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

@ExtendWith(MockitoExtension::class)
class MealRecordQueryServiceTest {

    @Mock
    private lateinit var mealFoodRepository: MealFoodRepository

    @Mock
    private lateinit var mealRecordRepository: MealRecordRepository

    @Mock
    private lateinit var symptomRepository: SymptomRepository

    @Mock
    private lateinit var mealRecordConverter: MealRecordConverter

    private val service by lazy {
        MealQueryService(mealFoodRepository, mealRecordRepository, symptomRepository, mealRecordConverter)
    }

    private val userId = 1L

    @Nested
    inner class `음식 기록 상세 조회` {

        @Test
        fun `형식이 잘못된 mealFoodId면 MEAL_FOOD_NOT_FOUND`() {
            whenever(mealRecordConverter.parseUuid("bad")).thenReturn(null)

            assertThatThrownBy { service.getDetail("bad", userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.MEAL_FOOD_NOT_FOUND)
        }
    }

    @Nested
    inner class `끼니 상세 조회` {

        @Test
        fun `본인 끼니면 증상과 함께 변환한다`() {
            val mealRecord = MealRecordFixture.mealRecord()
            val detail = com.gerd.domain.meal.dto.MealRecordDetailDTO(
                mealId = MealRecordFixture.MEAL_RECORD_ID.toString(),
                mealGroupId = MealRecordFixture.MEAL_RECORD_ID.toString(),
                eatenAt = "2026-06-11T12:30:00+09:00",
                memo = null,
                stateRecords = null,
            )
            whenever(mealRecordConverter.parseUuid(MealRecordFixture.MEAL_RECORD_ID.toString())).thenReturn(MealRecordFixture.MEAL_RECORD_ID)
            whenever(mealRecordRepository.findByIdAndUserId(MealRecordFixture.MEAL_RECORD_ID, userId)).thenReturn(mealRecord)
            whenever(symptomRepository.findByMealRecordId(MealRecordFixture.MEAL_RECORD_ID)).thenReturn(emptyList())
            whenever(mealRecordConverter.toGroupDetail(mealRecord, emptyList())).thenReturn(detail)

            val result = service.getGroupDetail(MealRecordFixture.MEAL_RECORD_ID.toString(), userId)

            assertThat(result.mealId).isEqualTo(MealRecordFixture.MEAL_RECORD_ID.toString())
            verify(symptomRepository).findByMealRecordId(MealRecordFixture.MEAL_RECORD_ID)
        }
    }

    @Nested
    inner class `후보 조회` {

        @Test
        fun `최근 끼니가 없으면 빈 배열을 반환하고 추가 조회하지 않는다`() {
            whenever(mealRecordRepository.findByUserIdAndEatenAtAfter(any(), any())).thenReturn(emptyList())

            val result = service.getCandidates(userId)

            assertThat(result).isEmpty()
            verify(symptomRepository, never()).findLinkedMealRecordIdsByUserId(any())
            verify(mealFoodRepository, never()).findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())
        }

        @Test
        fun `증상에 연결되지 않은 끼니만 후보로 변환한다`() {
            val linked = MealRecordFixture.mealRecord(id = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"))
            val unlinked = MealRecordFixture.mealRecord()
            val foods = listOf(MealRecordFixture.mealFood())
            whenever(mealRecordRepository.findByUserIdAndEatenAtAfter(any(), any())).thenReturn(listOf(linked, unlinked))
            whenever(symptomRepository.findLinkedMealRecordIdsByUserId(userId)).thenReturn(listOf(linked.id))
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(listOf(unlinked.id)))
                .thenReturn(foods)
            whenever(mealRecordConverter.toCandidates(listOf(unlinked), foods)).thenReturn(emptyList())

            service.getCandidates(userId)

            verify(mealFoodRepository).findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(listOf(unlinked.id))
        }
    }
}
