package com.gerd.domain.mypage.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidMedicationNamesValidator::class])
annotation class ValidMedicationNames(
    val message: String = "복용약 이름은 비어 있을 수 없고 최대 100자까지 입력할 수 있습니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
