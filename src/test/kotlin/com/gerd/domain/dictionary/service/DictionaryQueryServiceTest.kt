package com.gerd.domain.dictionary.service

import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import com.gerd.domain.food.repository.FoodCategoryView
import com.gerd.global.fixture.DictionaryFixture
import com.gerd.global.fixture.FoodFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class DictionaryQueryServiceTest {

    @Mock private lateinit var dictionaryRepository: UserFoodDictionaryRepository
    @Mock private lateinit var foodCategoryMapRepository: FoodCategoryMapRepository

    private val service by lazy {
        DictionaryQueryService(
            dictionaryRepository = dictionaryRepository,
            foodCategoryMapRepository = foodCategoryMapRepository,
        )
    }

    private val userId = 1L

    private fun categoryView(foodId: Long, code: String): FoodCategoryView = object : FoodCategoryView {
        override val foodId = foodId
        override val code = code
    }

    @Nested
    inner class `도감 개수 조회` {

        @Test
        fun `SAFE와 CAUTION·RISK 개수를 각각 반환한다`() {
            whenever(dictionaryRepository.countByUser_IdAndDictionaryType(userId, DictionaryType.SAFE))
                .thenReturn(5L)
            whenever(dictionaryRepository.countByUser_IdAndDictionaryTypeIn(
                userId, listOf(DictionaryType.CAUTION, DictionaryType.RISK),
            )).thenReturn(3L)

            val result = service.getCount(userId)

            assertThat(result.safeCount).isEqualTo(5)
            assertThat(result.cautionRiskCount).isEqualTo(3)
        }

        @Test
        fun `도감 항목이 없으면 0을 반환한다`() {
            whenever(dictionaryRepository.countByUser_IdAndDictionaryType(userId, DictionaryType.SAFE))
                .thenReturn(0L)
            whenever(dictionaryRepository.countByUser_IdAndDictionaryTypeIn(any(), any()))
                .thenReturn(0L)

            val result = service.getCount(userId)

            assertThat(result.safeCount).isEqualTo(0)
            assertThat(result.cautionRiskCount).isEqualTo(0)
        }
    }

    @Nested
    inner class `안전 음식 조회` {

        @Test
        fun `결과가 size 이하면 hasNext가 false다`() {
            val food = FoodFixture.food(id = 1L)
            val entry = DictionaryFixture.entry(id = 1L, food = food, type = DictionaryType.SAFE)
            whenever(dictionaryRepository.findWithFoodByCursorAndType(
                eq(userId), eq(DictionaryType.SAFE), eq(null), any(),
            )).thenReturn(listOf(entry))
            whenever(foodCategoryMapRepository.findCategoryViewsByFoodIds(listOf(1L)))
                .thenReturn(emptyList())

            val result = service.getSafeFoods(rawSize = 20, cursor = null, userId = userId)

            assertThat(result.hasNext).isFalse()
            assertThat(result.nextCursor).isNull()
            assertThat(result.items).hasSize(1)
        }

        @Test
        fun `결과가 size+1이면 hasNext가 true이고 nextCursor는 마지막 항목의 id다`() {
            val size = 2
            // size+1 = 3개 반환
            val entries = listOf(
                DictionaryFixture.entry(id = 10L, food = FoodFixture.food(id = 1L), type = DictionaryType.SAFE),
                DictionaryFixture.entry(id = 9L, food = FoodFixture.food(id = 2L), type = DictionaryType.SAFE),
                DictionaryFixture.entry(id = 8L, food = FoodFixture.food(id = 3L), type = DictionaryType.SAFE),
            )
            whenever(dictionaryRepository.findWithFoodByCursorAndType(
                eq(userId), eq(DictionaryType.SAFE), eq(null), any(),
            )).thenReturn(entries)
            whenever(foodCategoryMapRepository.findCategoryViewsByFoodIds(any()))
                .thenReturn(emptyList())

            val result = service.getSafeFoods(rawSize = size, cursor = null, userId = userId)

            assertThat(result.hasNext).isTrue()
            assertThat(result.nextCursor).isEqualTo(9L) // pageEntries.last().id
            assertThat(result.items).hasSize(2)
        }

        @Test
        fun `카테고리 코드를 음식 id 기준으로 매핑한다`() {
            val food = FoodFixture.food(id = 1L)
            val entry = DictionaryFixture.entry(id = 1L, food = food, type = DictionaryType.SAFE)
            whenever(dictionaryRepository.findWithFoodByCursorAndType(
                eq(userId), eq(DictionaryType.SAFE), eq(null), any(),
            )).thenReturn(listOf(entry))
            whenever(foodCategoryMapRepository.findCategoryViewsByFoodIds(listOf(1L)))
                .thenReturn(listOf(categoryView(foodId = 1L, code = "grain")))

            val result = service.getSafeFoods(rawSize = 20, cursor = null, userId = userId)

            assertThat(result.items.first().code).isEqualTo("grain")
        }

        @Test
        fun `externalId가 null인 음식 항목은 결과에서 제외한다`() {
            val foodWithoutExternalId = FoodFixture.food(id = 1L).apply {
                externalId = null
            }
            val entry = DictionaryFixture.entry(id = 1L, food = foodWithoutExternalId, type = DictionaryType.SAFE)
            whenever(dictionaryRepository.findWithFoodByCursorAndType(
                eq(userId), eq(DictionaryType.SAFE), eq(null), any(),
            )).thenReturn(listOf(entry))

            val result = service.getSafeFoods(rawSize = 20, cursor = null, userId = userId)

            assertThat(result.items).isEmpty()
        }

        @Test
        fun `rawSize가 null이면 기본값 20으로 조회한다`() {
            whenever(dictionaryRepository.findWithFoodByCursorAndType(
                eq(userId), eq(DictionaryType.SAFE), eq(null), eq(PageRequest.of(0, 21)),
            )).thenReturn(emptyList())

            service.getSafeFoods(rawSize = null, cursor = null, userId = userId)

            // 기본 size=20 → pageable size=21
        }

        @Test
        fun `rawSize가 MAX_SIZE를 초과하면 MAX_SIZE로 제한한다`() {
            whenever(dictionaryRepository.findWithFoodByCursorAndType(
                eq(userId), eq(DictionaryType.SAFE), eq(null), eq(PageRequest.of(0, 51)),
            )).thenReturn(emptyList())

            service.getSafeFoods(rawSize = 999, cursor = null, userId = userId)

            // clamp 후 size=50 → pageable size=51
        }
    }

    @Nested
    inner class `주의·위험 음식 조회` {

        @Test
        fun `CAUTION과 RISK 타입을 함께 조회한다`() {
            val cautionFood = FoodFixture.food(id = 1L)
            val riskFood = FoodFixture.food(id = 2L)
            val entries = listOf(
                DictionaryFixture.entry(id = 2L, food = cautionFood, type = DictionaryType.CAUTION),
                DictionaryFixture.entry(id = 1L, food = riskFood, type = DictionaryType.RISK),
            )
            whenever(dictionaryRepository.findWithFoodByCursorAndTypeIn(
                eq(userId), eq(listOf(DictionaryType.CAUTION, DictionaryType.RISK)), eq(null), any(),
            )).thenReturn(entries)
            whenever(foodCategoryMapRepository.findCategoryViewsByFoodIds(any()))
                .thenReturn(emptyList())

            val result = service.getCautionRiskFoods(rawSize = 20, cursor = null, userId = userId)

            assertThat(result.items).hasSize(2)
            assertThat(result.items.map { it.type }).containsExactly(DictionaryType.CAUTION, DictionaryType.RISK)
        }

        @Test
        fun `cursor가 있으면 해당 id 이하의 항목만 조회한다`() {
            whenever(dictionaryRepository.findWithFoodByCursorAndTypeIn(
                eq(userId), eq(listOf(DictionaryType.CAUTION, DictionaryType.RISK)), eq(5L), any(),
            )).thenReturn(emptyList())

            val result = service.getCautionRiskFoods(rawSize = 20, cursor = 5L, userId = userId)

            assertThat(result.items).isEmpty()
            assertThat(result.hasNext).isFalse()
        }

        @Test
        fun `결과가 비어있으면 hasNext false, nextCursor null이다`() {
            whenever(dictionaryRepository.findWithFoodByCursorAndTypeIn(any(), any(), anyOrNull(), any()))
                .thenReturn(emptyList())

            val result = service.getCautionRiskFoods(rawSize = 20, cursor = null, userId = userId)

            assertThat(result.hasNext).isFalse()
            assertThat(result.nextCursor).isNull()
            assertThat(result.items).isEmpty()
        }
    }
}
