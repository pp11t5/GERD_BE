package com.gerd.domain.dictionary.service

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.MealRecordFixture
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
class DictionaryCommandServiceTest {

    @Mock private lateinit var dictionaryRepository: UserFoodDictionaryRepository
    @Mock private lateinit var mealFoodRepository: MealFoodRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var symptomRepository: SymptomRepository

    private val service by lazy {
        DictionaryCommandService(
            dictionaryRepository = dictionaryRepository,
            mealFoodRepository = mealFoodRepository,
            userRepository = userRepository,
            symptomRepository = symptomRepository,
        )
    }

    private val userId = 1L
    private val mealRecordId = MealRecordFixture.MEAL_RECORD_ID

    @Nested
    inner class `안전 음식 적재` {

        @Test
        fun `끼니의 음식을 원자적 upsert로 SAFE 등록한다`() {
            whenever(mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)).thenReturn(listOf(1L))
            whenever(userRepository.existsById(userId)).thenReturn(true)

            service.upsertSafeEntries(userId, mealRecordId)

            verify(dictionaryRepository).insertIfAbsent(userId, 1L, "SAFE")
        }

        @Test
        fun `이미 SAFE로 존재해도 예외 없이 완료된다`() {
            whenever(mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)).thenReturn(listOf(1L))
            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(dictionaryRepository.insertIfAbsent(userId, 1L, "SAFE")).thenReturn(0)

            service.upsertSafeEntries(userId, mealRecordId)

            verify(dictionaryRepository).insertIfAbsent(userId, 1L, "SAFE")
        }

        @Test
        fun `끼니에 음식이 없으면 아무것도 저장하지 않는다`() {
            whenever(mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)).thenReturn(emptyList())

            service.upsertSafeEntries(userId, mealRecordId)

            verify(dictionaryRepository, never()).insertIfAbsent(any(), any(), any())
        }

        @Test
        fun `유저가 존재하지 않으면 USER_NOT_FOUND`() {
            whenever(mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)).thenReturn(listOf(1L))
            whenever(userRepository.existsById(userId)).thenReturn(false)

            assertThatThrownBy { service.upsertSafeEntries(userId, mealRecordId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)

            verify(dictionaryRepository, never()).insertIfAbsent(any(), any(), any())
        }
    }

    @Nested
    inner class `안전 음식 제거` {

        @Test
        fun `다른 편안 증상에서 유효하지 않은 음식만 삭제한다`() {
            whenever(mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)).thenReturn(listOf(1L))
            whenever(symptomRepository.findFoodIdsStillSafeByOtherSymptoms(userId, listOf(1L), mealRecordId))
                .thenReturn(emptyList())

            service.removeSafeEntries(userId, mealRecordId)

            verify(dictionaryRepository).deleteByUserIdAndFoodIdsAndType(userId, listOf(1L), DictionaryType.SAFE)
        }

        @Test
        fun `다른 편안 증상에서 유효한 음식은 삭제하지 않는다`() {
            whenever(mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)).thenReturn(listOf(1L))
            whenever(symptomRepository.findFoodIdsStillSafeByOtherSymptoms(userId, listOf(1L), mealRecordId))
                .thenReturn(listOf(1L))

            service.removeSafeEntries(userId, mealRecordId)

            verify(dictionaryRepository, never()).deleteByUserIdAndFoodIdsAndType(any(), any(), any())
        }

        @Test
        fun `끼니에 음식이 없으면 삭제 쿼리를 호출하지 않는다`() {
            whenever(mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)).thenReturn(emptyList())

            service.removeSafeEntries(userId, mealRecordId)

            verify(dictionaryRepository, never()).deleteByUserIdAndFoodIdsAndType(any(), any(), any())
        }

        @Test
        fun `일부 음식만 다른 증상에서 유효하면 나머지만 삭제한다`() {
            whenever(mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)).thenReturn(listOf(1L, 2L))
            whenever(symptomRepository.findFoodIdsStillSafeByOtherSymptoms(userId, listOf(1L, 2L), mealRecordId))
                .thenReturn(listOf(1L))

            service.removeSafeEntries(userId, mealRecordId)

            verify(dictionaryRepository).deleteByUserIdAndFoodIdsAndType(userId, listOf(2L), DictionaryType.SAFE)
        }
    }

    @Nested
    inner class `주의·위험 음식 적재` {

        @Test
        fun `CAUTION 판정이면 CAUTION 타입으로 upsert한다`() {
            whenever(userRepository.existsById(userId)).thenReturn(true)

            service.upsertCautionRiskEntry(userId, 1L, JudgmentGrade.CAUTION)

            verify(dictionaryRepository).insertIfAbsent(userId, 1L, "CAUTION")
        }

        @Test
        fun `RISK 판정이면 RISK 타입으로 upsert한다`() {
            whenever(userRepository.existsById(userId)).thenReturn(true)

            service.upsertCautionRiskEntry(userId, 1L, JudgmentGrade.RISK)

            verify(dictionaryRepository).insertIfAbsent(userId, 1L, "RISK")
        }

        @Test
        fun `RECOMMEND 판정이면 저장하지 않는다`() {
            service.upsertCautionRiskEntry(userId, 1L, JudgmentGrade.RECOMMEND)

            verify(dictionaryRepository, never()).insertIfAbsent(any(), any(), any())
        }

        @Test
        fun `이미 같은 타입으로 존재해도 예외 없이 완료된다`() {
            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(dictionaryRepository.insertIfAbsent(userId, 1L, "CAUTION")).thenReturn(0)

            service.upsertCautionRiskEntry(userId, 1L, JudgmentGrade.CAUTION)

            verify(dictionaryRepository).insertIfAbsent(userId, 1L, "CAUTION")
        }

        @Test
        fun `유저가 존재하지 않으면 USER_NOT_FOUND`() {
            whenever(userRepository.existsById(userId)).thenReturn(false)

            assertThatThrownBy { service.upsertCautionRiskEntry(userId, 1L, JudgmentGrade.CAUTION) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)

            verify(dictionaryRepository, never()).insertIfAbsent(any(), any(), any())
        }
    }
}
