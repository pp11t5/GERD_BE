package com.gerd.domain.food.entity.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

// 알레르기 마스터 code 집합 (allergens.code와 1:1) — API 요청 값이자 Swagger 노출용 enum
enum class AllergenCode(@get:JsonValue val code: String) {
    MILK("milk"),
    EGG("egg"),
    WHEAT("wheat"),
    SOY("soy"),
    PEANUT("peanut"),
    CRUSTACEAN("crustacean"),
    TREE_NUT("tree_nut"),
    FISH_SHELLFISH("fish_shellfish"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(code: String): AllergenCode =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown AllergenCode code: $code")
    }
}
