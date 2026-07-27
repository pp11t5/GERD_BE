package com.gerd.domain.symptom.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.food.service.FoodCategoryReader
import com.gerd.domain.symptom.dto.SymptomPatternAnalysisDTO
import com.gerd.domain.symptom.dto.SymptomPatternFeatureDTO
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.repository.SymptomRepository
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

/**
 * 규칙 기반 증상 패턴 분석 갱신 — LLM 호출 없이 rolling window 집계만으로 라벨을 결정한다
 * 계산이 가벼워(DB 조회 + 로컬 집계) 별도 비동기 처리 없이 호출 트랜잭션 안에서 동기로 수행한다
 */
@Service
class SymptomPatternAnalysisRefreshService(
    private val symptomRepository: SymptomRepository,
    private val symptomPatternStatsCalculator: SymptomPatternStatsCalculator,
    private val foodCategoryReader: FoodCategoryReader,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
) {

    fun refresh(symptom: Symptom, userId: Long) {
        if (!symptom.isAnalysisDirty) return
        val expectedVersion = symptom.analysisVersion

        val since = LocalDateTime.now().minusDays(SymptomPatternStatsCalculator.WINDOW_DAYS.toLong())
        val rows = symptomRepository.findLinkedRows(userId, since)
        val stats = symptomPatternStatsCalculator.calculate(SymptomPatternStatsCalculator.WINDOW_DAYS, rows)
        val feature = symptomPatternStatsCalculator.resolveFeature(stats)

        val dto = SymptomPatternAnalysisDTO(
            label = feature.label.labelText,
            pattern = render(feature.label.pattern, feature, userId),
            advice = render(feature.label.advice, feature, userId),
        )
        symptom.updateAnalysis(objectMapper.writeValueAsString(dto), expectedVersion)
    }

    // {placeholder}를 feature 값으로 치환 — {food_group}은 표시명으로, {닉네임}은 실제 사용될 때만 조회
    private fun render(template: String, feature: SymptomPatternFeatureDTO, userId: Long): String {
        if (!template.contains('{')) return template

        var result = template
            .replace("{window_days}", feature.windowDays.toString())
            .replace("{comfort_n}", feature.comfortCount.toString())

        feature.foodGroup?.let { code ->
            val displayName = foodCategoryReader.getAll().firstOrNull { it.code == code }?.displayName ?: code
            result = result.replace("{food_group}", displayName)
        }
        feature.repeatCount?.let { result = result.replace("{repeat_n}", it.toString()) }

        if (result.contains(NICKNAME_PLACEHOLDER)) {
            val nickname = userRepository.findById(userId).map { it.nickname }.orElse("")
            result = result.replace(NICKNAME_PLACEHOLDER, nickname)
        }
        return result
    }

    companion object {
        private const val NICKNAME_PLACEHOLDER = "{닉네임}"
    }
}
