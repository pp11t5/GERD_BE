package com.gerd.domain.symptom.dto

import com.gerd.domain.symptom.dto.enums.SymptomPatternLabel

/**
 * 최근 rolling window 집계로 계산한 증상 패턴 라벨 + 멘트 슬롯 값
 */
data class SymptomPatternFeatureDTO(
    val label: SymptomPatternLabel,
    val windowDays: Int,
    val comfortCount: Int,
    val foodGroup: String? = null,
    val repeatCount: Int? = null,
)
