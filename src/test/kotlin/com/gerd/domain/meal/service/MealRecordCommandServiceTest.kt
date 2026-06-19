package com.gerd.domain.meal.service

import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.dto.MealAnalysisSnapshotDTO
import com.gerd.domain.judgment.service.FoodJudgmentQueryService
import com.gerd.domain.meal.dto.MealRecordAppendRequestDTO
import com.gerd.domain.meal.entity.MealFood
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.FoodFixture
import com.gerd.global.fixture.MealRecordFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.SimpleTransactionStatus
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MealRecordCommandServiceTest {

    @Mock
    private lateinit var mealFoodRepository: MealFoodRepository

    @Mock
    private lateinit var mealRecordRepository: MealRecordRepository

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var mealRecordConverter: MealRecordConverter

    @Mock
    private lateinit var foodJudgmentQueryService: FoodJudgmentQueryService

    @Mock
    private lateinit var transactionManager: PlatformTransactionManager

    private lateinit var service: MealCommandService

    private val objectMapper = ObjectMapper()
    private val userId = 1L
    private val foodExternalId = FoodFixture.EXTERNAL_ID

    @BeforeEach
    fun setUp() {
        service = MealCommandService(
            mealFoodRepository = mealFoodRepository,
            mealRecordRepository = mealRecordRepository,
            foodRepository = foodRepository,
            mealRecordConverter = mealRecordConverter,
            foodJudgmentQueryService = foodJudgmentQueryService,
            objectMapper = objectMapper,
            transactionManager = transactionManager,
        )
    }

    @Nested
    inner class `생성` {

        @Test
        fun `판정 스냅샷을 독립 트랜잭션에서 만든 뒤 식사 음식을 저장한다`() {
            val food = FoodFixture.food(id = 10L)
            val mealRecord = MealRecordFixture.mealRecord()
            whenever(transactionManager.getTransaction(any())).thenAnswer { SimpleTransactionStatus() }
            whenever(mealRecordConverter.parseUuid(foodExternalId.toString())).thenReturn(foodExternalId)
            whenever(foodRepository.findByExternalId(foodExternalId)).thenReturn(food)
            whenever(foodJudgmentQueryService.getJudgment(foodExternalId.toString(), userId)).thenReturn(judgment() to true)
            whenever(mealRecordConverter.parseEatenAt("2026-06-11T12:30:00+09:00")).thenReturn(MealRecordFixture.EATEN_AT)
            whenever(mealRecordRepository.save(any())).thenReturn(mealRecord)
            whenever(mealFoodRepository.save(any())).thenAnswer { invocation ->
                (invocation.arguments[0] as MealFood).apply {
                    ReflectionTestUtils.setField(this, "id", 1L)
                    externalId = MealRecordFixture.MEAL_FOOD_EXTERNAL_ID
                }
            }
            whenever(mealRecordConverter.toSummary(any(), any())).thenReturn(mealFoodDetail())

            val result = service.create(
                MealRecordAppendRequestDTO(
                    foodExternalId = foodExternalId.toString(),
                    eatenAt = "2026-06-11T12:30:00+09:00",
                ),
                userId,
            )

            val captor = argumentCaptor<MealFood>()
            verify(mealFoodRepository).save(captor.capture())
            assertThat(captor.firstValue.mealRecordId).isEqualTo(MealRecordFixture.MEAL_RECORD_ID)
            assertThat(captor.firstValue.judgedGrade).isEqualTo(JudgmentGrade.CAUTION)
            val analysis = objectMapper.readValue(captor.firstValue.analysisJson, MealAnalysisSnapshotDTO::class.java)
            assertThat(analysis.judgmentGrade).isEqualTo(JudgmentGrade.CAUTION)
            assertThat(analysis.triggerAnalysis.ment).isEqualTo("맵고 짤 수 있어요")
            assertThat(analysis.triggerAnalysis.content).isEqualTo("천천히 드세요")
            assertThat(analysis.allergyAnalysis.ment).isEqualTo("자극 가능성이 있어요")
            assertThat(analysis.allergyAnalysis.content).isEqualTo("식후 바로 눕지 마세요")
            assertThat(result.mealFoodId).isEqualTo(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString())

            val definitions = argumentCaptor<TransactionDefinition>()
            verify(transactionManager, times(2)).getTransaction(definitions.capture())
            assertThat(definitions.allValues[0].propagationBehavior).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW)
            assertThat(definitions.allValues[0].isReadOnly).isTrue()
        }

        @Test
        fun `형식이 잘못된 음식 UUID면 FOOD_NOT_FOUND`() {
            whenever(mealRecordConverter.parseUuid("bad")).thenReturn(null)

            assertThatThrownBy { service.create(MealRecordAppendRequestDTO(foodExternalId = "bad"), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
            verify(foodRepository, never()).findByExternalId(any())
        }

        @Test
        fun `타인의 비공개 음식이면 FOOD_NOT_FOUND`() {
            val privateFood = FoodFixture.food(
                id = 10L,
                source = FoodSource.USER,
                visibility = FoodVisibility.PRIVATE,
                ownerUserId = 2L,
            )
            whenever(mealRecordConverter.parseUuid(foodExternalId.toString())).thenReturn(foodExternalId)
            whenever(foodRepository.findByExternalId(foodExternalId)).thenReturn(privateFood)

            assertThatThrownBy { service.create(MealRecordAppendRequestDTO(foodExternalId = foodExternalId.toString()), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
            verify(mealFoodRepository, never()).save(any())
        }
    }

    @Nested
    inner class `단일 음식 삭제` {

        @Test
        fun `삭제 후 남은 음식이 없으면 부모 끼니도 삭제한다`() {
            val mealFood = MealRecordFixture.mealFood()
            val mealRecord = MealRecordFixture.mealRecord()
            whenever(mealRecordConverter.parseUuid(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()))
                .thenReturn(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID)
            whenever(mealFoodRepository.findByExternalIdAndUserId(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID, userId)).thenReturn(mealFood)
            whenever(mealFoodRepository.countByMealRecordId(MealRecordFixture.MEAL_RECORD_ID)).thenReturn(0)
            whenever(mealRecordRepository.findByIdAndUserId(MealRecordFixture.MEAL_RECORD_ID, userId)).thenReturn(mealRecord)

            service.delete(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString(), userId)

            inOrder(mealFoodRepository, mealRecordRepository) {
                verify(mealFoodRepository).delete(mealFood)
                verify(mealFoodRepository).flush()
                verify(mealFoodRepository).countByMealRecordId(MealRecordFixture.MEAL_RECORD_ID)
                verify(mealRecordRepository).delete(mealRecord)
            }
        }

        @Test
        fun `삭제 후 다른 음식이 남아 있으면 부모 끼니는 유지한다`() {
            val mealFood = MealRecordFixture.mealFood()
            whenever(mealRecordConverter.parseUuid(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()))
                .thenReturn(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID)
            whenever(mealFoodRepository.findByExternalIdAndUserId(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID, userId)).thenReturn(mealFood)
            whenever(mealFoodRepository.countByMealRecordId(MealRecordFixture.MEAL_RECORD_ID)).thenReturn(1)

            service.delete(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString(), userId)

            verify(mealFoodRepository).delete(mealFood)
            verify(mealFoodRepository).flush()
            verify(mealRecordRepository, never()).delete(any())
        }
    }

    @Nested
    inner class `끼니 전체 삭제` {

        @Test
        fun `본인 끼니와 소속 음식을 함께 삭제한다`() {
            val mealRecord = MealRecordFixture.mealRecord()
            val foods = listOf(MealRecordFixture.mealFood())
            whenever(mealRecordConverter.parseUuid(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString()))
                .thenReturn(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID)
            whenever(mealRecordRepository.findByExternalIdAndUserId(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID, userId))
                .thenReturn(mealRecord)
            whenever(mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(MealRecordFixture.MEAL_RECORD_ID)).thenReturn(foods)

            service.deleteMealRecord(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(), userId)

            verify(mealFoodRepository).deleteAll(foods)
            verify(mealRecordRepository).delete(mealRecord)
        }

        @Test
        fun `끼니가 없으면 MEAL_RECORD_NOT_FOUND`() {
            whenever(mealRecordConverter.parseUuid("bad")).thenReturn(null)

            assertThatThrownBy { service.deleteMealRecord("bad", userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        }
    }

    private fun judgment() = JudgmentResponseDTO(
        foodExternalId = foodExternalId.toString(),
        foodName = "된장찌개",
        category = "soup_stew",
        grade = JudgmentGrade.CAUTION,
        personalTitle = "속이 불편할 수 있어요",
        items = listOf(
            JudgmentResponseDTO.JudgmentItemDTO("맵고 짤 수 있어요", "천천히 드세요"),
            JudgmentResponseDTO.JudgmentItemDTO("자극 가능성이 있어요", "식후 바로 눕지 마세요"),
        ),
        stateRecords = JudgmentResponseDTO.StateRecordsDTO(total = 0, records = emptyList()),
        substitutes = emptyList(),
    )

    private fun mealFoodDetail() = com.gerd.domain.meal.dto.MealFoodRecordDetailDTO(
        mealFoodId = MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString(),
        eatenAt = "2026-06-11T12:30:00+09:00",
        food = com.gerd.domain.meal.dto.MealFoodRecordDetailDTO.FoodInfoDTO(
            mealRecordExternalId = MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
            name = "된장찌개",
            category = "soup_stew",
        ),
        analysis = null,
        stateRecord = null,
    )
}
