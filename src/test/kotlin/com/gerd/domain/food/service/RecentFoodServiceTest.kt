package com.gerd.domain.food.service

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.FoodSearchHistory
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.repository.FoodSearchHistoryRepository
import com.gerd.global.apiPayload.GeneralException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RecentFoodServiceTest {

    @Mock
    private lateinit var foodSearchHistoryRepository: FoodSearchHistoryRepository

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var foodCategoryReader: FoodCategoryReader

    @InjectMocks
    private lateinit var service: RecentFoodService

    private val userId = 1L
    private val otherUserId = 2L
    private val externalId = UUID.fromString("9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")

    private fun food(
        id: Long,
        source: FoodSource = FoodSource.SEED,
        visibility: FoodVisibility = FoodVisibility.PUBLIC,
        ownerUserId: Long? = null,
    ): Food = Food(name = "된장찌개", source = source, visibility = visibility, ownerUserId = ownerUserId).apply {
        ReflectionTestUtils.setField(this, "id", id)
        externalId = this@RecentFoodServiceTest.externalId // BaseEntity의 public var로 직접 할당
    }

    private fun history(id: Long, food: Food, searchedAt: LocalDateTime): FoodSearchHistory =
        FoodSearchHistory(userId = userId, food = food, searchedAt = searchedAt).apply {
            ReflectionTestUtils.setField(this, "id", id)
        }

    @Nested
    inner class addRecent {

        @Test
        fun `형식이 잘못된 UUID면 FOOD_NOT_FOUND`() {
            assertThatThrownBy { service.addRecent("not-a-uuid", userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
            verify(foodRepository, never()).findByExternalId(any())
        }

        @Test
        fun `음식이 없으면 FOOD_NOT_FOUND`() {
            whenever(foodRepository.findByExternalId(externalId)).thenReturn(null)

            assertThatThrownBy { service.addRecent(externalId.toString(), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
        }

        @Test
        fun `타인의 비공개 음식이면 노출 범위 밖이라 FOOD_NOT_FOUND`() {
            whenever(foodRepository.findByExternalId(externalId))
                .thenReturn(food(10, source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = otherUserId))

            assertThatThrownBy { service.addRecent(externalId.toString(), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
        }

        @Test
        fun `기존 기록이 없으면 새로 저장한다`() {
            val food = food(10)
            whenever(foodRepository.findByExternalId(externalId)).thenReturn(food)
            whenever(foodSearchHistoryRepository.findByUserIdAndFoodId(userId, 10L)).thenReturn(null)
            whenever(foodSearchHistoryRepository.save(any())).thenAnswer { history(100, food, LocalDateTime.now()) }
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(10L))).thenReturn(emptyMap())

            val result = service.addRecent(externalId.toString(), userId)

            assertThat(result.foodExternalId).isEqualTo(externalId.toString())
            verify(foodSearchHistoryRepository).save(any())
        }

        @Test
        fun `기존 기록이 있으면 새로 저장하지 않고 시각만 갱신한다`() {
            val food = food(10)
            val existing = history(100, food, LocalDateTime.of(2026, 6, 1, 0, 0))
            whenever(foodRepository.findByExternalId(externalId)).thenReturn(food)
            whenever(foodSearchHistoryRepository.findByUserIdAndFoodId(userId, 10L)).thenReturn(existing)
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(10L))).thenReturn(emptyMap())

            val before = existing.searchedAt
            val result = service.addRecent(externalId.toString(), userId)

            assertThat(result.recentId).isEqualTo(100L)
            assertThat(existing.searchedAt).isAfter(before)
            verify(foodSearchHistoryRepository, never()).save(any())
        }

        @Test
        fun `보관 상한을 초과하면 오래된 것부터 삭제한다`() {
            val food = food(10)
            whenever(foodRepository.findByExternalId(externalId)).thenReturn(food)
            whenever(foodSearchHistoryRepository.findByUserIdAndFoodId(userId, 10L)).thenReturn(null)
            whenever(foodSearchHistoryRepository.save(any())).thenAnswer { history(111, food, LocalDateTime.now()) }
            // 최근순 11개 — 상한(10) 이후 1개가 삭제 대상
            whenever(foodSearchHistoryRepository.findIdsByUserIdOrderByRecent(userId)).thenReturn((1L..11L).toList())
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(10L))).thenReturn(emptyMap())

            service.addRecent(externalId.toString(), userId)

            verify(foodSearchHistoryRepository).deleteAllById(listOf(11L))
        }
    }

    @Nested
    inner class deleteRecent {

        @Test
        fun `본인 항목이 없으면 RECENT_NOT_FOUND`() {
            whenever(foodSearchHistoryRepository.findByIdAndUserId(999L, userId)).thenReturn(null)

            assertThatThrownBy { service.deleteRecent(999L, userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.RECENT_NOT_FOUND)
        }

        @Test
        fun `본인 항목이면 삭제한다`() {
            val existing = history(100, food(10), LocalDateTime.now())
            whenever(foodSearchHistoryRepository.findByIdAndUserId(100L, userId)).thenReturn(existing)

            service.deleteRecent(100L, userId)

            verify(foodSearchHistoryRepository).delete(existing)
        }
    }

    @Nested
    inner class deleteAllRecent {

        @Test
        fun `사용자의 모든 기록을 삭제한다`() {
            service.deleteAllRecent(userId)

            verify(foodSearchHistoryRepository).deleteByUserId(eq(userId))
        }
    }
}
