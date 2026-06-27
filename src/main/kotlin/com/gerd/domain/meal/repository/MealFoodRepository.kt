package com.gerd.domain.meal.repository

import com.gerd.domain.meal.entity.MealFood
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

import java.util.UUID

interface MealFoodRepository : JpaRepository<MealFood, Long> {

    fun findByExternalIdAndUser_Id(externalId: UUID, userId: Long): MealFood?

    // mealRecord 연관관계의 id로 조회 — @SQLRestriction(deleted_at IS NULL)은 엔티티 쿼리에 자동 적용된다
    @Query("select mf from MealFood mf where mf.mealRecord.id = :mealRecordId order by mf.eatenAt asc")
    fun findByMealRecordIdOrderByEatenAtAsc(@Param("mealRecordId") mealRecordId: Long): List<MealFood>

    @Query(
        "select mf from MealFood mf where mf.mealRecord.id in :mealRecordIds " +
            "order by mf.mealRecord.id asc, mf.eatenAt asc",
    )
    fun findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(@Param("mealRecordIds") mealRecordIds: List<Long>): List<MealFood>

    @Query("select count(mf) from MealFood mf where mf.mealRecord.id = :mealRecordId")
    fun countByMealRecordId(@Param("mealRecordId") mealRecordId: Long): Long

    fun findByUser_IdAndEatenAtBetween(userId: Long, start: LocalDateTime, end: LocalDateTime): List<MealFood>
}
