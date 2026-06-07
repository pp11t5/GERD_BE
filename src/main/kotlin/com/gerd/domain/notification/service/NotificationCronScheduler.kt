package com.gerd.domain.notification.service

import com.gerd.domain.fcm.FcmPushSender
import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.notification.entity.enums.DailyNotificationTime
import com.gerd.domain.notification.entity.enums.NotificationType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * 정기 푸시 발송 스케줄러
 * - 매일 기록 푸시: 시간대별 활성 유저 sendAll (DB 직접 조회)
 * - 주간 리포트: FCM 토픽 발송 — Firebase fan-out으로 월요일 스파이크 분산
 * - notification.scheduler.enabled=false 시 비활성화 (stage 환경 등)
 */
@Component
@ConditionalOnProperty(name = ["notification.scheduler.enabled"], havingValue = "true", matchIfMissing = false)
class NotificationCronScheduler(
    private val fcmPushSender: FcmPushSender,
) {

    // 매일 기록 푸시 — 시간대별 Cron
    @Scheduled(cron = "0 0 8 * * *")
    fun sendDailyRecordMorning8() = sendDailyRecord(DailyNotificationTime.MORNING_8)

    @Scheduled(cron = "0 0 20 * * *")
    fun sendDailyRecordEvening8() = sendDailyRecord(DailyNotificationTime.EVENING_8)

    @Scheduled(cron = "0 0 21 * * *")
    fun sendDailyRecordNight9() = sendDailyRecord(DailyNotificationTime.NIGHT_9)

    @Scheduled(cron = "0 0 22 * * *")
    fun sendDailyRecordNight10() = sendDailyRecord(DailyNotificationTime.NIGHT_10)

    // 주간 리포트 — 매주 월요일 09:00, 토픽 발송
    @Scheduled(cron = "0 0 9 * * MON")
    fun sendWeeklyReport() {
        fcmPushSender.sendToTopic(
            TOPIC_WEEKLY_REPORT,
            FcmPayload(
                title = "이번 주 리포트가 도착했어요",
                body = "한 주간의 식사·증상 패턴을 확인해보세요.",
                type = NotificationType.WEEKLY_REPORT,
            )
        )
    }

    // 해당 시간대 알림 활성 유저 토큰 조회 후 500개 단위 배치 발송
    private fun sendDailyRecord(time: DailyNotificationTime) {
        log.info { "일일 기록 푸시 스케줄러는 아직 구현되지 않았습니다. time=$time" }
    }

    companion object {
        const val TOPIC_WEEKLY_REPORT = "weekly-report"
    }
}
