package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.gerd.domain.notification.entity.enums.NotificationType
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Message
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FcmMessageFactoryTest {

    private val factory = FcmMessageFactory()

    private val payload = FcmPayload(
        title = "테스트 제목",
        body = "테스트 내용",
        type = NotificationType.POST_MEAL,
        targetId = "record-1",
    )

    // Message.getData()/getNotification()이 package-private이라 리플렉션으로 직접 조회
    private fun dataOf(message: Message): Map<String, String> {
        val getData = Message::class.java.getDeclaredMethod("getData")
        getData.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return getData.invoke(message) as Map<String, String>
    }

    private fun notificationOf(message: Message): Any? {
        val getNotification = Message::class.java.getDeclaredMethod("getNotification")
        getNotification.isAccessible = true
        return getNotification.invoke(message)
    }

    private fun apnsOf(message: Message): ApnsConfig? {
        val getApnsConfig = Message::class.java.getDeclaredMethod("getApnsConfig")
        getApnsConfig.isAccessible = true
        return getApnsConfig.invoke(message) as ApnsConfig?
    }

    private fun fieldOf(instance: Any, name: String): Any? {
        val field = instance.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(instance)
    }

    @Nested
    inner class `build` {

        @Test
        fun `ANDROID 플랫폼이면 AndroidConfig가 포함된 메시지를 반환한다`() {
            val message = factory.build("fcm-token", DevicePlatform.ANDROID, payload)

            assertThat(message).isNotNull
        }

        @Test
        fun `IOS 플랫폼이면 메시지를 반환한다`() {
            val message = factory.build("fcm-token", DevicePlatform.IOS, payload)

            assertThat(message).isNotNull
        }

        @Test
        fun `data 필드에 type·title·body·targetId가 명세대로 채워진다`() {
            val message = factory.build("fcm-token", DevicePlatform.ANDROID, payload)

            assertThat(dataOf(message)).containsExactlyInAnyOrderEntriesOf(
                mapOf("type" to "post_meal", "title" to "테스트 제목", "body" to "테스트 내용", "targetId" to "record-1"),
            )
        }

        @Test
        fun `targetId가 없으면 data에 targetId 키 자체가 빠진다`() {
            val noTargetPayload = payload.copy(type = NotificationType.DAILY_RECORD, targetId = null)

            val message = factory.build("fcm-token", DevicePlatform.ANDROID, noTargetPayload)

            assertThat(dataOf(message)).containsExactlyInAnyOrderEntriesOf(
                mapOf("type" to "daily_record", "title" to "테스트 제목", "body" to "테스트 내용"),
            )
        }

        @Test
        fun `이연 단건 식후 알림만 data-only 메시지다`() {
            val message = factory.build(
                "fcm-token",
                DevicePlatform.ANDROID,
                payload.copy(type = NotificationType.POST_MEAL_DELAYED_SINGLE),
            )

            assertThat(notificationOf(message)).isNull()
        }

        @Test
        fun `이연 단건 식후 알림은 IOS APNs alert와 category를 포함한다`() {
            val message = factory.build(
                "fcm-token",
                DevicePlatform.IOS,
                payload.copy(type = NotificationType.POST_MEAL_DELAYED_SINGLE),
            )
            val apns = apnsOf(message)!!

            @Suppress("UNCHECKED_CAST")
            val headers = fieldOf(apns, "headers") as Map<String, String>
            @Suppress("UNCHECKED_CAST")
            val aps = (fieldOf(apns, "payload") as Map<String, Any>)["aps"] as Map<String, Any>
            val alert = aps["alert"]!!

            assertThat(headers).containsExactlyInAnyOrderEntriesOf(
                mapOf("apns-push-type" to "alert", "apns-priority" to "10"),
            )
            assertThat(fieldOf(alert, "title")).isEqualTo("테스트 제목")
            assertThat(fieldOf(alert, "body")).isEqualTo("테스트 내용")
            assertThat(aps).containsEntry("category", "post_meal_delayed_single")
        }

        @Test
        fun `이연 단건 외 알림은 FCM notification을 포함하고 커스텀 APNs 설정을 사용하지 않는다`() {
            val types = listOf(
                NotificationType.POST_MEAL,
                NotificationType.POST_MEAL_DELAYED_BULK,
                NotificationType.DAILY_RECORD,
                NotificationType.WEEKLY_REPORT,
            )

            types.forEach { type ->
                val message = factory.build("fcm-token", DevicePlatform.IOS, payload.copy(type = type))

                assertThat(notificationOf(message)).isNotNull
                assertThat(apnsOf(message)).isNull()
            }
        }
    }
}
