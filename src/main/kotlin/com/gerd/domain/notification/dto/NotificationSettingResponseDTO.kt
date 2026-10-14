package com.gerd.domain.notification.dto

import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.entity.enums.DailyNotificationTime

data class NotificationSettingResponseDTO(
    val postMealNotificationEnabled: Boolean,
    val dailyRecordNotificationEnabled: Boolean,
    val dailyNotificationTime: DailyNotificationTime,
    val weeklyReportEnabled: Boolean,
    val marketingEnabled: Boolean,
) {
    companion object {
        fun from(
            setting: UserNotificationSetting,
            marketingEnabled: Boolean,
        ) = NotificationSettingResponseDTO(
            postMealNotificationEnabled = setting.postMealNotificationEnabled,
            dailyRecordNotificationEnabled = setting.dailyRecordNotificationEnabled,
            dailyNotificationTime = setting.dailyNotificationTime,
            weeklyReportEnabled = setting.weeklyReportEnabled,
            marketingEnabled = marketingEnabled,
        )
    }
}
