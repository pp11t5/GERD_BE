package com.gerd.domain.food.service

import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.FoodFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * 음식 검색 서비스 단위 테스트
 *
 * 검색어 검증·정규화(공백 제거)·size 보정·DTO 매핑·hasExactMatch 판정 등 서비스 로직만 mock으로 검증한다.
 * 실제 이름 매칭(ILIKE·공백 무시·정렬)은 DB 엔진에 의존하므로 H2로는 충실히 재현할 수 없어 제외한다.
 * 검색 시점에 음식 생성은 수행하지 않는다 — 생성은 식사 기록 등록 시점에 위임한다.
 */
@ExtendWith(MockitoExtension::class)
class FoodSearchServiceTest {

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var foodCategoryReader: FoodCategoryReader

    @InjectMocks
    private lateinit var service: FoodSearchService

    private val userId = 1L

    @Nested
    inner class `검색어 검증` {

        @Test
        fun `검색어가 null이면 INVALID_SEARCH_QUERY`() {
            assertThatThrownBy { service.search(null, null, userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.INVALID_SEARCH_QUERY)
            verify(foodRepository, never()).search(any(), any(), any())
        }

        @Test
        fun `검색어가 공백뿐이면 INVALID_SEARCH_QUERY`() {
            assertThatThrownBy { service.search("   ", null, userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.INVALID_SEARCH_QUERY)
        }

        @Test
        fun `검색어가 최대 길이를 초과하면 INVALID_SEARCH_QUERY`() {
            assertThatThrownBy { service.search("가".repeat(101), null, userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.INVALID_SEARCH_QUERY)
        }
    }

    @Nested
    inner class search {

        @Test
        fun `공백을 제거한 검색어로 리포지토리를 조회한다`() {
            val food = FoodFixture.food(id = 1, name = "감자된장")
            whenever(foodRepository.search("감자된장", 10, userId)).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(any())).thenReturn(emptyMap())

            service.search("  감자 된장  ", null, userId)

            verify(foodRepository).search("감자된장", 10, userId)
        }

        @Test
        fun `size를 1과 50 사이로 보정한다`() {
            val food = FoodFixture.food(id = 1, name = "된장")
            whenever(foodRepository.search("된장", 50, userId)).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(any())).thenReturn(emptyMap())

            service.search("된장", 999, userId)

            verify(foodRepository).search("된장", 50, userId)
        }

        @Test
        fun `결과를 externalId와 카테고리를 포함한 DTO로 매핑한다`() {
            val food = FoodFixture.food(id = 7, name = "된장찌개")
            whenever(foodRepository.search("된장찌개", 10, userId)).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(7L))).thenReturn(mapOf(7L to "soup_stew"))

            val result = service.search("된장찌개", null, userId)

            assertThat(result.foods).hasSize(1)
            assertThat(result.foods[0].externalId).isEqualTo(food.externalId.toString())
            assertThat(result.foods[0].name).isEqualTo("된장찌개")
            assertThat(result.foods[0].category).isEqualTo("soup_stew")
        }

        @Test
        fun `완전 일치 결과가 있으면 hasExactMatch가 true다`() {
            val food = FoodFixture.food(id = 3, name = "된장찌개")
            whenever(foodRepository.search("된장찌개", 10, userId)).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(any())).thenReturn(emptyMap())

            val result = service.search("된장찌개", null, userId)

            assertThat(result.hasExactMatch).isTrue()
        }

        @Test
        fun `완전 일치 결과가 없으면 hasExactMatch가 false다`() {
            val food = FoodFixture.food(id = 4, name = "된장찌개볶음")
            whenever(foodRepository.search("된장", 10, userId)).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(any())).thenReturn(emptyMap())

            val result = service.search("된장", null, userId)

            assertThat(result.hasExactMatch).isFalse()
        }

        @Test
        fun `공백 포함 검색어는 공백 무시 후 완전 일치를 판단한다`() {
            val food = FoodFixture.food(id = 4, name = "된 장 찌 개")
            whenever(foodRepository.search("된장찌개", 10, userId)).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(any())).thenReturn(emptyMap())

            val result = service.search("된 장 찌 개", null, userId)

            assertThat(result.hasExactMatch).isTrue()
        }

        @Test
        fun `검색 결과가 없어도 음식을 생성하지 않는다`() {
            whenever(foodRepository.search("신메뉴", 10, userId)).thenReturn(emptyList())
            whenever(foodCategoryReader.loadPrimaryByFoodIds(any())).thenReturn(emptyMap())

            val result = service.search("신메뉴", null, userId)

            assertThat(result.foods).isEmpty()
            assertThat(result.hasExactMatch).isFalse()
        }
    }
}
