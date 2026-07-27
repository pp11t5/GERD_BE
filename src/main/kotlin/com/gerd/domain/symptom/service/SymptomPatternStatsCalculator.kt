package com.gerd.domain.symptom.service

import com.gerd.domain.symptom.dto.SymptomPatternFeatureDTO
import com.gerd.domain.symptom.dto.SymptomPatternStatsDTO
import com.gerd.domain.symptom.dto.enums.SymptomPatternLabel
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


    // 집계 결과에서 가장 뚜렷한(comfort/discomfort 중 값이 큰) 그룹을 골라 라벨을 판정
    // 그룹 간 값이 동률이면 groupBy 순서(= rows 순서, occurredAt desc)상 먼저 나온 그룹을 우선한다
    fun resolveFeature(stats: SymptomPatternStatsDTO): SymptomPatternFeatureDTO {
        val strongest = stats.groupStats.maxByOrNull { maxOf(it.comfortCount, it.discomfortCount) }
        val strongestCount = strongest?.let { maxOf(it.comfortCount, it.discomfortCount) } ?: 0
        val reliable = stats.totalRecordCount >= MIN_RELIABLE_RECORD_COUNT && strongestCount >= MIN_RELIABLE_PATTERN_COUNT

        val label = when {
            !reliable -> SymptomPatternLabel.OBSERVING
            strongest != null && strongest.discomfortCount >= strongest.comfortCount -> SymptomPatternLabel.CAUTION
            else -> SymptomPatternLabel.MAINTAIN
        }

        return SymptomPatternFeatureDTO(
            label = label,
            windowDays = stats.windowDays,
            comfortCount = strongest?.comfortCount ?: 0,
            foodGroup = strongest?.groupKey,
            repeatCount = strongest?.discomfortCount,
        )
    }

    private fun groupKeyOf(row: SymptomMealPatternRow): String =
        row.category ?: UNCATEGORIZED_CATEGORY

    private fun SymptomState.isComfort(): Boolean =
        this == SymptomState.COMFORTABLE || this == SymptomState.GOOD

    private fun SymptomState.isDiscomfort(): Boolean =
        this == SymptomState.UNCOMFORTABLE || this == SymptomState.SEVERE

    companion object {
        const val WINDOW_DAYS = 14
        private const val MIN_RELIABLE_RECORD_COUNT = 3
        private const val MIN_RELIABLE_PATTERN_COUNT = 2
    }

    private val UNCATEGORIZED_CATEGORY = "uncategorized"
}
