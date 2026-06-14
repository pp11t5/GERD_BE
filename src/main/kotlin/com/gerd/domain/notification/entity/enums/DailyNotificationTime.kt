package com.gerd.domain.notification.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class DailyNotificationTime(@get:JsonValue val code: String) {
    MORNING_8("morning_8"),
    EVENING_8("evening_8"),
    NIGHT_9("night_9"),
    NIGHT_10("night_10"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): DailyNotificationTime =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown DailyNotificationTime code: $code")
    }
}
