package com.gerd.domain.symptom.service

import com.gerd.domain.symptom.repository.SymptomRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class SymptomPatternRefreshService(
    private val symptomRepository: SymptomRepository,
    private val symptomPatternAnalysisService: SymptomPatternAnalysisService,
    private val symptomAnalysisResultWriter: SymptomAnalysisResultWriter,
) {

    @Async("analysisTaskExecutor")
    fun refreshAsync(symptomId: String, userId: Long) {
        try {
            val symptomExternalId = runCatching { java.util.UUID.fromString(symptomId) }.getOrNull() ?: return
            val symptom = symptomRepository.findByExternalIdAndUser_Id(symptomExternalId, userId) ?: return
            if (!symptom.isAnalysisDirty) return
            val expectedVersion = symptom.analysisVersion

            val result = symptomPatternAnalysisService.generate(symptom, userId)
            if (result.shouldUpdate) {
                symptomAnalysisResultWriter.updateIfVersionMatches(
                    symptomId = symptomId,
                    userId = userId,
                    expectedVersion = expectedVersion,
                    analysisJson = result.analysisJson,
                )
            }
        } catch (e: Exception) {
            log.warn { "증상 패턴 분석 비동기 갱신 실패: symptomId=$symptomId, userId=$userId, ${e.javaClass.simpleName} - ${e.message}" }
        }
    }
}
