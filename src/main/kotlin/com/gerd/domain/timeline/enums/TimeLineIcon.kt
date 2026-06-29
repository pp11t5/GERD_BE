package com.gerd.domain.timeline.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class TimeLineIcon(@get:JsonValue val code: String) {
    SUN("sun"),
    MOON("moon"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): TimeLineIcon =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown TimeLineIcon code: $code")
    }
}
