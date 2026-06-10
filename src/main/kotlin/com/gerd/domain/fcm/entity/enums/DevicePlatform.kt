package com.gerd.domain.fcm.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class DevicePlatform(@get:JsonValue val code: String) {
    IOS("ios"),
    ANDROID("android"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): DevicePlatform =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown DevicePlatform code: $code")
    }
}
