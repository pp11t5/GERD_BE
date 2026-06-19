package com.gerd.domain.meal.dto

import com.gerd.domain.judgment.dto.JudgmentResponseDTO.JudgmentItemDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO

// 개별 음식 기록 시점의 신호등 분석 스냅샷 — MealFood.analysisJson에 직렬화 저장
// grade는 MealFood.judgedGrade 컬럼에, foodName·category는 food 조인으로 복원하므로 제외
data class MealAnalysisSnapshotDTO(
    val personalTitle: String,
    val items: List<JudgmentItemDTO>,
    val substitutes: List<SubstituteDTO>,
)
