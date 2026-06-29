package com.gerd.domain.mypage.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidMedicationNamesValidator : ConstraintValidator<ValidMedicationNames, List<String>?> {

    override fun isValid(value: List<String>?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        return value.all { it.isNotBlank() && it.length <= MAX_MEDICATION_NAME_LENGTH }
    }

    companion object {
        private const val MAX_MEDICATION_NAME_LENGTH = 100
    }
}
