package com.gerd.domain.meal.repository

import com.gerd.domain.meal.entity.MealRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.UUID

interface MealRecordRepository : JpaRepository<MealRecord, Long> {

    // 단건 조회/수정/삭제 — @SQLRestriction이 삭제 row를 거르므로 결과 없음 = MEAL404_1
    fun findByExternalIdAndUser_Id(externalId: UUID, userId: Long): MealRecord?

    // "같이 먹은 음식" 추가 시 끼니 존재·본인 소유 검증 (삭제 row 제외)
    fun existsByUser_IdAndMealGroupId(userId: Long, mealGroupId: UUID): Boolean

    // 타임라인 1일치 — 기록 단위 범위 스캔, eatenAt asc + id tie-breaker로 끼니 그룹핑 순서 보장
    @Query(
        """
        select m from MealRecord m
        where m.user.id = :userId and m.eatenAt >= :from and m.eatenAt < :to
        order by m.eatenAt asc, m.id asc
        """,
    )
    fun findDailyRecords(userId: Long, from: LocalDateTime, to: LocalDateTime): List<MealRecord>
}
