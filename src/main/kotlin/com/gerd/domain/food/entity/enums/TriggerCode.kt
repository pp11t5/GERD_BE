package com.gerd.domain.food.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

// 트리거 음식 마스터 code 집합 (trigger_labels.code와 1:1) — API 요청 값이자 Swagger 노출용 enum
enum class TriggerCode(@get:JsonValue val code: String) {
    CAFFEINE("caffeine"),
    CARBONATED("carbonated"),
    ALCOHOL("alcohol"),
    SPICY("spicy"),
    FRIED_FATTY("fried_fatty"),
    CHOCOLATE("chocolate"),
    CITRUS("citrus"),
    TOMATO("tomato"),
    MINT("mint"),
    ONION_GARLIC_RAW("onion_garlic_raw"),
    CHEESE_DAIRY("cheese_dairy"),
    REFINED_FLOUR("refined_flour"),
    ;

    companion object {
        // 잘못된 code는 역직렬화 단계에서 예외 → 전역 핸들러가 400으로 변환
        @JsonCreator
        @JvmStatic
        fun from(code: String): TriggerCode =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown TriggerCode code: $code")
    }
}
