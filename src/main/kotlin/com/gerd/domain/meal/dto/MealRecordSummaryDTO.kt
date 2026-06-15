package com.gerd.domain.meal.dto

import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import io.swagger.v3.oas.annotations.media.Schema

// 식사 기록 1건 요약 — 목록·생성 응답 공통 타입
data class MealRecordSummaryDTO(
    @field:Schema(description = "식사 기록 외부 식별자(UUID)", example = "7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val mealId: String,

    @field:Schema(description = "끼니 묶음 키(UUID) — 같은 값 = 같은 끼니", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val mealGroupId: String,

    @field:Schema(description = "먹은 시각 ISO-8601(offset 포함)", example = "2026-06-11T12:30:00+09:00")
    val eatenAt: String,

    @field:Schema(description = "음식 요약")
    val food: FoodSummaryDTO,

    // 생성 시점 신호등 등급 스냅샷 (ADR-0017). 판정 미전달/실패 시 null
    @field:Schema(description = "신호등 판정 등급 스냅샷", example = "RECOMMEND", nullable = true)
    val judgedGrade: JudgmentGrade?,
)
