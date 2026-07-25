package com.gerd.domain.symptom.service

import com.gerd.domain.symptom.dto.SymptomPatternStatsDTO
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomMealPatternRow
import org.springframework.stereotype.Component

/**
 * 연결 식사-증상 rows를 순수 집계
 *
 * groupKey는 현재 category — 음식명/트리거코드로 grain을 바꾸려면 groupKeyOf()만 교체하면 된다
 */
@Component
class SymptomPatternStatsCalculator {

    // windowDays: rolling window 기간, rows: rolling window 내 연결 식사-증상 rows
    fun calculate(windowDays: Int, rows: List<SymptomMealPatternRow>): SymptomPatternStatsDTO {
        val distinctRows = rows.distinctBy { it.symptomInternalId }

        val groupStats = rows
            .groupBy(::groupKeyOf)
            .map { (groupKey, groupRows) ->
                val distinctGroupRows = groupRows.distinctBy { it.symptomInternalId }
                SymptomPatternStatsDTO.GroupStatDTO(
                    groupKey = groupKey,
                    comfortCount = distinctGroupRows.count { it.symptomState.isComfort() },
                    discomfortCount = distinctGroupRows.count { it.symptomState.isDiscomfort() },
                )
            }

        return SymptomPatternStatsDTO(
            windowDays = windowDays,
            totalRecordCount = distinctRows.size,
            comfortCount = distinctRows.count { it.symptomState.isComfort() },
            discomfortCount = distinctRows.count { it.symptomState.isDiscomfort() },
            groupStats = groupStats,
        )
    }


    private fun groupKeyOf(row: SymptomMealPatternRow): String =
        row.category ?: UNCATEGORIZED_CATEGORY

    private fun SymptomState.isComfort(): Boolean =
        this == SymptomState.COMFORTABLE || this == SymptomState.GOOD

    private fun SymptomState.isDiscomfort(): Boolean =
        this == SymptomState.UNCOMFORTABLE || this == SymptomState.SEVERE

    private val UNCATEGORIZED_CATEGORY = "uncategorized"
}
