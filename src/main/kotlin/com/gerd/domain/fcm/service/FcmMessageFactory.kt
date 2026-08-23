package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.ApsAlert
import com.google.firebase.messaging.Message
import org.springframework.stereotype.Component

/**
 * platform별 FCM 메시지 빌더
 * - 공통: notification 블록 없이 title/body와 리치 푸시 데이터를 data로 전달
 * - ANDROID: AndroidConfig (priority=HIGH)
 * - IOS: APNs alert(payload title/body, category)로 시스템 알림과 액션을 표시
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

    // iOS: APNs alert — category는 앱의 UNNotificationCategory 식별자와 일치해야 한다.
    private fun buildIos(token: String, payload: FcmPayload): Message =
        baseBuilder(payload)
            .setToken(token)
            .setApnsConfig(
                ApnsConfig.builder()
                    .putHeader("apns-push-type", "alert")
                    .putHeader("apns-priority", "10")
                    .setAps(
                        Aps.builder()
                            .setAlert(
                                ApsAlert.builder()
                                    .setTitle(payload.title)
                                    .setBody(payload.body)
                                    .build()
                            )
                            .setCategory(payload.type.code)
                            .build()
                    )
                    .build()
            )
            .build()
}
