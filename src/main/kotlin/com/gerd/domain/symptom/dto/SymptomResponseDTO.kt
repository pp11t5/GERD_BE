package com.gerd.domain.symptom.dto

import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 증상 상세 정보 응답 (증상 상세 화면)
 */
data class SymptomResponseDTO(
    @field:Schema(description = "증상 기록 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val symptomId: String,

    @field:Schema(description = "속 상태 — 아이콘/등급 표시용", example = "comfortable")
    val symptomState: SymptomState,

    @field:Schema(description = "상태 code. 문구/이모지는 프론트에서 매핑", example = "comfortable")
    val stateTitle: String,

    @field:Schema(description = "느낀 증상 목록", example = "[\"throat_foreign_body\"]")
    val symptomTypes: List<SymptomType>,

    @field:Schema(description = "증상 발생 시각 ISO-8601(offset 포함)", example = "2026-05-12T19:30:00+09:00")
    val occurredAt: String,

    @field:Schema(description = "연결된 원인 끼니. 미연결 증상은 null", nullable = true)
    val linkedMeal: LinkedMealDTO?,

    @field:Schema(description = "AI 맞춤 분석. 분석 전이면 null", nullable = true)
    val analysis: AnalysisDTO?,
) {
    data class LinkedMealDTO(
        @field:Schema(description = "끼니 식별자(UUID)")
        val mealRecordId: String,

        @field:Schema(description = "연결된 음식 목록")
        val foods: List<LinkedFoodDTO>,
    )

    data class LinkedFoodDTO(
        @field:Schema(description = "식사 음식기록 외부 식별자(UUID)")
        val mealFoodId: String,

        @field:Schema(description = "음식 이름", example = "아메리카노")
        val name: String,

        @field:Schema(description = "대표 음식 분류 code", example = "beverage", nullable = true)
        val category: String?,
    )

    data class AnalysisDTO(
        @field:Schema(description = "분석 항목")
        val items: List<Item>,
    )

    data class Item(
        @field:Schema(description = "강조 문구", example = "편안한 식사 패턴이에요")
        val emphasis: String,

        @field:Schema(description = "본문", example = "이번 주 '저녁 가벼운 식사' 후 편안함 응답이 3회 연속 기록됐어요.")
        val body: String,
    )
}
