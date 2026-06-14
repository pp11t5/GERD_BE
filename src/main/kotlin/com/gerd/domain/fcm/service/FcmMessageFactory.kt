package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.stereotype.Component

/**
 * platform별 FCM 메시지 빌더
 * - ANDROID: AndroidConfig (priority=HIGH)
 * - IOS: ApnsConfig (sound=default, apns-priority=10)
 * - 공통: Notification(title, body) + data(type, targetId)
 */
@Component
class FcmMessageFactory {

    // 플랫폼에 따른 분기
    fun build(token: String, platform: DevicePlatform, payload: FcmPayload): Message =
        when (platform) {
            DevicePlatform.ANDROID -> buildAndroid(token, payload)
            DevicePlatform.IOS -> buildIos(token, payload)
        }

    private fun baseBuilder(payload: FcmPayload): Message.Builder =
        Message.builder()
            .setNotification(
                Notification.builder()
                    .setTitle(payload.title)
                    .setBody(payload.body)
                    .build()
            )
            .putAllData(payload.toDataMap())

    // Android
    private fun buildAndroid(token: String, payload: FcmPayload): Message =
        baseBuilder(payload)
            .setToken(token)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build()
            )
            .build()

    // iOS: APNs 즉시 전송 + 기본 사운드
    private fun buildIos(token: String, payload: FcmPayload): Message =
        baseBuilder(payload)
            .setToken(token)
            .setApnsConfig(
                ApnsConfig.builder()
                    .putHeader("apns-priority", "10")
                    .setAps(Aps.builder().setSound("default").build())
                    .build()
            )
            .build()
}
