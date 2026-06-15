package com.gerd.domain.notification.service

import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.PENDING
import com.gerd.domain.notification.repository.NotificationPendingRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime

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
) {

    // 리스너에서 호출 — 식후 알림 예약
    @Transactional
    fun enqueue(userId: Long, mealRecordId: Long) {
        val now = LocalDateTime.now()

        // 1) 야간 판정 먼저 — 식후 2h가 야간(22:00~09:00)에 걸리면 다음 09:00로 이연(delayed=true)
        val (scheduledAt, delayed) = resolveSchedule(now.plusHours(POST_MEAL_DELAY_HOURS))

        // 2) 쿨다운은 '낮 즉시 발송분'에만 — 야간 이연분은 09:00 묶음 규칙이라 제외
        // TODO: if (!delayed && 90분(COOLDOWN_MINUTES) 내 등록 이력 존재) return  (식사 기록 API 후 구현)

        // 3) NotificationPending save (type = POST_MEAL 고정, mealRecordId·scheduledAt·delayed)
    }

    // 기준시각이 야간이면 다음 09:00로 이연
    private fun resolveSchedule(base: LocalDateTime): Pair<LocalDateTime, Boolean> {
        val t = base.toLocalTime()
        val inQuietHours = t >= QUIET_HOUR_START || t < DEFERRED_HOUR
        if (!inQuietHours) return base to false

        // 22시 이후면 다음날 09:00, 새벽(00~09시)이면 당일 09:00
        val date = if (t >= QUIET_HOUR_START) base.toLocalDate().plusDays(1) else base.toLocalDate()
        return date.atTime(DEFERRED_HOUR) to true
    }

    // 기록 삭제 시 PENDING된 알림 취소
    @Transactional
    fun cancelByMealRecordId(userId: Long, mealRecordId: Long) {
        // TODO 구현
        // 1) mealRecordId로 PENDING 조회 (status=PENDING)
        // 2) 각 PENDING cancel() → status=CANCELLED
    }

    // 크론에서 호출 — due PENDING을 유저별로 묶어 비동기 발송에 위임
    fun processPending() {
        val pendings = notificationPendingRepository.findDueForActiveUsers(PENDING, LocalDateTime.now())
        log.info { "식후 알림 처리 시작: 대상 ${pendings.size}건" }
        pendings.groupBy { it.user.id!! }.forEach { (userId, userPendings) ->
            postMealPendingSender.sendForUser(userId, userPendings.mapNotNull { it.id })
        }
    }

    companion object {
        private const val POST_MEAL_DELAY_HOURS = 2L      // 식후 발송 지연
        private const val COOLDOWN_MINUTES = 90L          // 낮 즉시 발송 최소 간격
        private val QUIET_HOUR_START: LocalTime = LocalTime.of(22, 0)  // 야간 무발송 시작
        private val DEFERRED_HOUR: LocalTime = LocalTime.of(9, 0)      // 이연 발송 시각
    }
}
