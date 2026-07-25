package com.gerd.domain.symptom.dto

/**
 * rolling window 연결 기록의 순수 집계 결과 (라벨 판정 이전 단계)
 * groupKey는 지금은 음식 카테고리지만, 음식명/트리거코드 등으로 grain을 바꿔 낄 수 있게 분리
 */
data class SymptomPatternStatsDTO(
    val windowDays: Int,
    val totalRecordCount: Int,
    val comfortCount: Int,
    val discomfortCount: Int,
    val groupStats: List<GroupStatDTO>,
) {
    data class GroupStatDTO(
        val groupKey: String,
        val comfortCount: Int,
        val discomfortCount: Int,
    )
}
