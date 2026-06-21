package com.gerd.domain.meal.repository

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.entity.MealFood
import com.querydsl.core.types.Ops
import jdk.incubator.vector.VectorOperators.AND
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface MealFoodRepository : JpaRepository<MealFood, Long> {

    fun findByExternalIdAndUser_Id(externalId: UUID, userId: Long): MealFood?

    fun findByMealRecordIdOrderByEatenAtAsc(mealRecordId: Long): List<MealFood>

    fun findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(mealRecordIds: List<Long>): List<MealFood>

    fun countByMealRecordId(mealRecordId: Long): Long

    fun findByUser_IdAndEatenAtBetween(userId: Long, start: LocalDateTime, end: LocalDateTime): List<MealFood>
}
