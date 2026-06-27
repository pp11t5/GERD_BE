package com.gerd.domain.meal.repository

import com.gerd.domain.auth.entity.User
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
import jakarta.persistence.EntityManager
import java.time.LocalDateTime

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class)
class MealRecordRepositoryTest @Autowired constructor(
    private val mealFoodRepository: MealFoodRepository,
    private val mealRecordRepository: MealRecordRepository,
    private val em: EntityManager,
) {

    @Nested
    inner class `MealFood 조회` {

        @Test
        fun `mealRecordId로 소속 음식을 먹은 시각 오름차순 조회한다`() {
            val user = saveUser()
            val record = mealRecordRepository.save(mealRecord(user = user))
            val later = mealFood(user = user, mealRecord = record, eatenAt = LocalDateTime.of(2026, 6, 11, 13, 0))
            val earlier = mealFood(user = user, mealRecord = record, eatenAt = LocalDateTime.of(2026, 6, 11, 12, 0))
            mealFoodRepository.saveAll(listOf(later, earlier))

            val result = mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(record.id!!)

            assertThat(result.map { it.eatenAt }).containsExactly(
                LocalDateTime.of(2026, 6, 11, 12, 0),
                LocalDateTime.of(2026, 6, 11, 13, 0),
            )
        }

        @Test
        fun `mealRecordId 기준 음식 개수를 센다`() {
            val user = saveUser(email = "count@test.com")
            val record = mealRecordRepository.save(mealRecord(user = user))
            mealFoodRepository.saveAll(
                listOf(
                    mealFood(user = user, mealRecord = record),
                    mealFood(user = user, mealRecord = record, foodId = 2L),
                ),
            )

            val count = mealFoodRepository.countByMealRecordId(record.id!!)

            assertThat(count).isEqualTo(2)
        }
    }

    private fun saveUser(email: String = "user@test.com"): User =
        User(email = email).also {
            em.persist(it)
            em.flush()
        }

    private fun mealRecord(
        user: User,
        eatenAt: LocalDateTime = LocalDateTime.of(2026, 6, 11, 12, 30),
    ) = MealRecord(user = user, eatenAt = eatenAt)

    private fun mealFood(
        user: User,
        foodId: Long = 1L,
        mealRecord: MealRecord,
        eatenAt: LocalDateTime = LocalDateTime.of(2026, 6, 11, 12, 30),
    ) = MealFood(
        user = user,
        foodId = foodId,
        mealRecord = mealRecord,
        eatenAt = eatenAt,
        judgedGrade = JudgmentGrade.RECOMMEND,
    )
}
