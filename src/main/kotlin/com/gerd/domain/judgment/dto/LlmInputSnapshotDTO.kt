package com.gerd.domain.judgment.dto

/**
 * LLM 입력 스냅샷 — 프롬프트 본문이자 캐시 키의 원천
 *
 * - PII(닉네임·이름·식별자) 절대 포함 금지 — 무료 티어 프롬프트는 학습에 쓰일 수 있다 (spec §5)
 * - 캐시 키 안정성을 위해 모든 리스트는 생성 시점에 code 기준 정렬된 상태여야 한다
 * - history는 식사기록 도메인 연동 전까지 빈 값 고정 — 연동 시 키가 자연 분리되며 캐시도 자연 무효화된다
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
