package com.gerd.domain.notification.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.domain.fcm.service.FcmPushSender
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.domain.notification.service.NotificationBatchPolicy.FCM_MULTICAST_BATCH_SIZE
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * 주간 리포트 알림
 * - 메커니즘: 커서 페이징(500건) → 멀티캐스트
 */
@Service
class WeeklyReportNotificationUseCase(
    private val fcmPushSender: FcmPushSender,
    private val userFcmTokenRepository: UserFcmTokenRepository,
) {

    fun send() {
        log.info { "주간 리포트 발송 시작" }
        var cursor = 0L
        val pageable = PageRequest.of(0, FCM_MULTICAST_BATCH_SIZE)
        var totalSent = 0

        do {
            val slice = userFcmTokenRepository.findByWeeklyReportEnabled(cursor, pageable)
            val tokens = slice.content.map { it.token }
            if (tokens.isNotEmpty()) {
                fcmPushSender.sendMulticast(tokens, PAYLOAD)
                totalSent += tokens.size
                cursor = slice.content.last().userId!!
            }
        } while (slice.hasNext())

        log.info { "주간 리포트 발송 완료: 총 ${totalSent}건" }
    }

    companion object {
        private val PAYLOAD = FcmPayload(
            title = "이번 주 리포트가 준비됐어요",
            body = "불편했던 음식과 신호등 분포를 확인해 보세요.",
            type = NotificationType.WEEKLY_REPORT,
        )
    }
}
