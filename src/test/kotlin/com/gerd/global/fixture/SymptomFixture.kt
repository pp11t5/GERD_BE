package com.gerd.global.fixture

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.UUID

object SymptomFixture {

    const val SYMPTOM_ID: Long = 12L
    val SYMPTOM_EXTERNAL_ID: UUID = UUID.fromString("9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val OCCURRED_AT: LocalDateTime = LocalDateTime.of(2026, 5, 12, 19, 30, 0)

    fun user(
        id: Long = 1L,
        nickname: String? = "유진",
    ): User = User(
        email = "user@test.com",
        nickname = nickname,
        role = UserRole.USER,
    ).apply {
        ReflectionTestUtils.setField(this, "id", id)
    }

    fun symptom(
        id: Long = SYMPTOM_ID,
        externalId: UUID = SYMPTOM_EXTERNAL_ID,
        user: User = user(),
        symptomState: SymptomState = SymptomState.COMFORTABLE,
        symptomTypes: Set<SymptomType> = emptySet(),
        occurredAt: LocalDateTime = OCCURRED_AT,
        mealRecordId: Long = MealRecordFixture.MEAL_RECORD_ID,
        memo: String? = null,
        analysisJson: String? = null,
        isAnalysisDirty: Boolean = true,
        analysisVersion: Long = 0L,
    ): Symptom = Symptom(
        user = user,
        symptomState = symptomState,
        symptomTypes = symptomTypes,
        occurredAt = occurredAt,
        mealRecordId = mealRecordId,
        memo = memo,
        analysisJson = analysisJson,
        isAnalysisDirty = isAnalysisDirty,
        analysisVersion = analysisVersion,
    ).apply {
        ReflectionTestUtils.setField(this, "id", id)
        this.externalId = externalId
    }
}
