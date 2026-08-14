package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.Message
import org.springframework.stereotype.Component

/**
 * platform별 FCM 메시지 빌더 — data-only (notification 블록 없음, title/body도 data로 전달)
 * - 클라이언트가 항상 직접 알림을 빌드해야 액션 버튼(인터랙티브) 부착이 가능하기 때문
 * - ANDROID: AndroidConfig (priority=HIGH)
 * - IOS: ApnsConfig background push (apns-push-type=background, apns-priority=5, content-available=1)
 *   — Apple이 배터리/네트워크 상황에 따라 지연·스로틀링·드롭할 수 있음(전달 보장 없음)
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

    // iOS: silent(background) push — alert/sound/badge 없이 앱을 깨워 데이터만 전달
    private fun buildIos(token: String, payload: FcmPayload): Message =
        baseBuilder(payload)
            .setToken(token)
            .setApnsConfig(
                ApnsConfig.builder()
                    .putHeader("apns-push-type", "background")
                    .putHeader("apns-priority", "5")
                    .setAps(Aps.builder().setContentAvailable(true).build())
                    .build()
            )
            .build()
}
