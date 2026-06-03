package com.gerd.domain.food.service

import com.gerd.domain.food.dto.FoodCategoryDTO
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import org.springframework.stereotype.Component

// 검색·최근 응답 조립 시 음식별 카테고리를 한 번에 로딩해 N+1을 막는다 (두 서비스 공용)
@Component
class FoodCategoryReader(
    private val foodCategoryMapRepository: FoodCategoryMapRepository,
) {

    fun loadByFoodIds(foodIds: List<Long>): Map<Long, List<FoodCategoryDTO>> {
        if (foodIds.isEmpty()) return emptyMap()
        return foodCategoryMapRepository.findCategoryViewsByFoodIds(foodIds)
            .groupBy({ it.foodId }, { FoodCategoryDTO(it.code, it.displayName) })
    }
}
