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
 * - POST_MEAL_DELAYED_SINGLE: data-only + iOS APNs alert/category
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

    private fun baseBuilder(payload: FcmPayload): Message.Builder =
        Message.builder()
            .putAllData(payload.toDataMap())
            .apply {
                if (!isDataOnly(payload)) {
                    setNotification(Notification.builder().setTitle(payload.title).setBody(payload.body).build())
                }
            }

    private fun isDataOnly(payload: FcmPayload): Boolean =
        payload.type == NotificationType.POST_MEAL_DELAYED_SINGLE

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

    // 이연 단건만 iOS의 커스텀 액션을 위해 APNs alert/category를 직접 구성한다.
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
}
