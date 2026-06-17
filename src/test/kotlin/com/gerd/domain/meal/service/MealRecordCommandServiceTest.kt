package com.gerd.domain.meal.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.dto.CreateMealRecordByTextRequestDTO
import com.gerd.domain.meal.dto.CreateMealRecordRequestDTO
import com.gerd.domain.meal.dto.UpdateMealMemoRequestDTO
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.FoodFixture
import com.gerd.global.fixture.MealRecordFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MealRecordCommandServiceTest {

    @Mock
    private lateinit var mealRecordRepository: MealRecordRepository

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var mealRecordAssembler: MealRecordAssembler

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var service: MealRecordCommandService

    private val userId = 1L
    private val otherUserId = 2L
    private val foodExternalId = FoodFixture.EXTERNAL_ID
    private val eatenAt = LocalDateTime.of(2026, 6, 11, 12, 30, 0)

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        service = MealRecordCommandService(mealRecordRepository, foodRepository, mealRecordAssembler, userRepository)
    }

    @Nested
    inner class `생성` {

        @Test
        fun `형식이 잘못된 음식 UUID면 FOOD_NOT_FOUND`() {
            whenever(mealRecordAssembler.parseUuid("bad")).thenReturn(null)

            assertThatThrownBy { service.create(request(foodExternalId = "bad"), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
            verify(foodRepository, never()).findByExternalId(any())
        }

        @Test
        fun `음식이 없으면 FOOD_NOT_FOUND`() {
            whenever(mealRecordAssembler.parseUuid(foodExternalId.toString())).thenReturn(foodExternalId)
            whenever(foodRepository.findByExternalId(foodExternalId)).thenReturn(null)

            assertThatThrownBy { service.create(request(), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
        }

        @Test
        fun `타인의 비공개 음식이면 노출 범위 밖이라 FOOD_NOT_FOUND`() {
            whenever(mealRecordAssembler.parseUuid(foodExternalId.toString())).thenReturn(foodExternalId)
            whenever(foodRepository.findByExternalId(foodExternalId)).thenReturn(
                FoodFixture.food(id = 10L, source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = otherUserId),
            )

            assertThatThrownBy { service.create(request(), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
        }

        @Test
        fun `끼니 미전달이면 새 끼니 키를 발급하고 등급 스냅샷을 저장한다`() {
            stubVisibleFood()
            stubUser()
            whenever(mealRecordAssembler.parseEatenAt(anyOrNull())).thenReturn(eatenAt)
            whenever(mealRecordRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(mealRecordAssembler.toSummary(any(), any())).thenReturn(summaryStub())

            service.create(request(mealGroupId = null, judgedGrade = JudgmentGrade.CAUTION), userId)

            val captor = argumentCaptor<MealRecord>()
            verify(mealRecordRepository).save(captor.capture())
            assertThat(captor.firstValue.mealGroupId).isNotNull()
            assertThat(captor.firstValue.judgedGrade).isEqualTo(JudgmentGrade.CAUTION)
            assertThat(captor.firstValue.eatenAt).isEqualTo(eatenAt)
            verify(mealRecordRepository, never()).existsByUser_IdAndMealGroupId(any(), any())
        }

        @Test
        fun `끼니 전달 시 본인 소유면 같은 끼니로 저장한다`() {
            val groupId = MealRecordFixture.MEAL_GROUP_ID
            stubVisibleFood()
            stubUser()
            whenever(mealRecordAssembler.parseUuid(groupId.toString())).thenReturn(groupId)
            whenever(mealRecordRepository.existsByUser_IdAndMealGroupId(userId, groupId)).thenReturn(true)
            whenever(mealRecordAssembler.parseEatenAt(anyOrNull())).thenReturn(eatenAt)
            whenever(mealRecordRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(mealRecordAssembler.toSummary(any(), any())).thenReturn(summaryStub())

            service.create(request(mealGroupId = groupId.toString()), userId)

            val captor = argumentCaptor<MealRecord>()
            verify(mealRecordRepository).save(captor.capture())
            assertThat(captor.firstValue.mealGroupId).isEqualTo(groupId)
        }

        @Test
        fun `끼니 전달 시 존재하지 않으면 MEAL_GROUP_NOT_FOUND`() {
            val groupId = MealRecordFixture.MEAL_GROUP_ID
            stubVisibleFood()
            whenever(mealRecordAssembler.parseUuid(groupId.toString())).thenReturn(groupId)
            whenever(mealRecordAssembler.parseEatenAt(anyOrNull())).thenReturn(eatenAt)
            whenever(mealRecordRepository.existsByUser_IdAndMealGroupId(userId, groupId)).thenReturn(false)

            assertThatThrownBy { service.create(request(mealGroupId = groupId.toString()), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.MEAL_GROUP_NOT_FOUND)
            verify(mealRecordRepository, never()).save(any())
        }

        @Test
        fun `끼니 키 형식이 잘못되면 MEAL_GROUP_NOT_FOUND`() {
            stubVisibleFood()
            whenever(mealRecordAssembler.parseUuid("bad-group")).thenReturn(null)
            whenever(mealRecordAssembler.parseEatenAt(anyOrNull())).thenReturn(eatenAt)

            assertThatThrownBy { service.create(request(mealGroupId = "bad-group"), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.MEAL_GROUP_NOT_FOUND)
        }
    }

    @Nested
    inner class `텍스트 생성` {

        @Test
        fun `동일 이름의 본인 USER 음식이 있으면 재사용한다`() {
            val existingFood = FoodFixture.food(id = 20L, name = "감자탕", source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = userId)
            whenever(foodRepository.findByNameAndOwnerUserIdAndSource("감자탕", userId, FoodSource.USER)).thenReturn(existingFood)
            whenever(userRepository.getReferenceById(userId)).thenReturn(User(email = "test@test.com"))
            whenever(mealRecordAssembler.parseEatenAt(anyOrNull())).thenReturn(eatenAt)
            whenever(mealRecordRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(mealRecordAssembler.toSummary(any(), any())).thenReturn(summaryStub())

            service.createByText(textRequest(foodTextInput = "감자탕"), userId)

            verify(foodRepository, never()).save(any())
            val captor = argumentCaptor<MealRecord>()
            verify(mealRecordRepository).save(captor.capture())
            assertThat(captor.firstValue.foodId).isEqualTo(20L)
        }

        @Test
        fun `동일 이름의 USER 음식이 없으면 새로 생성하고 기록한다`() {
            val newFood = FoodFixture.food(id = 21L, name = "감자탕", source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = userId)
            whenever(foodRepository.findByNameAndOwnerUserIdAndSource("감자탕", userId, FoodSource.USER)).thenReturn(null)
            whenever(foodRepository.save(any<com.gerd.domain.food.entity.Food>())).thenReturn(newFood)
            whenever(userRepository.getReferenceById(userId)).thenReturn(User(email = "test@test.com"))
            whenever(mealRecordAssembler.parseEatenAt(anyOrNull())).thenReturn(eatenAt)
            whenever(mealRecordRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(mealRecordAssembler.toSummary(any(), any())).thenReturn(summaryStub())

            service.createByText(textRequest(foodTextInput = "감자탕"), userId)

            val captor = argumentCaptor<MealRecord>()
            verify(mealRecordRepository).save(captor.capture())
            assertThat(captor.firstValue.foodId).isEqualTo(21L)
        }

        @Test
        fun `앞뒤 공백은 제거하고 동일 이름 기준으로 음식을 조회한다`() {
            val existingFood = FoodFixture.food(id = 22L, name = "감자탕", source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = userId)
            whenever(foodRepository.findByNameAndOwnerUserIdAndSource("감자탕", userId, FoodSource.USER)).thenReturn(existingFood)
            whenever(userRepository.getReferenceById(userId)).thenReturn(User(email = "test@test.com"))
            whenever(mealRecordAssembler.parseEatenAt(anyOrNull())).thenReturn(eatenAt)
            whenever(mealRecordRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(mealRecordAssembler.toSummary(any(), any())).thenReturn(summaryStub())

            service.createByText(textRequest(foodTextInput = "  감자탕  "), userId)

            verify(foodRepository).findByNameAndOwnerUserIdAndSource("감자탕", userId, FoodSource.USER)
        }
    }

    @Nested
    inner class `메모 수정` {

        @Test
        fun `기록이 없거나 타인 소유면 MEAL_NOT_FOUND`() {
            whenever(mealRecordAssembler.parseUuid(MealRecordFixture.MEAL_EXTERNAL_ID.toString()))
                .thenReturn(MealRecordFixture.MEAL_EXTERNAL_ID)
            whenever(mealRecordRepository.findByExternalIdAndUser_Id(MealRecordFixture.MEAL_EXTERNAL_ID, userId))
                .thenReturn(null)

            assertThatThrownBy { service.updateMemo(MealRecordFixture.MEAL_EXTERNAL_ID.toString(), UpdateMealMemoRequestDTO("메모"), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.MEAL_NOT_FOUND)
        }

        @Test
        fun `메모가 200자를 초과하면 MEMO_TOO_LONG`() {
            stubOwnedRecord()

            assertThatThrownBy { service.updateMemo(MealRecordFixture.MEAL_EXTERNAL_ID.toString(), UpdateMealMemoRequestDTO("가".repeat(201)), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.MEMO_TOO_LONG)
        }

        @Test
        fun `200자 이하 메모는 저장하고 빈 문자열은 메모를 비운다`() {
            val record = stubOwnedRecord()
            whenever(mealRecordAssembler.toDetail(record)).thenReturn(detailStub())

            service.updateMemo(MealRecordFixture.MEAL_EXTERNAL_ID.toString(), UpdateMealMemoRequestDTO("가".repeat(200)), userId)
            assertThat(record.memo).isEqualTo("가".repeat(200))

            service.updateMemo(MealRecordFixture.MEAL_EXTERNAL_ID.toString(), UpdateMealMemoRequestDTO(""), userId)
            assertThat(record.memo).isNull()
        }
    }

    @Nested
    inner class `삭제` {

        @Test
        fun `기록이 없으면 MEAL_NOT_FOUND`() {
            whenever(mealRecordAssembler.parseUuid(MealRecordFixture.MEAL_EXTERNAL_ID.toString()))
                .thenReturn(MealRecordFixture.MEAL_EXTERNAL_ID)
            whenever(mealRecordRepository.findByExternalIdAndUser_Id(MealRecordFixture.MEAL_EXTERNAL_ID, userId))
                .thenReturn(null)

            assertThatThrownBy { service.delete(MealRecordFixture.MEAL_EXTERNAL_ID.toString(), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.MEAL_NOT_FOUND)
            verify(mealRecordRepository, never()).delete(any())
        }

        @Test
        fun `본인 기록이면 soft delete를 위임한다`() {
            val record = stubOwnedRecord()

            service.delete(MealRecordFixture.MEAL_EXTERNAL_ID.toString(), userId)

            verify(mealRecordRepository).delete(record)
        }
    }

    private fun textRequest(
        foodTextInput: String = "감자탕",
        eatenAt: String? = null,
        mealGroupId: String? = null,
        judgedGrade: JudgmentGrade? = null,
    ) = CreateMealRecordByTextRequestDTO(foodTextInput, eatenAt, mealGroupId, judgedGrade)

    private fun request(
        foodExternalId: String = this.foodExternalId.toString(),
        eatenAt: String? = null,
        mealGroupId: String? = null,
        judgedGrade: JudgmentGrade? = JudgmentGrade.RECOMMEND,
    ) = CreateMealRecordRequestDTO(foodExternalId, eatenAt, mealGroupId, judgedGrade)

    private fun stubVisibleFood() {
        whenever(mealRecordAssembler.parseUuid(foodExternalId.toString())).thenReturn(foodExternalId)
        whenever(foodRepository.findByExternalId(foodExternalId)).thenReturn(FoodFixture.food(id = 10L))
    }

    private fun stubUser() {
        whenever(userRepository.getReferenceById(userId)).thenReturn(User(email = "test@test.com"))
    }

    private fun stubOwnedRecord(): MealRecord {
        val record = MealRecordFixture.mealRecord(id = 1L)
        whenever(mealRecordAssembler.parseUuid(MealRecordFixture.MEAL_EXTERNAL_ID.toString()))
            .thenReturn(MealRecordFixture.MEAL_EXTERNAL_ID)
        whenever(mealRecordRepository.findByExternalIdAndUser_Id(MealRecordFixture.MEAL_EXTERNAL_ID, userId))
            .thenReturn(record)
        return record
    }

    private fun summaryStub() = com.gerd.domain.meal.dto.MealRecordSummaryDTO(
        mealId = MealRecordFixture.MEAL_EXTERNAL_ID.toString(),
        mealGroupId = MealRecordFixture.MEAL_GROUP_ID.toString(),
        eatenAt = "2026-06-11T12:30:00+09:00",
        food = com.gerd.domain.food.dto.FoodSummaryDTO("x", "음식", "cat"),
        judgedGrade = JudgmentGrade.RECOMMEND,
    )

    private fun detailStub() = com.gerd.domain.meal.dto.MealRecordDetailDTO(
        mealId = MealRecordFixture.MEAL_EXTERNAL_ID.toString(),
        mealGroupId = MealRecordFixture.MEAL_GROUP_ID.toString(),
        eatenAt = "2026-06-11T12:30:00+09:00",
        memo = null,
        judgedGrade = JudgmentGrade.RECOMMEND,
        food = com.gerd.domain.meal.dto.MealRecordDetailDTO.MealFoodDetailDTO("x", "음식", "cat", null),
        stateRecords = emptyList(),
    )
}
