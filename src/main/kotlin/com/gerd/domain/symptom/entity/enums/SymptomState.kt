package com.gerd.domain.symptom.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class SymptomState(@get:JsonValue val code: String) {
    COMFORTABLE("comfortable"),     // 편안
    GOOD("good"),                   // 양호
    NORMAL("normal"),               // 보통
    UNCOMFORTABLE("uncomfortable"), // 불편
    SEVERE("severe"),               // 심각
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): SymptomState =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown SymptomState code: $code")
    }
}