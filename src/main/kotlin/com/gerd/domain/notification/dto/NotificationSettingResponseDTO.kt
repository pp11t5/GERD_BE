package com.gerd.domain.notification.dto

import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.entity.enums.DailyNotificationTime

data class NotificationSettingResponseDTO(
    val postMealNotificationEnabled: Boolean,
    val dailyRecordNotificationEnabled: Boolean,
    val dailyNotificationTime: DailyNotificationTime,
    val weeklyReportEnabled: Boolean,
) {
    companion object {
        fun from(setting: UserNotificationSetting, marketingAgreed: Boolean) = NotificationSettingResponseDTO(
            postMealNotificationEnabled = setting.postMealNotificationEnabled && marketingAgreed,
            dailyRecordNotificationEnabled = setting.dailyRecordNotificationEnabled && marketingAgreed,
            dailyNotificationTime = setting.dailyNotificationTime,
            weeklyReportEnabled = setting.weeklyReportEnabled && marketingAgreed,
        )
    }
}
