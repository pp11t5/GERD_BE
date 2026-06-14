package com.gerd.domain.notification.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.domain.fcm.service.FcmPushSender
import com.gerd.domain.notification.entity.enums.NotificationType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * 주간 리포트 알림
 * - 메커니즘: 멀티캐스트 (weeklyReportEnabled=true 유저 토큰 직접 조회)
 */
@Service
class WeeklyReportNotificationUseCase(
    private val fcmPushSender: FcmPushSender,
    private val userFcmTokenRepository: UserFcmTokenRepository,
) {

    fun send() {
        val tokens = userFcmTokenRepository.findTokensForWeeklyReport()
        log.info { "주간 리포트 발송 시작: 대상 ${tokens.size}명" }
        fcmPushSender.sendMulticast(
            tokens,
            FcmPayload(
                title = "이번 주 리포트가 준비됐어요",
                body = "불편했던 음식과 신호등 분포를 확인해 보세요.",
                type = NotificationType.WEEKLY_REPORT,
            ),
        )
    }
}
