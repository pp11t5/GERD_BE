package com.gerd.domain.food.service

import com.gerd.domain.food.entity.FoodCategory
import com.gerd.domain.food.entity.FoodCategoryMap
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import com.gerd.domain.food.repository.FoodCategoryRepository
import com.gerd.global.fixture.FoodFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException

@ExtendWith(MockitoExtension::class)
class FoodCategoryAssignerTest {

    @Mock private lateinit var foodCategoryRepository: FoodCategoryRepository
    @Mock private lateinit var foodCategoryMapRepository: FoodCategoryMapRepository

    private val assigner by lazy { FoodCategoryAssigner(foodCategoryRepository, foodCategoryMapRepository) }

    @Test
    fun `categoryCode가 null이면 아무것도 하지 않는다`() {
        assigner.assignIfPresent(FoodFixture.food(), null)

        verify(foodCategoryRepository, never()).findByCode(any())
        verify(foodCategoryMapRepository, never()).save(any())
    }

    @Test
    fun `알 수 없는 code면 저장하지 않는다`() {
        whenever(foodCategoryRepository.findByCode("unknown")).thenReturn(null)

        assigner.assignIfPresent(FoodFixture.food(), "unknown")

        verify(foodCategoryMapRepository, never()).save(any())
    }

    @Test
    fun `유효한 code면 카테고리 매핑을 저장한다`() {
        val food = FoodFixture.food()
        val category = FoodCategory(code = "soup_stew", displayName = "국·찌개")
        whenever(foodCategoryRepository.findByCode("soup_stew")).thenReturn(category)

        assigner.assignIfPresent(food, "soup_stew")

        val captor = argumentCaptor<FoodCategoryMap>()
        verify(foodCategoryMapRepository).save(captor.capture())
        assertThat(captor.firstValue.food).isEqualTo(food)
        assertThat(captor.firstValue.foodCategory).isEqualTo(category)
    }

    @Test
    fun `경합으로 이미 매핑이 있으면 예외를 삼킨다`() {
        val food = FoodFixture.food()
        val category = FoodCategory(code = "soup_stew", displayName = "국·찌개")
        whenever(foodCategoryRepository.findByCode("soup_stew")).thenReturn(category)
        whenever(foodCategoryMapRepository.save(any())).thenThrow(DataIntegrityViolationException("dup"))

        assigner.assignIfPresent(food, "soup_stew")
    }
}
