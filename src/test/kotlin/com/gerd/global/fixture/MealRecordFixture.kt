package com.gerd.global.fixture

import com.gerd.domain.auth.entity.User
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.entity.MealRecord
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.UUID

object MealRecordFixture {

    // 기록 단건 식별자 / 끼니 묶음 키 — 응답은 record 필드와 대조하므로 값 자체는 무관
    val MEAL_EXTERNAL_ID: UUID = UUID.fromString("7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val MEAL_GROUP_ID: UUID = UUID.fromString("c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val EATEN_AT: LocalDateTime = LocalDateTime.of(2026, 6, 11, 12, 30, 0)

    // 끼니/등급/메모 분기를 표현 가능한 식사 기록 — id·externalId는 주입
    fun mealRecord(
        id: Long = 1L,
        user: User = User(email = "fixture@test.com"),
        foodId: Long = 1L,
        mealGroupId: UUID = MEAL_GROUP_ID,
        eatenAt: LocalDateTime = EATEN_AT,
        judgedGrade: JudgmentGrade? = JudgmentGrade.RECOMMEND,
        memo: String? = null,
        externalId: UUID = MEAL_EXTERNAL_ID,
    ): MealRecord = MealRecord(
        user = user,
        foodId = foodId,
        mealGroupId = mealGroupId,
        eatenAt = eatenAt,
        judgedGrade = judgedGrade,
        memo = memo,
    ).apply {
        ReflectionTestUtils.setField(this, "id", id)
        this.externalId = externalId // BaseEntity의 public var로 직접 할당
    }
}
