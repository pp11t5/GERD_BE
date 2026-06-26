package com.gerd.domain.meal.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import io.swagger.v3.oas.annotations.media.Schema

// 개별 음식 기록 시점의 신호등 분석 스냅샷 — MealFood.analysisJson에 직렬화 저장
data class MealAnalysisSnapshotDTO(
    @field:Schema(description = "신호등 판정 등급", example = "RECOMMEND")
    val judgmentGrade: JudgmentGrade,

    @field:Schema(description = "트리거 음식 기반 분석")
    val triggerAnalysis: AnalysisItemDTO,

    @field:Schema(description = "알레르기 기반 분석")
    val allergyAnalysis: AnalysisItemDTO,
) {

    data class AnalysisItemDTO(
        @field:Schema(description = "강조 문구", example = "평소 민감한 재료가 들어있지 않아요")
        val ment: String,

        @field:Schema(description = "분석 본문", example = "최근 먹은 음식 중 비슷한 음식을 먹고 속이 편했어요.")
        val content: String,
    )
}
