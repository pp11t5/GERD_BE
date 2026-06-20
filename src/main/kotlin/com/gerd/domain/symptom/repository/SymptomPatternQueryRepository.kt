package com.gerd.domain.symptom.repository

import java.time.LocalDateTime

interface SymptomPatternQueryRepository {
    fun findLinkedRows(userId: Long, since: LocalDateTime): List<SymptomMealPatternRow>
}
