package com.gerd.domain.notification.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class NotificationType(
    @get:JsonValue val code: String,
    val settingType: NotificationSettingType,
) {
    POST_MEAL("post_meal", NotificationSettingType.POST_MEAL),
    POST_MEAL_DELAYED_SINGLE("post_meal_delayed_single", NotificationSettingType.POST_MEAL),
    POST_MEAL_DELAYED_BULK("post_meal_delayed_bulk", NotificationSettingType.POST_MEAL),
    DAILY_RECORD("daily_record", NotificationSettingType.DAILY_RECORD),
    WEEKLY_REPORT("weekly_report", NotificationSettingType.WEEKLY_REPORT),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): NotificationType =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown NotificationType code: $code")
    }
}
