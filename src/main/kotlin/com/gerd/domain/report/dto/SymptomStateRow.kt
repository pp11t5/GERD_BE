package com.gerd.domain.report.dto

import com.gerd.domain.symptom.entity.enums.SymptomState
import java.time.LocalDate

data class SymptomStateRow(
    val date: LocalDate,
    val state: SymptomState,
)
