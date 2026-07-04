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
import org.mockito.kotlin.eq
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
                mealRecordId = MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
                eatenAt = "2026-06-11T12:30:00+09:00",
                meals = listOf(
                    com.gerd.domain.meal.dto.MealRecordDetailDTO.MealFoodDetailDTO(
                        mealFoodId = MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString(),
                        name = "된장찌개",
                        category = "soup_stew",
                        eatenAt = "2026-06-11T12:30:00+09:00",
                    ),
                ),
                stateRecords = null,
            )
            val foods = listOf(MealRecordFixture.mealFood())
            whenever(mealRecordConverter.parseUuid(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString()))
                .thenReturn(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID)
            whenever(mealRecordRepository.findByExternalIdAndUser_Id(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID, userId))
                .thenReturn(mealRecord)
            whenever(mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(MealRecordFixture.MEAL_RECORD_ID)).thenReturn(foods)
            whenever(symptomRepository.findByMealRecordId(MealRecordFixture.MEAL_RECORD_ID)).thenReturn(emptyList())
            whenever(mealRecordConverter.toGroupDetail(mealRecord, foods, emptyList())).thenReturn(detail)

            val result = service.getGroupDetail(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(), userId)

            assertThat(result.mealRecordId).isEqualTo(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString())
            assertThat(result.meals).hasSize(1)
            verify(mealFoodRepository).findByMealRecordIdOrderByEatenAtAsc(MealRecordFixture.MEAL_RECORD_ID)
            verify(symptomRepository).findByMealRecordId(MealRecordFixture.MEAL_RECORD_ID)
        }
    }

    @Nested
    inner class `후보 조회` {

        @Test
        fun `최근 끼니가 없으면 빈 배열을 반환하고 추가 조회하지 않는다`() {
            whenever(mealRecordRepository.findByUser_IdAndEatenAtAfter(any(), any())).thenReturn(emptyList())

            val result = service.getCandidates(userId)

            assertThat(result).isEmpty()
            verify(symptomRepository, never()).findLinkedMealRecordIdsByUserId(any())
            verify(mealFoodRepository, never()).findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())
        }

        @Test
        fun `증상에 연결되지 않은 끼니만 후보로 변환한다`() {
            val linked = MealRecordFixture.mealRecord(id = 11L)
            val unlinked = MealRecordFixture.mealRecord()
            val foods = listOf(MealRecordFixture.mealFood())
            whenever(mealRecordRepository.findByUser_IdAndEatenAtAfter(any(), any())).thenReturn(listOf(linked, unlinked))
            whenever(symptomRepository.findLinkedMealRecordIdsByUserId(userId)).thenReturn(listOf(linked.id!!))
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(listOf(unlinked.id!!)))
                .thenReturn(foods)
            whenever(mealRecordConverter.toCandidates(listOf(unlinked), foods)).thenReturn(emptyList())

            service.getCandidates(userId)

            verify(mealFoodRepository).findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(listOf(unlinked.id!!))
        }
    }

    @Nested
    inner class `미기록 식사 개수 조회` {

        @Test
        fun `증상 미연결 끼니 개수를 그대로 반환한다`() {
            whenever(mealRecordRepository.countUnlinkedByUser_IdAndEatenAtAfter(eq(userId), any())).thenReturn(3L)

            val result = service.getUnRecordedSymptomCount(userId)

            assertThat(result.count).isEqualTo(3)
        }
    }

    @Nested
    inner class `최근 음식별 요약 조회` {

        @Test
        fun `최근 음식이 없으면 빈 배열을 반환하고 추가 조회하지 않는다`() {
            whenever(mealFoodRepository.findByUser_IdAndEatenAtAfterOrderByEatenAtDesc(eq(userId), any()))
                .thenReturn(emptyList())

            val result = service.getRecentFoodSummaries(userId)

            assertThat(result).isEmpty()
            verify(symptomRepository, never()).findByMealRecordIdIn(any())
            verify(mealRecordConverter, never()).toFoodSummaries(any(), any())
        }

        @Test
        fun `음식이 속한 끼니 ID로 증상을 조회해 변환에 넘긴다`() {
            val mealFood = MealRecordFixture.mealFood()
            val symptoms = emptyList<com.gerd.domain.symptom.entity.Symptom>()
            whenever(mealFoodRepository.findByUser_IdAndEatenAtAfterOrderByEatenAtDesc(eq(userId), any()))
                .thenReturn(listOf(mealFood))
            whenever(symptomRepository.findByMealRecordIdIn(listOf(MealRecordFixture.MEAL_RECORD_ID)))
                .thenReturn(symptoms)
            whenever(mealRecordConverter.toFoodSummaries(listOf(mealFood), symptoms)).thenReturn(emptyList())

            service.getRecentFoodSummaries(userId)

            verify(symptomRepository).findByMealRecordIdIn(listOf(MealRecordFixture.MEAL_RECORD_ID))
            verify(mealRecordConverter).toFoodSummaries(listOf(mealFood), symptoms)
        }
    }
}
