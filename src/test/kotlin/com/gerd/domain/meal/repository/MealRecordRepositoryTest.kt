package com.gerd.domain.meal.repository

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.entity.MealFood
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.global.config.QuerydslTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class)
class MealRecordRepositoryTest @Autowired constructor(
    private val mealFoodRepository: MealFoodRepository,
    private val mealRecordRepository: MealRecordRepository,
) {

    @Nested
    inner class `MealFood 조회` {

        @Test
        fun `mealRecordId로 소속 음식을 먹은 시각 오름차순 조회한다`() {
            val recordId = mealRecordRepository.save(mealRecord()).id
            val later = mealFood(mealRecordId = recordId, eatenAt = LocalDateTime.of(2026, 6, 11, 13, 0))
            val earlier = mealFood(mealRecordId = recordId, eatenAt = LocalDateTime.of(2026, 6, 11, 12, 0))
            mealFoodRepository.saveAll(listOf(later, earlier))

            val result = mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(recordId)

            assertThat(result.map { it.eatenAt }).containsExactly(
                LocalDateTime.of(2026, 6, 11, 12, 0),
                LocalDateTime.of(2026, 6, 11, 13, 0),
            )
        }

        @Test
        fun `mealRecordId 기준 음식 개수를 센다`() {
            val recordId = mealRecordRepository.save(mealRecord()).id
            mealFoodRepository.saveAll(
                listOf(
                    mealFood(mealRecordId = recordId),
                    mealFood(mealRecordId = recordId, foodId = 2L),
                ),
            )

            val count = mealFoodRepository.countByMealRecordId(recordId)

            assertThat(count).isEqualTo(2)
        }
    }

    private fun mealRecord(
        id: UUID = UUID.randomUUID(),
        userId: Long = 1L,
        eatenAt: LocalDateTime = LocalDateTime.of(2026, 6, 11, 12, 30),
    ) = MealRecord(id = id, userId = userId, eatenAt = eatenAt)

    private fun mealFood(
        userId: Long = 1L,
        foodId: Long = 1L,
        mealRecordId: UUID,
        eatenAt: LocalDateTime = LocalDateTime.of(2026, 6, 11, 12, 30),
    ) = MealFood(
        userId = userId,
        foodId = foodId,
        mealRecordId = mealRecordId,
        eatenAt = eatenAt,
        judgedGrade = JudgmentGrade.RECOMMEND,
    )
}
