package com.gerd.domain.food.entity.enums

// DB에는 소문자 코드로 저장한다 — 스키마 CHECK 제약 및 시드 데이터('seed','curated','user')와 일치시키기 위함
enum class FoodSource(val code: String) {
    SEED("seed"),       // 식약처 시드 데이터
    CURATED("curated"), // 운영자 큐레이션
    USER("user"),       // 사용자 추가 음식
    ;

    companion object {
        fun from(code: String): FoodSource =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown FoodSource code: $code")
    }
}
