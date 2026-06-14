package com.gerd.domain.notification.entity

import com.gerd.domain.auth.entity.User
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDateTime

/**
 * 유저별로 발송 대기 중인 알림을 저장하는 테이블
 * - 발송 시각이 된 알림을 스케줄러가 조회하여 실제 발송 처리
 * - 09:00 묶음 발송 시, 해당 시각에 발송 대기 중인 알림이 있는지 조회하여 묶음 발송 여부 판단
 * - 발송 후에는 상태를 SENT로 업데이트하여 재발송 방지
 */
@Entity
@Table(
    name = "notification_pending",
    indexes = [
        // 스케줄러: PENDING 중 발송 시각 도달한 것 조회
        Index(name = "idx_notification_pending_status_scheduled_at", columnList = "status, scheduled_at"),
        // 09:00 묶음 발송 시 유저별 PENDING 건수 집계
        Index(name = "idx_notification_pending_user_status_scheduled_at", columnList = "user_id, status, scheduled_at"),
    ]
)
class NotificationPending(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val type: NotificationType,

    // 연결된 식사 기록 ID — 묶음 발송 시 미기록 목록 딥링크에 사용
    // 식사 기록이 없는 알림도 있을 수 있으므로 nullable 허용
    // 식사 기록과 연결되어 있을 경우, cascade ondelete 처리 필요
    // 식사 기록 완료 시 반영
    @Column(name = "meal_record_id")
    val mealRecordId: Long? = null,

    @Column(name = "scheduled_at", nullable = false)
    val scheduledAt: LocalDateTime,

    // Quiet Hours로 인해 다음날 09:00으로 이연된 알림 여부
    @Column(name = "delayed", nullable = false)
    val delayed: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: NotificationPendingStatus = NotificationPendingStatus.PENDING,

    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    val id: Long? = null,

) : BaseTimeEntity() {

    fun markSent() {
        status = NotificationPendingStatus.SENT
    }

    fun cancel() {
        status = NotificationPendingStatus.CANCELLED
    }
}
