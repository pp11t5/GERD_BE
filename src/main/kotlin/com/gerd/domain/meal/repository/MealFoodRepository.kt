package com.gerd.domain.meal.repository

import com.gerd.domain.meal.entity.MealFood
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MealFoodRepository : JpaRepository<MealFood, Long> {

    fun findByExternalIdAndUser_Id(externalId: UUID, userId: Long): MealFood?

    fun findByMealRecordIdOrderByEatenAtAsc(mealRecordId: Long): List<MealFood>

    fun findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(mealRecordIds: List<Long>): List<MealFood>

    fun countByMealRecordId(mealRecordId: Long): Long

}
