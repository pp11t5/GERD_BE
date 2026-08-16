package com.gerd.domain.judgment.dto

import com.gerd.domain.judgment.dto.JudgmentResponseDTO.JudgmentItemDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade

// 캐시 value — 사용자 식별 정보 없이 입력 스냅샷만으로 결정되는 최종 응답 본문
data class CachedJudgment(
    val foodExternalId: String?,  // 텍스트 판정은 DB 음식 엔티티가 없어 null
    val foodName: String,
    val category: String?,
    val grade: JudgmentGrade,
    val personalTitle: String,
    val items: List<JudgmentItemDTO>,
    val substitutes: List<SubstituteDTO>,
    val categoryCode: String? = null, // 텍스트 판정에서 미분류 음식을 분류한 결과 — 이미 분류된 음식이거나 미분류 상태를 판단할 수 없으면 null
)
