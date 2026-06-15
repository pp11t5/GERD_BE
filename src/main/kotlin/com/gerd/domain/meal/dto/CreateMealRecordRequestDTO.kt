package com.gerd.domain.meal.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

// 식사 기록 생성 요청 — "내 식단에 추가" / "같이 먹은 음식이 있나요?"
data class CreateMealRecordRequestDTO(
    @field:NotBlank
    @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val foodExternalId: String,

    // ISO-8601(offset 포함). 미전달 시 서버 now(Asia/Seoul). 형식 오류는 MEAL400_2 (typed 수신 시 COMMON 코드로 새어 String 파싱)
    @field:Schema(description = "먹은 시각 ISO-8601(offset 포함). 미전달 시 서버 현재 시각", example = "2026-06-11T12:30:00+09:00", nullable = true)
    val eatenAt: String? = null,

    // 미전달 = 새 끼니(서버가 uuid 발급), 전달 = 기존 끼니에 추가(본인 소유 검증, 실패 MEAL404_2)
    @field:Schema(description = "끼니 묶음 키(UUID). 전달 시 같은 끼니에 추가", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f", nullable = true)
    val mealGroupId: String? = null,

    // FE가 신호등 판정 화면에서 들고 온 등급 스냅샷 (ADR-0017). 미전달/null = 등급 미표시
    @field:Schema(description = "신호등 판정 등급 스냅샷", example = "RECOMMEND", nullable = true)
    val judgedGrade: JudgmentGrade? = null,
)
