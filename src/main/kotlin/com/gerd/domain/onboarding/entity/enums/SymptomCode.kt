package com.gerd.domain.onboarding.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

// 온보딩 06 증상 체크리스트 code (다중선택, user_symptoms.symptom_code) — API 요청 값이자 Swagger 노출용 enum
enum class SymptomCode(@get:JsonValue val code: String) {
    HEARTBURN_REFLUX("heartburn_reflux"),
    POST_MEAL_COUGH("post_meal_cough"),
    THROAT_GLOBUS("throat_globus"),
    SOUR_MOUTH_ODOR("sour_mouth_odor"),
    SUPINE_CHEST_TIGHT("supine_chest_tight"),
    NONE_BUT_MANAGE("none_but_manage"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): SymptomCode =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown SymptomCode code: $code")
    }
}
