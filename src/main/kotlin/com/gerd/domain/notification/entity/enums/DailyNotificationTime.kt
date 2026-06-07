package com.gerd.domain.notification.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class DailyNotificationTime(@get:JsonValue val code: String, val topic: String) {
    MORNING_8("morning_8",  "daily-record-morning-8"),
    EVENING_8("evening_8",  "daily-record-evening-8"),
    NIGHT_9("night_9",      "daily-record-night-9"),
    NIGHT_10("night_10",    "daily-record-night-10"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): DailyNotificationTime =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown DailyNotificationTime code: $code")
    }
}
