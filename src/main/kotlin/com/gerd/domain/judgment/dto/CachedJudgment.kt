package com.gerd.domain.judgment.dto

import com.gerd.domain.judgment.dto.JudgmentResponseDTO.JudgmentItemDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade

/**
 * 캐시 value — 닉네임 placeholder({nickname}) 상태로 저장한다
 *
 * 치환 후 본문을 캐시하면 닉네임 변경 시 TTL 동안 옛 닉네임이 노출되므로,
 * 치환은 HIT/MISS 모두 응답 직전에 수행한다 (spec §7)
 */
data class CachedJudgment(
    val foodExternalId: String,
    val foodName: String,
    val grade: JudgmentGrade,
    val personalTitleTemplate: String,
    val items: List<JudgmentItemDTO>,
    val substitutes: List<SubstituteDTO>,
)
