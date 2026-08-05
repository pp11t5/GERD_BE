package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.gerd.domain.notification.entity.enums.NotificationType
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

    // Message.getData()가 package-private이라 리플렉션으로 직접 조회
    private fun dataOf(message: Message): Map<String, String> {
        val getData = Message::class.java.getDeclaredMethod("getData")
        getData.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return getData.invoke(message) as Map<String, String>
    }

    @Nested
    inner class `build` {

        @Test
        fun `ANDROID 플랫폼이면 AndroidConfig가 포함된 메시지를 반환한다`() {
            val message = factory.build("fcm-token", DevicePlatform.ANDROID, payload)

            assertThat(message).isNotNull
        }

        @Test
        fun `IOS 플랫폼이면 ApnsConfig가 포함된 메시지를 반환한다`() {
            val message = factory.build("fcm-token", DevicePlatform.IOS, payload)

            assertThat(message).isNotNull
        }

        @Test
        fun `data 필드에 type과 targetId가 명세대로 채워진다`() {
            val message = factory.build("fcm-token", DevicePlatform.ANDROID, payload)

            assertThat(dataOf(message)).containsExactlyInAnyOrderEntriesOf(
                mapOf("type" to "post_meal", "targetId" to "record-1"),
            )
        }

        @Test
        fun `targetId가 없으면 data에 targetId 키 자체가 빠진다`() {
            val noTargetPayload = payload.copy(type = NotificationType.DAILY_RECORD, targetId = null)

            val message = factory.build("fcm-token", DevicePlatform.ANDROID, noTargetPayload)

            assertThat(dataOf(message)).containsExactlyEntriesOf(mapOf("type" to "daily_record"))
        }
    }
}
