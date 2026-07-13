package com.gerd.domain.notification.repository

import com.gerd.domain.notification.entity.NotificationPending
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus
import com.gerd.domain.notification.entity.enums.NotificationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface NotificationPendingRepository : JpaRepository<NotificationPending, Long> {

    // 크론: 발송 시각 도달 + 활성 유저의 PENDING만 조회 (탈퇴 유예 유저 명시 제외)
    // @SQLRestriction은 to-one JOIN에 신뢰성 있 게 적용되지 않아 deletedAt 조건을 직접 건다
    @Query("""
        SELECT np FROM NotificationPending np
        JOIN FETCH np.user u
        WHERE np.status = :status
        AND np.scheduledAt <= :now
        AND u.deletedAt IS NULL
    """)
    fun findDueForActiveUsers(
        @Param("status") status: NotificationPendingStatus,
        @Param("now") now: LocalDateTime,
    ): List<NotificationPending>

    // 비동기 발송: 디스패치된 ID 중 아직 PENDING인 것만 재조회 (managed 엔티티 + 중복 발송 방지)
    fun findByIdInAndStatus(
        ids: List<Long>,
        status: NotificationPendingStatus,
    ): List<NotificationPending>

    // 쿨다운 판정: 유저의 특정 타입 '낮 즉시 발송분'(delayed=false)이 기준시각 이후 등록된 이력이 있는지
    // 야간 이연분(delayed=true)은 09:00 묶음 규칙이라 쿨다운 계산에서 제외
    fun existsByUserIdAndTypeAndDelayedFalseAndCreatedAtAfter(
        userId: Long,
        type: NotificationType,
        createdAt: LocalDateTime,
    ): Boolean

    // 식사 기록 삭제 시 취소: 해당 유저·식사기록의 특정 상태 알림 조회 (userId 스코프로 타 유저 차단)
    fun findByMealRecordIdAndUserIdAndStatus(
        mealRecordId: Long,
        userId: Long,
        status: NotificationPendingStatus,
    ): List<NotificationPending>
}
