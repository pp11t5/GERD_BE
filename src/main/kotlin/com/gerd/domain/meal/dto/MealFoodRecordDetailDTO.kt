package com.gerd.domain.meal.dto

import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.domain.meal.dto.MealAnalysisSnapshotDTO
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 개별 식사 기록 DTO
 */
data class MealFoodRecordDetailDTO(
    @field:Schema(description = "식사 음식 외부 식별자(UUID)", example = "7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val mealFoodId: String,

    @field:Schema(description = "먹은 시각 ISO-8601(offset 포함)", example = "2026-06-11T12:30:00+09:00")
    val eatenAt: String,

    @field:Schema(description = "음식 정보")
    val food: FoodInfoDTO,

    @field:Schema(description = "음식 분석 내용")
    val analysis: MealAnalysisSnapshotDTO?,

    @field:Schema(description = "연결된 상태 기록 (없으면 null)", nullable = true)
    val stateRecord: MealStateRecordDTO?,

    @field:Schema(description = "면책 고지")
    val disclaimer: String = JudgmentResponseDTO.DEFAULT_DISCLAIMER,

    @field:Schema(description = "판정 결과 화면에 함께 노출하는 일반 참고 자료 — 등급·트리거별로 달라지지 않는 공통 출처")
    val references: List<JudgmentResponseDTO.ReferenceDTO> = JudgmentResponseDTO.DEFAULT_REFERENCES,
) {

    data class FoodInfoDTO(
        @field:Schema(description = "부모 끼니 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        val mealRecordExternalId: String,

        @field:Schema(description = "음식 이름", example = "된장찌개")
        val name: String,

        @field:Schema(description = "대표 음식 분류 code", example = "soup_stew", nullable = true)
        val category: String?,
    )
}
