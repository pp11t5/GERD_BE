package com.gerd.domain.judgment.dto

import com.gerd.domain.judgment.dto.JudgmentResponseDTO.JudgmentItemDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade

// 캐시 value — 사용자 식별 정보 없이 입력 스냅샷만으로 결정되는 최종 응답 본문이라 그대로 저장·재사용한다
data class CachedJudgment(
    val foodExternalId: String?,  // 텍스트 판정은 DB 음식 엔티티가 없어 null
    val foodName: String,
    val category: String?,
    val grade: JudgmentGrade,
    val personalTitle: String,
    val items: List<JudgmentItemDTO>,
    val substitutes: List<SubstituteDTO>,
)
