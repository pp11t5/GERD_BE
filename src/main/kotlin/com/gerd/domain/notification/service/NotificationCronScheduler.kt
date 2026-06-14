package com.gerd.domain.notification.service

import com.gerd.domain.notification.entity.enums.DailyNotificationTime
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 정기 푸시 발송 스케줄러 — 트리거만 담당, 로직은 Facade에 위임
 * - 식후 알림 PENDING 처리: 매분
 * - 매일 밤 기록 푸시: 시간대별
 * - 주간 리포트: 매주 일요일 19:00
 */
@Component
@ConditionalOnProperty(name = ["notification.scheduler.enabled"], havingValue = "true", matchIfMissing = false)
class NotificationCronScheduler(
    private val notificationFacade: NotificationFacade,
) {

    // 식후 알림 PENDING 처리 — 매분 실행
    @Scheduled(cron = "0 * * * * *")
    fun processPostMealPending() = notificationFacade.processPostMealPending()

    // 매일 밤 기록 푸시 — 시간대별 Cron
    @Scheduled(cron = "0 0 8 * * *")
    fun sendDailyRecordMorning8() = notificationFacade.sendDailyRecord(DailyNotificationTime.MORNING_8)

    @Scheduled(cron = "0 0 20 * * *")
    fun sendDailyRecordEvening8() = notificationFacade.sendDailyRecord(DailyNotificationTime.EVENING_8)

    @Scheduled(cron = "0 0 21 * * *")
    fun sendDailyRecordNight9() = notificationFacade.sendDailyRecord(DailyNotificationTime.NIGHT_9)

    @Scheduled(cron = "0 0 22 * * *")
    fun sendDailyRecordNight10() = notificationFacade.sendDailyRecord(DailyNotificationTime.NIGHT_10)

    // 주간 리포트 — 매주 일요일 19:00, 토픽 발송
    @Scheduled(cron = "0 0 19 * * SUN")
    fun sendWeeklyReport() = notificationFacade.sendWeeklyReport()
}
