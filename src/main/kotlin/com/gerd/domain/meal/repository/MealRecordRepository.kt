package com.gerd.domain.meal.repository

import com.gerd.domain.meal.entity.MealFood
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MealFoodRepository : JpaRepository<MealFood, Long> {

    fun findByExternalIdAndUserId(externalId: UUID, userId: Long): MealFood?

    fun findByMealRecordIdOrderByEatenAtAsc(mealRecordId: UUID): List<MealFood>

    fun findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(mealRecordIds: List<UUID>): List<MealFood>

    fun countByMealRecordId(mealRecordId: UUID): Long

}
