package com.gerd.domain.meal.dto

import io.swagger.v3.oas.annotations.media.Schema

// 타임라인 카드 1장 = 끼니 1개 — 끼니 단위로 그룹핑한 목록 응답 (D10)
data class MealGroupDTO(
    @field:Schema(description = "끼니 묶음 키(UUID)", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val mealGroupId: String,

    // 대표 시각 = 소속 records eatenAt의 최솟값 (카드 표기 시각). 자정 교차 시 그날 실린 records 기준
    @field:Schema(description = "대표 시각(소속 기록 중 가장 이른 시각) ISO-8601", example = "2026-06-11T09:02:00+09:00")
    val eatenAt: String,

    @field:Schema(description = "끼니에 속한 기록들 (eatenAt 오름차순)")
    val records: List<MealRecordSummaryDTO>,
)
