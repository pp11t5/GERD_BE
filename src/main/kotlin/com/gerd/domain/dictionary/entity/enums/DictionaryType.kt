package com.gerd.domain.dictionary.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class DictionaryType(@get:JsonValue val code: String) {
    SAFE("safe"),
    CAUTION("caution"),
    RISK("risk"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): DictionaryType =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown DictionaryType code: $code")
    }
}
