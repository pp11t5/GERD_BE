package com.gerd.domain.notification.entity

import com.gerd.global.fixture.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserNotificationSettingTest {

    @Test
    fun `기본 생성 시 앱 알림 3종이 모두 활성화된다`() {
        val setting = UserNotificationSetting(user = UserFixture.user())

        assertThat(setting.postMealNotificationEnabled).isTrue()
        assertThat(setting.dailyRecordNotificationEnabled).isTrue()
        assertThat(setting.weeklyReportEnabled).isTrue()
    }
}
