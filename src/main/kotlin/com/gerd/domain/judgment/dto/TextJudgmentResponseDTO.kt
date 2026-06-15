package com.gerd.domain.judgment.dto

import com.gerd.domain.judgment.dto.JudgmentResponseDTO.JudgmentItemDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.StateRecordsDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import io.swagger.v3.oas.annotations.media.Schema

// 텍스트 입력 판정 응답 — DB 음식 엔티티가 없어 foodExternalId·category 미포함
data class TextJudgmentResponseDTO(
    @field:Schema(description = "음식 이름(입력값 그대로)", example = "아메리카노")
    val foodName: String,

    @field:Schema(description = "신호등 등급", example = "CAUTION")
    val grade: JudgmentGrade,

    @field:Schema(description = "개인화 제목 — LLM 생성, UNKNOWN·생성 실패 시 등급별 고정 제목", example = "속이 편안할 수 있도록 천천히 드세요!")
    val personalTitle: String,

    @field:Schema(description = "분석 항목 2슬롯 고정 — [0]=트리거·증상, [1]=알레르기·복용약")
    val items: List<JudgmentItemDTO>,

    @field:Schema(description = "연관 상태 기록 — 최대 3개 노출, total은 전체 개수")
    val stateRecords: StateRecordsDTO,

    // 텍스트 입력은 DB 음식 엔티티가 없어 대체식단 조회 불가 — 항상 빈 배열
    @field:Schema(description = "대체 식단 — 텍스트 판정에서는 항상 빈 배열")
    val substitutes: List<SubstituteDTO>,
)
