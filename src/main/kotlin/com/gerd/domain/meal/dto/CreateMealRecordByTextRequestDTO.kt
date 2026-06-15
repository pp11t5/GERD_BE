package com.gerd.domain.meal.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

// 텍스트 입력 식사 기록 생성 요청 — DB에 없는 음식을 이름으로 직접 기록할 때 사용
data class CreateMealRecordByTextRequestDTO(
    // 동일 이름의 본인 USER 음식이 이미 있으면 재사용, 없으면 자동 생성
    @field:NotBlank
    @field:Schema(description = "음식 이름(자유 텍스트)", example = "감자탕")
    val foodTextInput: String,

    @field:Schema(description = "먹은 시각 ISO-8601(offset 포함). 미전달 시 서버 현재 시각", example = "2026-06-11T12:30:00+09:00", nullable = true)
    val eatenAt: String? = null,

    @field:Schema(description = "끼니 묶음 키(UUID). 전달 시 같은 끼니에 추가", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f", nullable = true)
    val mealGroupId: String? = null,

    @field:Schema(description = "신호등 판정 등급 스냅샷", example = "CAUTION", nullable = true)
    val judgedGrade: JudgmentGrade? = null,
)
