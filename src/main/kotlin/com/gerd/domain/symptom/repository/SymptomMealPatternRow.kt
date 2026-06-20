package com.gerd.domain.symptom.repository

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.symptom.entity.enums.SymptomState
import java.time.LocalDateTime

/**
 * 증상과 식사 기록이 연결된 패턴 분석 결과를 담는 DTO 클래스
 * - symptomInternalId: 증상 내부 PK
 * - symptomState: 증상 상태 (예: 경증, 중등증, 중증)
 * - occurredAt: 증상 발생 시각
 * - mealRecordId: 연결된 식사 기록 ID
 * - foodName: 식사 기록에 포함된 음식 이름
 * - category: 음식 카테고리 (예: 탄수화물, 단백질, 지방)
 * - judgmentGrade: 음식 판정 등급
 */
data class SymptomMealPatternRow(
    val symptomInternalId: Long,
    val symptomState: SymptomState,
    val occurredAt: LocalDateTime,
    val mealRecordId: Long,
    val foodName: String,
    val category: String?,
    val judgmentGrade: JudgmentGrade?,
)
