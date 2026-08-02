package com.gerd.domain.notification.service

import com.gerd.domain.fcm.exception.FcmErrorCode
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.domain.fcm.service.FcmPushSender
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service

/**
 * 관리자용 테스트 발송 — 실제 알림과 동일한 문구를 특정 유저에게 즉시 발송 (prod 미노출, AdminNotificationTestController 참고)
 */
@Service
class AdminNotificationTestService(
    private val userFcmTokenRepository: UserFcmTokenRepository,
    private val fcmPushSender: FcmPushSender,
) {

    fun send(userId: Long, type: NotificationType, targetId: String?) {
        if (!userFcmTokenRepository.existsById(userId)) {
            throw GeneralException(FcmErrorCode.FCM_TOKEN_NOT_FOUND)
        }
        fcmPushSender.sendToUser(userId, NotificationPayloadFactory.of(type, targetId))
    }
}
