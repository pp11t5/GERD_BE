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

    @field:Schema(description = "신호등 등급: RECOMMEND(추천)·CAUTION(주의)·RISK(위험)·UNKNOWN(판단 불가)", example = "CAUTION")
    val grade: JudgmentGrade,

    @field:Schema(description = "개인화 제목 — LLM 생성, 생성 실패 시 등급별 고정 제목", example = "속이 편안할 수 있도록 천천히 드세요!")
    val personalTitle: String,

    @field:Schema(description = "분석 항목 2슬롯 고정 — [0]=트리거·증상, [1]=알레르기·최근 증상 패턴")
    val items: List<JudgmentItemDTO>,

    @field:Schema(description = "연관 상태 기록 — 최대 3개 노출, total은 전체 개수")
    val stateRecords: StateRecordsDTO,

    // 텍스트 입력은 DB 음식 엔티티가 없어 대체식단 조회 불가 — 항상 빈 배열
    @field:Schema(description = "대체 식단 — 텍스트 판정에서는 항상 빈 배열")
    val substitutes: List<SubstituteDTO>,

    @field:Schema(description = "면책 고지")
    val disclaimer: String = JudgmentResponseDTO.DEFAULT_DISCLAIMER,

    @field:Schema(description = "판정 결과 화면에 함께 노출하는 일반 참고 자료 — 등급·트리거별로 달라지지 않는 공통 출처")
    val references: List<JudgmentResponseDTO.ReferenceDTO> = JudgmentResponseDTO.DEFAULT_REFERENCES,

    @field:Schema(description = "LLM이 분류한 카테고리 code — 이미 분류된 음식이거나 분류 불가 시 null", example = "soup_stew")
    val categoryCode: String? = null,
)
