package com.gerd.domain.food.service

import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.repository.UserFoodRepository
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
 * 음식 검색 서비스 단위 테스트 (기본 동작 위주)
 *
 * 검색어 검증·정규화(공백 제거)·size 보정·DTO 매핑 등 서비스 로직만 mock으로 검증한다.
 * 실제 이름 매칭(ILIKE·공백 무시·정렬)은 DB 엔진에 의존하는데, 테스트 DB(H2)와 운영 DB(PostgreSQL)는
 * 검색 동작이 달라 H2로는 충실히 재현할 수 없다. 따라서 DB 레벨 검색은 여기서 테스트하지 않고
 * 실제 PostgreSQL에서 검증한다 (까다로운 매칭/정렬 케이스는 H2↔PostgreSQL 차이로 의미가 없어 제외).
 */
@ExtendWith(MockitoExtension::class)
class FoodSearchServiceTest {

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var foodCategoryReader: FoodCategoryReader

    @Mock
    private lateinit var userFoodRepository: UserFoodRepository

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
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(7L)))
                .thenReturn(mapOf(7L to "soup_stew"))

            val result = service.search("된장찌개", null, userId)

            assertThat(result).hasSize(1)
            assertThat(result[0].externalId).isEqualTo(food.externalId.toString())
            assertThat(result[0].name).isEqualTo("된장찌개")
            assertThat(result[0].category).isEqualTo("soup_stew")
        }

        @Test
        fun `완전 일치 결과가 있으면 UserFood를 새로 만들지 않는다`() {
            val food = FoodFixture.food(id = 3, name = "된장찌개")
            whenever(foodRepository.search("된장찌개", 10, userId)).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(any())).thenReturn(emptyMap())

            service.search("된장찌개", null, userId)

            verify(userFoodRepository, never()).save(any())
            verify(userFoodRepository, never()).existsByUserIdAndFoodId(any(), any())
        }

        @Test
        fun `완전 일치 없고 기존 USER 음식도 없으면 Food와 UserFood를 신규 생성한다`() {
            val newFood = FoodFixture.food(id = 99, name = "신메뉴", source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = userId)
            whenever(foodRepository.search("신메뉴", 10, userId)).thenReturn(emptyList())
            whenever(foodRepository.findByNameAndOwnerUserIdAndSource("신메뉴", userId, FoodSource.USER)).thenReturn(null)
            whenever(foodRepository.save(any())).thenReturn(newFood)
            whenever(userFoodRepository.existsByUserIdAndFoodId(userId, 99L)).thenReturn(false)
            whenever(userFoodRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(foodCategoryReader.loadPrimaryByFoodIds(any())).thenReturn(emptyMap())

            val result = service.search("신메뉴", null, userId)

            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("신메뉴")
            verify(foodRepository).save(any())
            verify(userFoodRepository).save(any())
        }

        @Test
        fun `완전 일치 없고 기존 USER 음식이 있으면 재사용하고 UserFood만 확인한다`() {
            val existing = FoodFixture.food(id = 55, name = "집밥", source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = userId)
            whenever(foodRepository.search("집밥", 10, userId)).thenReturn(emptyList())
            whenever(foodRepository.findByNameAndOwnerUserIdAndSource("집밥", userId, FoodSource.USER)).thenReturn(existing)
            whenever(userFoodRepository.existsByUserIdAndFoodId(userId, 55L)).thenReturn(true)
            whenever(foodCategoryReader.loadPrimaryByFoodIds(any())).thenReturn(emptyMap())

            val result = service.search("집밥", null, userId)

            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("집밥")
            verify(foodRepository, never()).save(any())
            verify(userFoodRepository, never()).save(any())
        }

        @Test
        fun `공백 포함 검색어도 공백 무시 후 완전 일치 판단한다`() {
            val food = FoodFixture.food(id = 4, name = "된 장 찌 개")
            whenever(foodRepository.search("된장찌개", 10, userId)).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(any())).thenReturn(emptyMap())

            service.search("된 장 찌 개", null, userId)

            verify(userFoodRepository, never()).save(any())
        }
    }
}
