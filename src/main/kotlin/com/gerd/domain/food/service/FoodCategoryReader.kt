package com.gerd.domain.food.service

import com.gerd.domain.food.repository.FoodCategoryMapRepository
import org.springframework.stereotype.Component

// 검색·최근 응답 조립 시 음식별 대표 카테고리 code를 한 번에 로딩해 N+1을 막는다 (두 서비스 공용)
@Component
class FoodCategoryReader(
    private val foodCategoryMapRepository: FoodCategoryMapRepository,
) {

    /**
     * 음식 1건당 대표 카테고리 code 1개를 반환한다 (분류가 없는 음식은 map에서 제외).
     *
     * 데이터상 한 음식이 여러 분류를 가질 수 있으나 화면 노출은 단일 — 가장 낮은 sortOrder를 대표로 본다.
     * 쿼리가 sortOrder asc로 정렬하므로 그룹의 첫 항목이 대표다.
     */
    fun loadPrimaryByFoodIds(foodIds: List<Long>): Map<Long, String> {
        if (foodIds.isEmpty()) return emptyMap()
        return foodCategoryMapRepository.findCategoryViewsByFoodIds(foodIds)
            .groupBy { it.foodId }
            .mapValues { (_, views) -> views.first().code }
    }
}
