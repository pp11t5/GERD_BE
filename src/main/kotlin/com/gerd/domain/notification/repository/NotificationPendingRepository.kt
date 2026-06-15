package com.gerd.domain.notification.repository

import com.gerd.domain.notification.entity.NotificationPending
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface NotificationPendingRepository : JpaRepository<NotificationPending, Long> {

    // 스케줄러: 발송 시각이 된 PENDING 전체 조회
    fun findByStatusAndScheduledAtLessThanEqual(
        status: NotificationPendingStatus,
        scheduledAt: LocalDateTime,
    ): List<NotificationPending>

    // 크론: 발송 시각 도달 + 활성 유저의 PENDING만 조회 (탈퇴 유예 유저 명시 제외)
    // @SQLRestriction은 to-one JOIN에 신뢰성 있게 적용되지 않아 deletedAt 조건을 직접 건다
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

    // 09:00 묶음 감지: 특정 유저의 특정 시각 PENDING 건수
    fun countByUserIdAndStatusAndScheduledAt(
        userId: Long,
        status: NotificationPendingStatus,
        scheduledAt: LocalDateTime,
    ): Long

    // 09:00 묶음 발송 시 해당 유저의 PENDING 전체 취소 대상 조회
    fun findByUserIdAndStatusAndScheduledAt(
        userId: Long,
        status: NotificationPendingStatus,
        scheduledAt: LocalDateTime,
    ): List<NotificationPending>
}
