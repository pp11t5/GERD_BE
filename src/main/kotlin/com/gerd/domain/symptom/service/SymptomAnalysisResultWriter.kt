package com.gerd.domain.symptom.service

import com.gerd.domain.symptom.repository.SymptomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SymptomAnalysisResultWriter(
    private val symptomRepository: SymptomRepository,
) {

    @Transactional
    fun updateIfVersionMatches(
        symptomId: String,
        userId: Long,
        expectedVersion: Long,
        analysisJson: String,
    ): Boolean {
        val symptomExternalId = runCatching { UUID.fromString(symptomId) }.getOrNull() ?: return false
        val symptom = symptomRepository.findByExternalIdAndUser_Id(symptomExternalId, userId) ?: return false
        return symptom.updateAnalysis(analysisJson, expectedVersion)
    }
}
