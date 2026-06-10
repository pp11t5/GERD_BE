package com.gerd.domain.notification.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class NotificationType(@get:JsonValue val code: String) {
    POST_MEAL("post_meal"),
    DAILY_RECORD("daily_record"),
    WEEKLY_REPORT("weekly_report"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): NotificationType =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown NotificationType code: $code")
    }
}
