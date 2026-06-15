package com.gerd.domain.meal.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import io.swagger.v3.oas.annotations.media.Schema

// 식사 상세 정보 — 개인화 분석은 FE가 judgment API 병행 호출(D4), 여기선 기록 도메인 데이터만
data class MealRecordDetailDTO(
    @field:Schema(description = "식사 기록 외부 식별자(UUID)", example = "7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val mealId: String,

    @field:Schema(description = "끼니 묶음 키(UUID)", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val mealGroupId: String,

    @field:Schema(description = "먹은 시각 ISO-8601(offset 포함)", example = "2026-06-11T12:30:00+09:00")
    val eatenAt: String,

    @field:Schema(description = "추가 메모 (미작성 시 null)", example = "점심 후 잠깐 누웠더니 답답했음", nullable = true)
    val memo: String?,

    @field:Schema(description = "신호등 판정 등급 스냅샷", example = "RECOMMEND", nullable = true)
    val judgedGrade: JudgmentGrade?,

    @field:Schema(description = "음식 상세 (soft-delete된 음식도 기록 보존을 위해 그대로 반환)")
    val food: MealFoodDetailDTO,

    // 이 식사에 연결된 상태 기록 — 모델·작성은 증상 기록 task 소유, 구현 전까지 항상 빈 배열 (D8)
    @field:Schema(description = "연결된 상태 기록 (증상 기록 task 구현 전까지 빈 배열)")
    val stateRecords: List<StateRecordDTO>,
) {

    data class MealFoodDetailDTO(
        @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        val externalId: String,

        @field:Schema(description = "음식 이름", example = "된장찌개")
        val name: String,

        @field:Schema(description = "대표 음식 분류 code", example = "soup_stew", nullable = true)
        val category: String?,

        @field:Schema(description = "Hero 설명 문구 (food.description)", example = "저자극·저지방 한식 조합이에요.", nullable = true)
        val description: String?,
    )

    // judgment API StateRecordDTO와 동일 shape (D8) — 도메인 결합을 피해 계약만 복제
    data class StateRecordDTO(
        @field:Schema(description = "상태 라벨", example = "편안해요")
        val label: String,

        @field:Schema(description = "기록 날짜", example = "2026-05-08")
        val date: String,

        @field:Schema(description = "기록 시점", example = "식후 90분")
        val timing: String,
    )
}
