package com.gerd.domain.food.service

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.util.UUID

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

    @InjectMocks
    private lateinit var service: FoodSearchService

    private val userId = 1L

    private fun food(id: Long, name: String): Food =
        Food(name = name, source = FoodSource.SEED, visibility = FoodVisibility.PUBLIC).apply {
            ReflectionTestUtils.setField(this, "id", id)
            externalId = UUID.fromString("00000000-0000-0000-0000-00000000000$id") // BaseEntity의 public var로 직접 할당
        }

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
            whenever(foodRepository.search("감자된장", 10, userId)).thenReturn(emptyList())

            service.search("  감자 된장  ", null, userId)

            verify(foodRepository).search("감자된장", 10, userId)
        }

        @Test
        fun `size를 1과 50 사이로 보정한다`() {
            whenever(foodRepository.search("된장", 50, userId)).thenReturn(emptyList())

            service.search("된장", 999, userId)

            verify(foodRepository).search("된장", 50, userId)
        }

        @Test
        fun `결과를 externalId와 카테고리를 포함한 DTO로 매핑한다`() {
            val food = food(7, "된장찌개")
            whenever(foodRepository.search("된장찌개", 10, userId)).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadByFoodIds(listOf(7L)))
                .thenReturn(mapOf(7L to listOf("soup_stew")))

            val result = service.search("된장찌개", null, userId)

            assertThat(result).hasSize(1)
            assertThat(result[0].externalId).isEqualTo(food.externalId.toString())
            assertThat(result[0].name).isEqualTo("된장찌개")
            assertThat(result[0].categories).containsExactly("soup_stew")
        }
    }
}
