package com.gerd.domain.symptom.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import com.gerd.domain.food.repository.FoodCategoryView
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.symptom.dto.SymptomCreateRequestDTO
import com.gerd.domain.symptom.dto.SymptomMemoUpdateRequestDTO
import com.gerd.domain.symptom.dto.SymptomResponseDTO
import com.gerd.domain.symptom.dto.SymptomUpdateRequestDTO
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.domain.symptom.exception.SymptomErrorCode
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.apiPayload.code.CommonErrorCode
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SymptomServiceTest {

    @Mock
    private lateinit var symptomRepository: SymptomRepository

    @Mock
    private lateinit var mealRecordRepository: MealRecordRepository

    @Mock
    private lateinit var mealFoodRepository: MealFoodRepository

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var foodCategoryMapRepository: FoodCategoryMapRepository

    @Mock
    private lateinit var symptomConverter: SymptomConverter

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var symptomPatternRefreshService: SymptomPatternRefreshService

    private val service by lazy {
        SymptomService(
            symptomRepository = symptomRepository,
            mealRecordRepository = mealRecordRepository,
            mealFoodRepository = mealFoodRepository,
            foodRepository = foodRepository,
            foodCategoryMapRepository = foodCategoryMapRepository,
            symptomConverter = symptomConverter,
            userRepository = userRepository,
            symptomPatternRefreshService = symptomPatternRefreshService,
        )
    }

    private val userId = 1L

    @Nested
    inner class `생성` {

        @Test
        fun `증상 기록을 저장하고 연결 음식 정보를 포함해 응답한다`() {
            val request = createRequest()
            val saved = SymptomFixture.symptom()
            whenever(userRepository.getReferenceById(userId)).thenReturn(SymptomFixture.user())
            whenever(mealRecordRepository.findByExternalIdAndUser_Id(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID, userId))
                .thenReturn(MealRecordFixture.mealRecord())
            whenever(symptomConverter.parseOccurredAt("2026-05-12T19:30:00+09:00")).thenReturn(SymptomFixture.OCCURRED_AT)
            whenever(symptomRepository.save(any())).thenReturn(saved)
            stubLinkedMeal()
            whenever(symptomConverter.toResponse(any(), any())).thenReturn(symptomResponse())

            val result = service.create(userId, request)

            val symptomCaptor = argumentCaptor<Symptom>()
            val linkedMealCaptor = argumentCaptor<SymptomResponseDTO.LinkedMealDTO>()
            verify(symptomRepository).save(symptomCaptor.capture())
            verify(symptomConverter).toResponse(any(), linkedMealCaptor.capture())
            verify(symptomPatternRefreshService).refreshAsync(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), userId)
            assertThat(symptomCaptor.firstValue.user.id).isEqualTo(userId)
            assertThat(symptomCaptor.firstValue.mealRecordId).isEqualTo(MealRecordFixture.MEAL_RECORD_ID)
            assertThat(symptomCaptor.firstValue.symptomState).isEqualTo(SymptomState.COMFORTABLE)
            assertThat(linkedMealCaptor.firstValue.mealRecordId).isEqualTo(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString())
            assertThat(linkedMealCaptor.firstValue.foods[0].mealFoodId).isEqualTo(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString())
            assertThat(linkedMealCaptor.firstValue.foods[0].category).isEqualTo("soup_stew")
            assertThat(result.symptomId).isEqualTo(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString())
        }

        @Test
        fun `끼니 식별자가 잘못된 형식이면 MEAL_RECORD_NOT_FOUND`() {
            val request = createRequest(mealRecordId = "bad")
            whenever(userRepository.getReferenceById(userId)).thenReturn(SymptomFixture.user())

            assertThatThrownBy { service.create(userId, request) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.MEAL_RECORD_NOT_FOUND)
            verify(symptomRepository, never()).save(any())
        }
    }

    @Nested
    inner class `상세 조회` {

        @Test
        fun `본인 증상 기록을 UUID로 조회하고 dirty면 비동기 갱신을 예약한다`() {
            val symptom = SymptomFixture.symptom(isAnalysisDirty = true)
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId)).thenReturn(symptom)
            stubLinkedMeal()
            whenever(symptomConverter.toResponse(any(), any())).thenReturn(symptomResponse())

            val result = service.getDetail(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), userId)

            verify(symptomPatternRefreshService).refreshAsync(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), userId)
            assertThat(result.symptomId).isEqualTo(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString())
        }

        @Test
        fun `증상 기록이 없으면 SYMPTOM_NOT_FOUND`() {
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId)).thenReturn(null)

            assertThatThrownBy { service.getDetail(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(SymptomErrorCode.SYMPTOM_NOT_FOUND)
            verify(mealRecordRepository, never()).findByIdAndUser_Id(any(), any())
        }
    }

    @Nested
    inner class `수정` {

        @Test
        fun `전체 수정하면 dirty 상태로 바꾸고 비동기 갱신을 예약한다`() {
            val symptom = SymptomFixture.symptom(isAnalysisDirty = false, analysisVersion = 1L)
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId)).thenReturn(symptom)
            whenever(mealRecordRepository.findByExternalIdAndUser_Id(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID, userId))
                .thenReturn(MealRecordFixture.mealRecord())
            whenever(symptomConverter.parseOccurredAt("2026-05-12T19:30:00+09:00")).thenReturn(SymptomFixture.OCCURRED_AT)

            service.update(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), updateRequest(), userId)

            assertThat(symptom.symptomState).isEqualTo(SymptomState.UNCOMFORTABLE)
            assertThat(symptom.symptomTypes).containsExactly(SymptomType.ACID_REFLUX)
            assertThat(symptom.isAnalysisDirty).isTrue()
            assertThat(symptom.analysisVersion).isEqualTo(2L)
            verify(symptomPatternRefreshService).refreshAsync(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), userId)
        }

        @Test
        fun `메모만 수정해도 dirty 상태로 바꾸고 비동기 갱신을 예약한다`() {
            val symptom = SymptomFixture.symptom(isAnalysisDirty = false, analysisVersion = 1L)
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId)).thenReturn(symptom)

            service.updateMemo(
                SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(),
                SymptomMemoUpdateRequestDTO(memo = "  조금 답답했어요  "),
                userId,
            )

            assertThat(symptom.memo).isEqualTo("조금 답답했어요")
            assertThat(symptom.isAnalysisDirty).isTrue()
            assertThat(symptom.analysisVersion).isEqualTo(2L)
            verify(symptomPatternRefreshService).refreshAsync(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), userId)
        }

        @Test
        fun `전체 수정에서 증상 상태가 없으면 INVALID_REQUEST`() {
            val symptom = SymptomFixture.symptom(isAnalysisDirty = false)
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId)).thenReturn(symptom)
            whenever(mealRecordRepository.findByExternalIdAndUser_Id(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID, userId))
                .thenReturn(MealRecordFixture.mealRecord())

            assertThatThrownBy {
                service.update(
                    SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(),
                    updateRequest(symptomState = null),
                    userId,
                )
            }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(CommonErrorCode.INVALID_REQUEST)

            verify(symptomPatternRefreshService, never()).refreshAsync(any(), any())
        }
    }

    @Nested
    inner class `삭제` {

        @Test
        fun `본인 증상 기록을 삭제한다`() {
            val symptom = SymptomFixture.symptom()
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId)).thenReturn(symptom)

            service.delete(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), userId)

            verify(symptomRepository).delete(symptom)
        }
    }

    private fun stubLinkedMeal() {
        whenever(mealRecordRepository.findByIdAndUser_Id(MealRecordFixture.MEAL_RECORD_ID, userId))
            .thenReturn(MealRecordFixture.mealRecord())
        whenever(mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(MealRecordFixture.MEAL_RECORD_ID))
            .thenReturn(listOf(MealRecordFixture.mealFood()))
        whenever(foodRepository.findAllByIdsIncludingDeleted(listOf(1L)))
            .thenReturn(listOf(FoodFixture.food(id = 1L)))
        whenever(foodCategoryMapRepository.findCategoryViewsByFoodIds(setOf(1L)))
            .thenReturn(listOf(categoryView(1L, "soup_stew")))
    }

    private fun categoryView(foodIdValue: Long, codeValue: String): FoodCategoryView =
        object : FoodCategoryView {
            override val foodId: Long = foodIdValue
            override val code: String = codeValue
        }

    private fun createRequest(
        mealRecordId: String = MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
    ) = SymptomCreateRequestDTO(
        symptomState = SymptomState.COMFORTABLE,
        symptomTypes = emptySet(),
        occurredAt = "2026-05-12T19:30:00+09:00",
        mealRecordId = mealRecordId,
        memo = "속이 편했어요",
    )

    private fun updateRequest(
        symptomState: SymptomState? = SymptomState.UNCOMFORTABLE,
    ) = SymptomUpdateRequestDTO(
        symptomState = symptomState,
        symptomTypes = setOf(SymptomType.ACID_REFLUX),
        occurredAt = "2026-05-12T19:30:00+09:00",
        mealRecordId = MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
        memo = "신물이 올라왔어요",
    )

    private fun symptomResponse() = SymptomResponseDTO(
        symptomId = SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(),
        symptomState = SymptomState.COMFORTABLE,
        stateTitle = "comfortable",
        symptomTypes = emptyList(),
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
        analysis = null,
    )
}
