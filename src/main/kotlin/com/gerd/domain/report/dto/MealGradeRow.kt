package com.gerd.domain.report.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import java.time.LocalDate

data class MealGradeRow(
    val date: LocalDate,
    val grade: JudgmentGrade?,
)
