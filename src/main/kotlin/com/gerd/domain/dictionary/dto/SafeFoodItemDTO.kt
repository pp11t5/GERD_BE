package com.gerd.domain.dictionary.dto

import io.swagger.v3.oas.annotations.media.Schema

// 안전 음식 목록 항목 — 항상 SAFE라 type 필드를 두지 않음
data class SafeFoodItemDTO(
    @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val foodId: String,

    @field:Schema(description = "음식 이름", example = "현미밥")
    val name: String,

    @field:Schema(description = "대표 음식 분류 code", example = "grain", nullable = true)
    val code: String?,
)
