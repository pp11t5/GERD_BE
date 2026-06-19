package com.gerd.domain.meal.repository

import com.gerd.domain.meal.entity.MealRecord
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface MealRecordRepository : JpaRepository<MealRecord, Long> {
    fun existsByExternalIdAndUserId(externalId: UUID, userId: Long): Boolean
    fun findByExternalIdAndUserId(externalId: UUID, userId: Long): MealRecord?
    fun findByIdAndUserId(id: Long, userId: Long): MealRecord?
    fun findByUserIdAndEatenAtAfter(userId: Long, cutoff: LocalDateTime): List<MealRecord>
}
