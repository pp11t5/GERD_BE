package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.gerd.domain.notification.entity.enums.NotificationType
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.ApsAlert
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.stereotype.Component

/**
 * platform별 FCM 메시지 빌더
 * - POST_MEAL, POST_MEAL_DELAYED_SINGLE: data-only + iOS APNs alert/category
 * - 그 외 알림: FCM notification과 data를 함께 전송
 */
@Component
class FcmMessageFactory {

    // 플랫폼에 따른 분기
    fun build(token: String, platform: DevicePlatform, payload: FcmPayload): Message =
        when (platform) {
            DevicePlatform.ANDROID -> buildAndroid(token, payload)
            DevicePlatform.IOS -> buildIos(token, payload)
        }

    // 전송 요청 로그가 실제 플랫폼별 전달 계약을 그대로 보여주도록 구성
    fun deliveryMetadata(platform: DevicePlatform, payload: FcmPayload): Map<String, Any> = buildMap {
        put("platform", platform.name)
        put("dataOnly", isDataOnly(payload))
        if (platform == DevicePlatform.IOS && isDataOnly(payload)) {
            put(
                "apns",
                mapOf(
                    "pushType" to "alert",
                    "priority" to "10",
                    "category" to payload.type.code,
                    "alert" to true,
                ),
            )
        }
    }

    private fun baseBuilder(payload: FcmPayload): Message.Builder =
        Message.builder()
            .putAllData(payload.toDataMap())
            .apply {
                if (!isDataOnly(payload)) {
                    setNotification(Notification.builder().setTitle(payload.title).setBody(payload.body).build())
                }
            }

    private fun isDataOnly(payload: FcmPayload): Boolean =
        payload.type in DATA_ONLY_TYPES

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

    // 식후 알림은 iOS의 커스텀 액션을 위해 APNs alert/category를 직접 구성한다.
    private fun buildIos(token: String, payload: FcmPayload): Message =
        baseBuilder(payload)
            .setToken(token)
            .apply {
                if (isDataOnly(payload)) {
                    setApnsConfig(
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
                }
            }
            .build()

    companion object {
        private val DATA_ONLY_TYPES = setOf(
            NotificationType.POST_MEAL,
            NotificationType.POST_MEAL_DELAYED_SINGLE,
        )
    }
}
