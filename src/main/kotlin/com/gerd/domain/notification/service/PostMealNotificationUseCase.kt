package com.gerd.domain.notification.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.notification.entity.NotificationPending
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.PENDING
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.domain.notification.repository.NotificationPendingRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
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
    private val userRepository: UserRepository,
    private val mealRecordRepository: MealRecordRepository,
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

        // 1) 야간 판정 먼저 — 식후 2h가 야간(22:00~09:00)에 걸리면 다음 09:00로 이연
        val (scheduledAt, delayed) = resolveSchedule(now.plusHours(POST_MEAL_DELAY_HOURS))

        // 2) 쿨다운은 '낮 즉시 발송분'에만 — 야간 이연분은 09:00 묶음 규칙이라 제외
        // 90분 내 이력 존재 시 return, 발송하지않음
        if (!delayed &&
            notificationPendingRepository.existsByUserIdAndTypeAndDelayedFalseAndCreatedAtAfter(
                userId, NotificationType.POST_MEAL, now.minusMinutes(COOLDOWN_MINUTES),
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

    // 기준시각이 야간이면 다음 09:00로 이연
    private fun resolveSchedule(base: LocalDateTime): Pair<LocalDateTime, Boolean> {
        val t = base.toLocalTime()
        val inQuietHours = t >= QUIET_HOUR_START || t < DEFERRED_HOUR
        if (!inQuietHours) return base to false

        // 22시 이후면 다음날 09:00, 새벽(00~09시)이면 당일 09:00
        val date = if (t >= QUIET_HOUR_START) base.toLocalDate().plusDays(1) else base.toLocalDate()
        return date.atTime(DEFERRED_HOUR) to true
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
        private const val POST_MEAL_DELAY_HOURS = 2L      // 식후 발송 지연
        private const val COOLDOWN_MINUTES = 90L          // 낮 즉시 발송 최소 간격
        private val QUIET_HOUR_START: LocalTime = LocalTime.of(22, 0)  // 야간 무발송 시작
        private val DEFERRED_HOUR: LocalTime = LocalTime.of(9, 0)      // 이연 발송 시각
    }
}
