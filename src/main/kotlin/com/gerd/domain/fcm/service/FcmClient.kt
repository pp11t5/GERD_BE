package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.UserFcmToken
import com.gerd.domain.fcm.exception.FcmErrorCode
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.global.apiPayload.GeneralException
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Firebase SDK를 직접 호출하는 인프라 구현체 (prod 전용)
 * - FcmPushSender: 개인·멀티캐스트 발송
 */
@Service
@Profile("prod")
class FcmClient(
    private val fcmMessageFactory: FcmMessageFactory,
    private val fcmTokenService: FcmTokenService,
    private val fcmTokenRepository: UserFcmTokenRepository,
    private val firebaseMessaging: FirebaseMessaging,
) : FcmPushSender {

    // 유저 토큰 조회 후 platform에 맞는 메시지 빌드 → 발송
    override fun sendToUser(userId: Long, payload: FcmPayload) {
        val fcmToken = fcmTokenRepository.findById(userId).orElse(null)
            ?: return  // 토큰 미등록 유저 — 조용히 스킵
        send(fcmToken, payload)
    }

    // 토큰 엔티티로 바로 발송 — 재조회 없이 (배치에서 토큰 1회 조회 후 재사용)
    override fun send(fcmToken: UserFcmToken, payload: FcmPayload) {
        val message = fcmMessageFactory.build(fcmToken.token, fcmToken.platform, payload)
        send(message, fcmToken.token)
    }

    // 단일 토큰 직접 발송 — 테스트용
    override fun sendRaw(token: String, payload: FcmPayload) {
        val fcmToken = fcmTokenRepository.findByToken(token)
            ?: throw GeneralException(FcmErrorCode.FCM_TOKEN_NOT_FOUND)

        val message = fcmMessageFactory.build(fcmToken.token, fcmToken.platform, payload)
        try {
            firebaseMessaging.send(message)
        } catch (e: FirebaseMessagingException) {
            log.error(e) { "FCM 단일 토큰 발송 실패: errorCode=${e.messagingErrorCode}" }
            throw GeneralException(FcmErrorCode.FCM_SEND_FAILED)
        }
    }

    // 멀티캐스트 — 500개씩 배치 발송
    override fun sendMulticast(tokens: List<String>, payload: FcmPayload) {
        if (tokens.isEmpty()) return
        tokens.chunked(500).forEach { batch ->
            val message = MulticastMessage.builder()
                .addAllTokens(batch)
                .setNotification(
                    Notification.builder()
                        .setTitle(payload.title)
                        .setBody(payload.body)
                        .build()
                )
                .putAllData(payload.toDataMap())
                .build()
            try {
                val result = firebaseMessaging.sendEachForMulticast(message)
                log.info { "멀티캐스트 발송: 성공=${result.successCount}, 실패=${result.failureCount}" }
                // 만료·무효 토큰 정리
                result.responses.forEachIndexed { i, response ->
                    if (!response.isSuccessful &&
                        response.exception?.messagingErrorCode in INVALID_TOKEN_CODES
                    ) {
                        fcmTokenService.deleteByToken(batch[i])
                    }
                }
            } catch (e: FirebaseMessagingException) {
                log.warn { "멀티캐스트 발송 실패: errorCode=${e.messagingErrorCode}" }
            }
        }
    }

    // 발송 실패 시 만료 토큰 정리
    private fun send(message: Message, token: String) {
        try {
            firebaseMessaging.send(message)
        } catch (e: FirebaseMessagingException) {
            if (e.messagingErrorCode in INVALID_TOKEN_CODES) {
                fcmTokenService.deleteByToken(token)
            }
            log.warn { "FCM 발송 실패: errorCode=${e.messagingErrorCode}" }
        }
    }

    companion object {
        private val INVALID_TOKEN_CODES = setOf(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT,
        )
    }
}
