package com.gerd.domain.meal.repository

import com.gerd.domain.meal.entity.MealRecord
import com.gerd.domain.report.dto.MealGradeRow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface MealRecordRepository : JpaRepository<MealRecord, Long> {
    fun findByExternalIdAndUser_Id(externalId: UUID, userId: Long): MealRecord?
    fun findByIdAndUser_Id(id: Long, userId: Long): MealRecord?
    fun findByUser_IdAndEatenAtAfter(userId: Long, cutoff: LocalDateTime): List<MealRecord>
    fun findByUser_IdAndEatenAtBetween(userId: Long, start: LocalDateTime, end: LocalDateTime): List<MealRecord>

    @Query("SELECT new com.gerd.domain.report.dto.MealGradeRow(CAST(m.eatenAt AS LocalDate), m.grade) FROM MealRecord m WHERE m.user.id = :userId AND m.eatenAt BETWEEN :start AND :end")
    fun findGradesByUserAndPeriod(
        @Param("userId") userId: Long,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<MealGradeRow>
}
