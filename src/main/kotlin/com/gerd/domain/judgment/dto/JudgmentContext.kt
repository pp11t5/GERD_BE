package com.gerd.domain.judgment.dto

import com.gerd.domain.food.entity.Food
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO

// 판정에 필요한 음식·사용자 컨텍스트 — Reader가 짧은 트랜잭션에서 일괄 로딩한 뒤 지연로딩 없이 사용한다
data class JudgmentContext(
    val food: Food,
    val category: String?,
    val foodTriggers: List<TagDTO>,
    val foodAllergens: List<TagDTO>,
    val userTriggers: List<TagDTO>,
    val userAllergens: List<TagDTO>,
    val medications: List<String>,
    val symptomCodes: List<String>,
    val history: LlmInputSnapshotDTO.HistorySnapshotDTO = LlmInputSnapshotDTO.HistorySnapshotDTO(),
) {

    val foodExternalId: String
        get() = requireNotNull(food.externalId) { "영속 음식은 externalId를 가진다" }.toString()
}
