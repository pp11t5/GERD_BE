package com.gerd.domain.symptom.repository

import com.gerd.domain.symptom.entity.Symptom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface SymptomRepository : JpaRepository<Symptom, Long> , SymptomPatternQueryRepository {
    fun findByMealRecordId(mealRecordId: Long): List<Symptom>
    fun findByMealRecordIdIn(mealRecordIds: List<Long>): List<Symptom>
    fun findByUser_IdAndOccurredAtBetween(userId: Long, start: LocalDateTime, end: LocalDateTime): List<Symptom>

    fun findByExternalIdAndUser_Id(externalId: UUID, userId: Long): Symptom?

    // 연결된 식사 기록 찾기
    @Query("SELECT s.mealRecordId FROM Symptom s WHERE s.user.id = :userId AND s.mealRecordId IS NOT NULL")
    fun findLinkedMealRecordIdsByUserId(@Param("userId") userId: Long): List<Long>

}
