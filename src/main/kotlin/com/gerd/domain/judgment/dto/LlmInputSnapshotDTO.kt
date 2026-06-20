package com.gerd.domain.judgment.dto

/**
 * LLM 입력 스냅샷 — 프롬프트 본문이자 캐시 키의 원천
 *
 * - 캐시 키 안정성을 위해 모든 리스트는 생성 시점에 code 기준 정렬된 상태여야 한다
 */
data class LlmInputSnapshotDTO(
    val food: FoodSnapshotDTO,
    val user: UserSnapshotDTO,
    val history: HistorySnapshotDTO = HistorySnapshotDTO(),
) {

    data class FoodSnapshotDTO(
        val name: String,
        val category: String?,
        val knownAttributes: List<String>,
        val triggerTags: List<TagDTO>,
        val allergenTags: List<TagDTO>,
    )

    data class UserSnapshotDTO(
        val nickname: String?,
        val symptoms: List<String>,
        val triggerFoods: List<TagDTO>,
        val allergies: List<TagDTO>,
        val meds: List<String>,
    )

    data class HistorySnapshotDTO(
        val similarFoodRecords: List<SimilarFoodRecordDTO> = emptyList(),
        val comfortCount: Int = 0,
        val discomfortCount: Int = 0,
    )

    data class SimilarFoodRecordDTO(
        val food: String,
        val state: String,
        val count: Int,
    )

    // code는 룰 매칭·정렬 기준, label은 LLM grounding용 한글 표시명
    data class TagDTO(
        val code: String,
        val label: String,
    )
}
