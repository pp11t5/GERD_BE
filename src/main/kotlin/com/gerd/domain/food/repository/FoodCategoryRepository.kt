package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.FoodCategory
import org.springframework.data.jpa.repository.JpaRepository

interface FoodCategoryRepository : JpaRepository<FoodCategory, Long> {
    fun findAllByOrderBySortOrderAsc(): List<FoodCategory>

    // LLM 분류 결과(code)를 실제 카테고리 엔티티로 변환
    fun findByCode(code: String): FoodCategory?
}
