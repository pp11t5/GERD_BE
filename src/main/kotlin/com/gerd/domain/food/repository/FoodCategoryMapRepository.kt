package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.FoodCategoryMap
import com.gerd.domain.food.entity.id.FoodCategoryMapId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FoodCategoryMapRepository : JpaRepository<FoodCategoryMap, FoodCategoryMapId> {

    /**
     * 여러 음식의 카테고리를 한 번에 로딩 (검색/최근 응답 조립 시 N+1 회피)
     *
     * foodId별로 그룹핑은 서비스에서 수행. sortOrder 순으로 정렬해 표시 순서를 보장한다.
     */
    @Query(
        """
        select m.food.id as foodId, c.code as code, c.displayName as displayName
        from FoodCategoryMap m
        join m.foodCategory c
        where m.food.id in :foodIds
        order by c.sortOrder asc
        """,
    )
    fun findCategoryViewsByFoodIds(foodIds: Collection<Long>): List<FoodCategoryView>
}

// 음식↔카테고리 평면 프로젝션 (foodId 기준 그룹핑용)
interface FoodCategoryView {
    val foodId: Long
    val code: String
    val displayName: String
}
