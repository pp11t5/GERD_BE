package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.FoodSubstitute
import com.gerd.domain.food.entity.id.FoodSubstituteId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FoodSubstituteRepository : JpaRepository<FoodSubstitute, FoodSubstituteId> {

    // 대체식과 함께 로딩 — substituteFood의 @SQLRestriction이 join에도 적용되어 soft-deleted 대체식은 제외된다
    @Query(
        """
        select fs from FoodSubstitute fs
        join fetch fs.substituteFood
        where fs.id.foodId = :foodId
        order by fs.sortOrder asc
        """,
    )
    fun findByFoodIdOrderBySortOrder(foodId: Long): List<FoodSubstitute>
}
