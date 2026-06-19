package com.gerd.domain.symptom.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class SymptomType(@get:JsonValue val code: String) {
    NONE("none"),                               // 없음
    THROAT_FOREIGN_BODY("throat_foreign_body"), // 목 이물감이 있어요
    ACID_REFLUX("acid_reflux"),                 // 신물이 느껴져요
    COUGH("cough"),                             // 기침이 나요
    CHEST_TIGHTNESS("chest_tightness"),         // 가슴이 답답해요
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): SymptomType =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown SymptomType code: $code")
    }
}