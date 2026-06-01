package com.gerd.domain.food.entity.enums

// DB에는 소문자 코드로 저장한다 — 스키마 CHECK 제약 및 시드 데이터('public','private')와 일치시키기 위함
enum class FoodVisibility(val code: String) {
    PUBLIC("public"),   // 공개 카탈로그
    PRIVATE("private"), // 사용자 비공개
    ;

    companion object {
        fun from(code: String): FoodVisibility =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown FoodVisibility code: $code")
    }
}
