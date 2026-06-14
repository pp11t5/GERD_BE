package com.gerd.domain.notification.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.domain.fcm.service.FcmPushSender
import com.gerd.domain.notification.entity.enums.DailyNotificationTime
import com.gerd.domain.notification.entity.enums.NotificationType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * 매일 밤 기록 작성 알림
 * - 메커니즘: 시간대별 커서 페이징(500건) → sendMulticast → 실패 토큰 정리
 */
@Service
class DailyRecordNotificationUseCase(
    private val userFcmTokenRepository: UserFcmTokenRepository,
    private val fcmPushSender: FcmPushSender,
) {

    fun send(time: DailyNotificationTime) {
        log.info { "매일 기록 알림 발송 시작: time=$time" }
        var cursor = 0L
        val pageable = PageRequest.of(0, BATCH_SIZE)
        var totalSent = 0

        do {
            val slice = userFcmTokenRepository
                .findByDailyRecordNotificationEnabledAndDailyNotificationTime(time, cursor, pageable)

            val tokens = slice.content.map { it.token }
            if (tokens.isNotEmpty()) {
                fcmPushSender.sendMulticast(tokens, PAYLOAD)
                totalSent += tokens.size
                cursor = slice.content.last().userId!!
            }
        } while (slice.hasNext())

        log.info { "매일 기록 알림 발송 완료: time=$time, 총 ${totalSent}건" }
    }

    companion object {
        private const val BATCH_SIZE = 500
        private val PAYLOAD = FcmPayload(
            title = "오늘 식사 기록을 남겨보세요",
            body = "오늘 하루 드신 식사를 기록하면 증상 패턴을 파악할 수 있어요.",
            type = NotificationType.DAILY_RECORD,
        )
    }
}
