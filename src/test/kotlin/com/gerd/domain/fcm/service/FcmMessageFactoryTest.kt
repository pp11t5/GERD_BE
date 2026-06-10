package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.gerd.domain.notification.entity.enums.NotificationType
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
    }

    @Nested
    inner class `buildTopic` {

        @Test
        fun `토픽 메시지를 반환한다`() {
            val message = factory.buildTopic("weekly-report", payload)

            assertThat(message).isNotNull
        }
    }
}
