package com.gerd.domain.notification.service

import com.gerd.domain.notification.entity.enums.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NotificationPayloadFactoryTest {

    @Test
    fun `post_meal은 targetId를 그대로 담는다`() {
        val payload = NotificationPayloadFactory.of(NotificationType.POST_MEAL, targetId = "100")

        assertThat(payload.type).isEqualTo(NotificationType.POST_MEAL)
        assertThat(payload.targetId).isEqualTo("100")
    }

    @Test
    fun `post_meal_delayed_bulk는 targetId 없이 bulkCount를 본문에 반영한다`() {
        val payload = NotificationPayloadFactory.of(NotificationType.POST_MEAL_DELAYED_BULK, bulkCount = 5)

        assertThat(payload.targetId).isNull()
        assertThat(payload.body).contains("5건")
    }

    @Test
    fun `daily_record와 weekly_report는 targetId가 없다`() {
        assertThat(NotificationPayloadFactory.of(NotificationType.DAILY_RECORD).targetId).isNull()
        assertThat(NotificationPayloadFactory.of(NotificationType.WEEKLY_REPORT).targetId).isNull()
    }
}
