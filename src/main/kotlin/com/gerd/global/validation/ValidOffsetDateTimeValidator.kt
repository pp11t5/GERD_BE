package com.gerd.global.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class ValidOffsetDateTimeValidator : ConstraintValidator<ValidOffsetDateTime, String?> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        if (value.isBlank()) return false
        return try {
            OffsetDateTime.parse(value)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }
}
