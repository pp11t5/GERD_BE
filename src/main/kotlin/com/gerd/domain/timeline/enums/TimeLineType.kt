package com.gerd.domain.timeline.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class TimeLineType(@get:JsonValue val code: String) {
    SINGLE("single"),
    GROUP("group"),
    SYMPTOM("symptom"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): TimeLineType =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown TimeLineType code: $code")
    }
}