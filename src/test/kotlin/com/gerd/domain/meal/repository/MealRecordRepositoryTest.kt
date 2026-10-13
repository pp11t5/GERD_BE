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
import java.time.LocalDate
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

    @Nested
    inner class `리포트 식사 등급 집계 조회` {

        @Test
        fun `사용자와 기간에 해당하는 끼니 등급만 날짜와 함께 조회한다`() {
            val user = saveUser("report@test.com")
            val otherUser = saveUser("other-report@test.com")
            mealRecordRepository.save(
                mealRecord(
                    user = user,
                    eatenAt = LocalDateTime.of(2026, 6, 21, 8, 0),
                    grade = JudgmentGrade.RECOMMEND,
                ),
            )
            mealRecordRepository.save(
                mealRecord(
                    user = user,
                    eatenAt = LocalDateTime.of(2026, 6, 22, 12, 0),
                    grade = JudgmentGrade.RISK,
                ),
            )
            mealRecordRepository.save(
                mealRecord(
                    user = user,
                    eatenAt = LocalDateTime.of(2026, 6, 28, 8, 0),
                    grade = JudgmentGrade.CAUTION,
                ),
            )
            mealRecordRepository.save(
                mealRecord(
                    user = otherUser,
                    eatenAt = LocalDateTime.of(2026, 6, 22, 12, 0),
                    grade = JudgmentGrade.CAUTION,
                ),
            )
            em.flush()
            em.clear()

            val result = mealRecordRepository.findGradesByUserAndPeriod(
                user.id!!,
                LocalDateTime.of(2026, 6, 21, 0, 0),
                LocalDateTime.of(2026, 6, 27, 23, 59),
            )

            assertThat(result.map { it.date }).containsExactlyInAnyOrder(
                LocalDate.of(2026, 6, 21),
                LocalDate.of(2026, 6, 22),
            )
            assertThat(result.map { it.grade }).containsExactlyInAnyOrder(
                JudgmentGrade.RECOMMEND,
                JudgmentGrade.RISK,
            )
        }
    }

    @Nested
    inner class `식사 기록 존재 여부 조회` {

        @Test
        fun `식사 기록이 있으면 true를 반환한다`() {
            val user = saveUser("exists@test.com")
            mealRecordRepository.save(mealRecord(user = user))
            em.flush()
            em.clear()

            val result = mealRecordRepository.existsByUser_Id(user.id!!)

            assertThat(result).isTrue()
        }

        @Test
        fun `삭제된 식사 기록만 있으면 false를 반환한다`() {
            val user = saveUser("deleted@test.com")
            mealRecordRepository.save(mealRecord(user = user, deletedAt = LocalDateTime.of(2026, 6, 11, 13, 0)))
            em.flush()
            em.clear()

            val result = mealRecordRepository.existsByUser_Id(user.id!!)

            assertThat(result).isFalse()
        }

        @Test
        fun `삭제된 식사와 삭제되지 않은 식사가 함께 있으면 true를 반환한다`() {
            val user = saveUser("mixed@test.com")
            mealRecordRepository.save(mealRecord(user = user, deletedAt = LocalDateTime.of(2026, 6, 11, 13, 0)))
            mealRecordRepository.save(mealRecord(user = user, eatenAt = LocalDateTime.of(2026, 6, 12, 12, 0)))
            em.flush()
            em.clear()

            val result = mealRecordRepository.existsByUser_Id(user.id!!)

            assertThat(result).isTrue()
        }

        @Test
        fun `다른 사용자의 식사 기록만 있으면 false를 반환한다`() {
            val user = saveUser("me@test.com")
            val otherUser = saveUser("other@test.com")
            mealRecordRepository.save(mealRecord(user = otherUser))
            em.flush()
            em.clear()

            val result = mealRecordRepository.existsByUser_Id(user.id!!)

            assertThat(result).isFalse()
        }

        @Test
        fun `식사 기록이 없으면 false를 반환한다`() {
            val user = saveUser("empty@test.com")

            val result = mealRecordRepository.existsByUser_Id(user.id!!)

            assertThat(result).isFalse()
        }
    }

    private fun saveUser(email: String = "user@test.com"): User =
        User(email = email, nickname = email.substringBefore("@")).also {
            em.persist(it)
            em.flush()
        }

    private fun mealRecord(
        user: User,
        eatenAt: LocalDateTime = LocalDateTime.of(2026, 6, 11, 12, 30),
        grade: JudgmentGrade? = null,
        deletedAt: LocalDateTime? = null,
    ) = MealRecord(user = user, eatenAt = eatenAt, grade = grade, deletedAt = deletedAt)

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
