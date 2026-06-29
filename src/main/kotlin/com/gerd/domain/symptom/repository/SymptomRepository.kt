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

    @Query("""
        SELECT DISTINCT s
        FROM Symptom s
        WHERE s.user.id = :userId
          AND s.mealRecordId IN (
              SELECT mf.mealRecord.id
              FROM MealFood mf
              WHERE mf.user.id = :userId
                AND mf.foodId = :foodId
          )
        ORDER BY s.occurredAt DESC, s.id DESC
    """)
    fun findLinkedSymptomsByUserIdAndFoodId(
        @Param("userId") userId: Long,
        @Param("foodId") foodId: Long,
    ): List<Symptom>

    @Query("""
        SELECT DISTINCT CAST(s.occurred_at AS date) AS record_date
        FROM symptom_records s
        WHERE s.user_id = :userId
          AND s.symptom_state = 'COMFORTABLE'
          AND s.deleted_at IS NULL
          AND CAST(s.occurred_at AS date) < :beforeDate
        ORDER BY record_date DESC
        LIMIT :limit
    """, nativeQuery = true)
    fun findComfortableRecordDatesBefore(
        @Param("userId") userId: Long,
        @Param("beforeDate") beforeDate: java.time.LocalDate,
        @Param("limit") limit: Int,
    ): List<java.time.LocalDate>

    @Query("SELECT new com.gerd.domain.report.dto.SymptomStateRow(CAST(s.occurredAt AS LocalDate), s.symptomState) FROM Symptom s WHERE s.user.id = :userId AND s.occurredAt BETWEEN :start AND :end")
    fun findStatesByUserAndPeriod(
        @Param("userId") userId: Long,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<com.gerd.domain.report.dto.SymptomStateRow>

    // 연결된 식사 기록 찾기
    @Query("SELECT s.mealRecordId FROM Symptom s WHERE s.user.id = :userId AND s.mealRecordId IS NOT NULL")
    fun findLinkedMealRecordIdsByUserId(@Param("userId") userId: Long): List<Long>

    // 특정 끼니를 제외한 다른 편안 증상에서 여전히 유효한 음식 ID 목록
    // symptom_state는 @Enumerated(STRING)이라 enum 이름 대문자로 조회
    @Query("""
        SELECT DISTINCT mf.food_id
        FROM symptom_records s
        INNER JOIN meal_foods mf ON s.meal_record_id = mf.meal_record_id
        WHERE s.user_id = :userId
          AND s.symptom_state IN ('COMFORTABLE', 'GOOD')
          AND s.deleted_at IS NULL
          AND mf.deleted_at IS NULL
          AND mf.food_id IN (:foodIds)
          AND s.meal_record_id <> :excludeMealRecordId
    """, nativeQuery = true)
    fun findFoodIdsStillSafeByOtherSymptoms(
        @Param("userId") userId: Long,
        @Param("foodIds") foodIds: List<Long>,
        @Param("excludeMealRecordId") excludeMealRecordId: Long,
    ): List<Long>
}
