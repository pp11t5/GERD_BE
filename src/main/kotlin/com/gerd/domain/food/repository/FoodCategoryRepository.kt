package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.FoodCategory
import org.springframework.data.jpa.repository.JpaRepository

interface FoodCategoryRepository : JpaRepository<FoodCategory, Long> {
    fun findAllByOrderBySortOrderAsc(): List<FoodCategory>
}
