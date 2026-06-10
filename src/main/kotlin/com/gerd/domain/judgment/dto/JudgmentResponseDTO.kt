package com.gerd.domain.judgment.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import io.swagger.v3.oas.annotations.media.Schema

// 신호등 판정 응답 — grade는 LLM 1차 판정이 아니라 안전 룰 오버라이드까지 거친 최종값
data class JudgmentResponseDTO(
    @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val foodExternalId: String,

    @field:Schema(description = "음식 이름", example = "아메리카노")
    val foodName: String,

    @field:Schema(description = "신호등 등급", example = "CAUTION")
    val grade: JudgmentGrade,

    @field:Schema(description = "개인화 제목 (등급별 톤)", example = "속이 편안할 수 있도록 천천히 드세요!")
    val personalTitle: String,

    @field:Schema(description = "분석 항목 2슬롯 고정 — [0]=트리거·증상, [1]=알레르기·복용약")
    val items: List<JudgmentItemDTO>,

    @field:Schema(description = "연관 상태 기록 리스트 (식사기록 도메인 연동 전까지 항상 빈 배열)")
    val stateRecords: List<StateRecordDTO>,

    @field:Schema(description = "안심되는 대체 식단 — CAUTION/RISK일 때만, 아니면 빈 배열")
    val substitutes: List<SubstituteDTO>,

    @field:Schema(description = "고정 면책 문구", example = "본 앱은 진단·치료 서비스가 아닙니다.")
    val disclaimer: String,

    @field:Schema(description = "캐시 응답 여부 (디버그/관측용)", example = "true")
    val cached: Boolean,
) {

    data class JudgmentItemDTO(
        @field:Schema(description = "강조 문구", example = "카페인이 들어 있어요")
        val emphasis: String,

        @field:Schema(description = "본문", example = "평소 민감하셨다면 천천히 드시는 게 좋아요.")
        val body: String,
    )

    data class StateRecordDTO(
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
}
