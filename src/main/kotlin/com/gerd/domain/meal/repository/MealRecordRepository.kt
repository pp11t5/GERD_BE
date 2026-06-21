package com.gerd.domain.meal.repository

import com.gerd.domain.meal.entity.MealRecord
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface MealRecordRepository : JpaRepository<MealRecord, Long> {
    fun existsByExternalIdAndUser_Id(externalId: UUID, userId: Long): Boolean
    fun findByExternalIdAndUser_Id(externalId: UUID, userId: Long): MealRecord?
    fun findByIdAndUser_Id(id: Long, userId: Long): MealRecord?
    fun findByUser_IdAndEatenAtAfter(userId: Long, cutoff: LocalDateTime): List<MealRecord>
}
