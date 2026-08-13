package com.gerd.domain.judgment.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import io.swagger.v3.oas.annotations.media.Schema

// 신호등 판정 응답 — grade는 LLM 1차 판정이 아니라 안전 룰 오버라이드까지 거친 최종값
data class JudgmentResponseDTO(
    @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val foodExternalId: String,

    @field:Schema(description = "음식 이름", example = "아메리카노")
    val foodName: String,

    // 데이터상 다중 분류가 가능하지만 화면 노출은 대표 분류 1개 분류 없으면 null
    @field:Schema(description = "대표 음식 분류 code", example = "beverage", nullable = true)
    val category: String?,

    @field:Schema(description = "신호등 등급: RECOMMEND(추천)·CAUTION(주의)·RISK(위험)·UNKNOWN(판단 불가)", example = "CAUTION")
    val grade: JudgmentGrade,

    @field:Schema(description = "개인화 제목 — LLM 생성, 등급 강등·생성 실패 시 등급별 고정 제목", example = "속이 편안할 수 있도록 천천히 드세요!")
    val personalTitle: String,

    @field:Schema(description = "분석 항목 2슬롯 고정 — [0]=트리거·증상, [1]=알레르기·복용약")
    val items: List<JudgmentItemDTO>,

    @field:Schema(description = "연관 상태 기록 — 최대 3개 노출, total은 전체 개수")
    val stateRecords: StateRecordsDTO,

    @field:Schema(description = "안심되는 대체 식단 — CAUTION/RISK일 때만, 아니면 빈 배열")
    val substitutes: List<SubstituteDTO>,

    @field:Schema(description = "면책 고지")
    val disclaimer: String = DEFAULT_DISCLAIMER,

    @field:Schema(description = "판정에 사용된 근거 자료")
    val references: List<ReferenceDTO> = DEFAULT_REFERENCES,
) {

    data class JudgmentItemDTO(
        @field:Schema(description = "강조 문구", example = "카페인이 들어 있어요")
        val emphasis: String,

        @field:Schema(description = "본문", example = "평소 민감하셨다면 천천히 드시는 게 좋아요.")
        val body: String,
    )

    data class StateRecordsDTO(
        @field:Schema(description = "전체 상태 기록 개수")
        val total: Int,

        @field:Schema(description = "최근 상태 기록 최대 3개")
        val records: List<JudgmentStateRecordDTO>,
    )

    data class JudgmentStateRecordDTO(
        @field:Schema(description = "상태 라벨", example = "보통이에요")
        val label: String,

        @field:Schema(description = "기록 날짜", example = "2026-05-08")
        val date: String,

        @field:Schema(description = "기록 시점", example = "식후 90분")
        val timing: String,
    )

    data class SubstituteDTO(
        @field:Schema(description = "대체 음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        val foodExternalId: String,

        @field:Schema(description = "대체 음식 이름", example = "디카페인 아메리카노")
        val name: String,
    )

    data class ReferenceDTO(
        @field:Schema(description = "출처 표기명", example = "미국소화기학회(ACG) 위식도역류질환 진료지침 2022")
        val label: String,

        @field:Schema(description = "출처 링크(PubMed 무료 초록 — ACG 원문은 페이월)", example = "https://pubmed.ncbi.nlm.nih.gov/34807007/")
        val url: String,
    )

    companion object {
        // TODO: 최종 면책 고지 문구는 제품·법무 확정 필요 — 지금은 placeholder
        const val DEFAULT_DISCLAIMER = "이 정보는 의학적 진단·치료를 대신하지 않아요. 증상이 계속되면 전문의와 상담해 주세요."

        val DEFAULT_REFERENCES = listOf(
            ReferenceDTO(
                label = "미국소화기학회(ACG) 위식도역류질환 진료지침 2022",
                url = "https://pubmed.ncbi.nlm.nih.gov/34807007/",
            ),
        )
    }
}
