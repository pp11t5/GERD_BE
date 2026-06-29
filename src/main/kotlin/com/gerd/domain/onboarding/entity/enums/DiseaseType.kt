package com.gerd.domain.onboarding.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class DiseaseType(@get:JsonValue val code: String, val label: String) {
    GERD("gerd", "역류성 식도염"),
    GASTRITIS_ULCER("gastritis_ulcer", "위염/위궤양"),
    IBS("ibs", "과민성 대장 증후군"),
    FUNCTIONAL_DYSPEPSIA("functional_dyspepsia", "기능성 소화불량"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): DiseaseType =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown DiseaseType code: $code")
    }
}
