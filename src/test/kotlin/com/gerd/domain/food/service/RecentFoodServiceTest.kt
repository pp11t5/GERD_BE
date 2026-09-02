package com.gerd.domain.food.service

import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodSearchHistoryRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.FoodFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class RecentFoodServiceTest {

    @Mock
    private lateinit var foodSearchHistoryRepository: FoodSearchHistoryRepository

    @Mock
    private lateinit var topSearchedFoodCache: TopSearchedFoodCache

    @InjectMocks
    private lateinit var service: RecentFoodService

    private val userId = 1L

    @Nested
    inner class recordQuery {

        @Test
        fun `기존 기록이 없으면 새로 저장한다`() {
            whenever(foodSearchHistoryRepository.findByUserIdAndQuery(userId, "된장찌개")).thenReturn(null)
            whenever(foodSearchHistoryRepository.save(any()))
                .thenReturn(FoodFixture.history(1, query = "된장찌개", searchedAt = LocalDateTime.now()))

            service.record("된장찌개", userId)

            verify(foodSearchHistoryRepository).save(any())
        }

        @Test
        fun `기존 기록이 있으면 새로 저장하지 않고 시각만 갱신한다`() {
            val existing = FoodFixture.history(100, query = "된장찌개", searchedAt = LocalDateTime.of(2026, 6, 1, 0, 0))
            whenever(foodSearchHistoryRepository.findByUserIdAndQuery(userId, "된장찌개")).thenReturn(existing)

            val before = existing.searchedAt
            service.record("된장찌개", userId)

            assertThat(existing.searchedAt).isAfter(before)
            verify(foodSearchHistoryRepository, never()).save(any())
        }

        @Test
        fun `보관 상한을 초과하면 오래된 것부터 삭제한다`() {
            whenever(foodSearchHistoryRepository.findByUserIdAndQuery(userId, "된장찌개")).thenReturn(null)
            whenever(foodSearchHistoryRepository.save(any()))
                .thenReturn(FoodFixture.history(1, query = "된장찌개", searchedAt = LocalDateTime.now()))
            // 최근순 11개 — 상한(10) 이후 1개가 삭제 대상
            whenever(foodSearchHistoryRepository.findIdsByUserIdOrderByRecent(userId)).thenReturn((1L..11L).toList())

            service.record("된장찌개", userId)

            verify(foodSearchHistoryRepository).deleteAllById(listOf(11L))
        }
    }

    @Nested
    inner class deleteRecent {

        @Test
        fun `본인 항목이 없으면 RECENT_NOT_FOUND`() {
            whenever(foodSearchHistoryRepository.findByIdAndUserId(100L, userId)).thenReturn(null)

            assertThatThrownBy { service.deleteRecent(100L, userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.RECENT_NOT_FOUND)
        }

        @Test
        fun `본인 항목이면 삭제한다`() {
            val existing = FoodFixture.history(100, searchedAt = LocalDateTime.now())
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

    @Nested
    inner class getTopSearched {

        // Mockito가 미스텁 List 반환을 emptyList()로 채우는 것 방지 — 캐시 미스(null) 기본값 고정
        @BeforeEach
        fun setUp() {
            whenever(topSearchedFoodCache.get()).thenReturn(null)
        }

        @Test
        fun `검색 기록이 없으면 빈 리스트를 반환한다`() {
            whenever(foodSearchHistoryRepository.findTop3MostSearched()).thenReturn(emptyList())

            val result = service.getTopSearched()

            assertThat(result).isEmpty()
        }

        @Test
        fun `검색 횟수 기준 상위 음식을 FoodSummaryDTO로 매핑한다`() {
            // mock() 호출을 whenever().thenReturn() 인자 안에서 할 경우 Mockito 진행 중 스터빙 상태와 충돌
            val views = listOf(
                mockView("ext-1", "된장찌개", "soup_stew"),
                mockView("ext-2", "비빔밥", "rice"),
                mockView("ext-3", "삼겹살", "meat"),
            )
            whenever(foodSearchHistoryRepository.findTop3MostSearched()).thenReturn(views)

            val result = service.getTopSearched()

            assertThat(result).containsExactly(
                FoodSummaryDTO("ext-1", "된장찌개", "soup_stew"),
                FoodSummaryDTO("ext-2", "비빔밥", "rice"),
                FoodSummaryDTO("ext-3", "삼겹살", "meat"),
            )
        }

        @Test
        fun `카테고리가 없는 음식도 null로 포함된다`() {
            val views = listOf(mockView("ext-1", "된장찌개", null))
            whenever(foodSearchHistoryRepository.findTop3MostSearched()).thenReturn(views)

            val result = service.getTopSearched()

            assertThat(result[0].category).isNull()
        }

        @Test
        fun `캐시에 값이 있으면 조회 없이 캐시값을 반환한다`() {
            val cached = listOf(FoodSummaryDTO("ext-1", "된장찌개", "soup_stew"))
            whenever(topSearchedFoodCache.get()).thenReturn(cached)

            val result = service.getTopSearched()

            assertThat(result).isEqualTo(cached)
            verify(foodSearchHistoryRepository, never()).findTop3MostSearched()
        }

        @Test
        fun `캐시가 비어있으면 조회 후 캐시에 채운다`() {
            whenever(topSearchedFoodCache.get()).thenReturn(null)
            val views = listOf(mockView("ext-1", "된장찌개", "soup_stew"))
            whenever(foodSearchHistoryRepository.findTop3MostSearched()).thenReturn(views)

            val result = service.getTopSearched()

            verify(topSearchedFoodCache).put(result)
        }

        private fun mockView(
            externalId: String,
            name: String,
            category: String?,
        ): FoodSearchHistoryRepository.TopSearchedFoodView =
            mock<FoodSearchHistoryRepository.TopSearchedFoodView>().also {
                whenever(it.getExternalId()).thenReturn(externalId)
                whenever(it.getName()).thenReturn(name)
                whenever(it.getCategory()).thenReturn(category)
            }
    }
}
