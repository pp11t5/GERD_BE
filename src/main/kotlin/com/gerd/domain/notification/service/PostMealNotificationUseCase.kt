package com.gerd.domain.notification.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.notification.entity.NotificationPending
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.PENDING
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.SENT
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.domain.notification.repository.NotificationPendingRepository
import com.gerd.global.config.properties.NotificationProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

/**
 * 식후 증상 기록 알림
 * - 예약: 리스너가 enqueue 호출 (쿨다운·야간 판정)
 * - 발송: 매분 크론이 processPending → 유저별로 PostMealPendingSender에 위임(별도 트랜잭션·스레드)
 */
@Service
@Transactional(readOnly = true)
class PostMealNotificationUseCase(
    private val notificationPendingRepository: NotificationPendingRepository,
    private val postMealPendingSender: PostMealPendingSender,
    private val userRepository: UserRepository,
    private val mealRecordRepository: MealRecordRepository,
    private val notificationProperties: NotificationProperties,
) {

    // 리스너에서 호출 — 식후 알림 예약
    // @Async 리스너라 예외가 응답에 영향 없이 스레드에서 유실되므로, 존재하지 않는 유저는 예외 대신 로그로 스킵
    @Transactional
    fun enqueue(userId: Long, mealRecordId: Long) {
        val now = LocalDateTime.now()
        val user = userRepository.findByIdOrNull(userId) ?: run {
            log.warn { "식후 알림 예약 스킵 — 존재하지 않는 유저 userId=$userId" }
            return
        }

        // AFTER_COMMIT + @Async라 커밋과 예약 사이에 끼니가 삭제될 수 있다 — cancelByMealRecordId가
        // 이 예약보다 먼저 실행돼도 놓치지 않도록, 삭제된 끼니면 예약 자체를 만들지 않는다
        if (!mealRecordRepository.existsById(mealRecordId)) {
            log.warn { "식후 알림 예약 스킵 — 이미 삭제된 끼니 mealRecordId=$mealRecordId" }
            return
        }

        // 1) 식후 지연 시각이 무발송 구간에 걸리면 설정된 이연 시각으로 예약
        val (scheduledAt, delayed) = resolveSchedule(now.plus(notificationProperties.delay))

        // 2) 쿨다운은 즉시 발송분에만 적용 — 이연분은 설정된 시각의 묶음 발송 규칙이라 제외
        // 쿨다운 기간 내 PENDING/SENT 이력 존재 시 return — CANCELLED는 쿨다운에서 제외
        if (!delayed &&
            notificationPendingRepository.existsByUserIdAndTypeAndDelayedFalseAndStatusInAndCreatedAtAfter(
                userId,
                NotificationType.POST_MEAL,
                COOLDOWN_STATUSES,
                now.minus(notificationProperties.cooldown),
            )
        ) return

        // 3) NotificationPending 저장
        notificationPendingRepository.save(
            NotificationPending(
                user = user,
                type = NotificationType.POST_MEAL,
                mealRecordId = mealRecordId,
                scheduledAt = scheduledAt,
                delayed = delayed,
            )
        )

    }

    // 기준시각이 무발송 구간이면 가장 가까운 이연 시각으로 예약
    private fun resolveSchedule(base: LocalDateTime): Pair<LocalDateTime, Boolean> {
        val t = base.toLocalTime()
        val quietHoursStart = notificationProperties.quietHoursStart
        val deferredTime = notificationProperties.deferredTime
        val inQuietHours = if (quietHoursStart < deferredTime) {
            t >= quietHoursStart && t < deferredTime
        } else {
            t >= quietHoursStart || t < deferredTime
        }
        if (!inQuietHours) return base to false

        val date = if (t < deferredTime) base.toLocalDate() else base.toLocalDate().plusDays(1)
        return date.atTime(deferredTime) to true
    }

    // 기록 삭제 시 PENDING된 알림 취소 (delayed 여부 무관 — 해당 식사의 미발송분 전체)
    @Transactional
    fun cancelByMealRecordId(userId: Long, mealRecordId: Long) {
        notificationPendingRepository
            .findByMealRecordIdAndUserIdAndStatus(mealRecordId, userId, PENDING)
            .forEach { it.cancel() }
        // managed 엔티티라 트랜잭션 커밋 시 status=CANCELLED가 자동 flush
    }

    // 크론에서 호출 — due PENDING을 유저별로 묶어 비동기 발송에 위임
    fun processPending() {
        val pendings = notificationPendingRepository.findDueForActiveUsers(PENDING, LocalDateTime.now())
        if (pendings.isEmpty()) return
        log.info { "식후 알림 처리 시작: 대상 ${pendings.size}건" }
        pendings.groupBy { it.user.id!! }.forEach { (userId, userPendings) ->
            postMealPendingSender.sendForUser(userId, userPendings.mapNotNull { it.id })
        }
    }

    companion object {
        private val COOLDOWN_STATUSES = setOf(PENDING, SENT)
    }
}
