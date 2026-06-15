package com.gerd.domain.meal.repository

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.global.config.QuerydslTestConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@Import(QuerydslTestConfig::class)
class MealRecordRepositoryTest @Autowired constructor(
    private val mealRecordRepository: MealRecordRepository,
    private val em: EntityManager,
) {

    private val userId = 1L
    private val otherUserId = 2L

    private fun newRecord(
        eatenAt: LocalDateTime,
        userId: Long = this.userId,
        mealGroupId: UUID = UUID.randomUUID(),
        judgedGrade: JudgmentGrade? = JudgmentGrade.RECOMMEND,
        foodId: Long = 10L,
    ) = MealRecord(
        userId = userId,
        foodId = foodId,
        mealGroupId = mealGroupId,
        eatenAt = eatenAt,
        judgedGrade = judgedGrade,
    )

    @Nested
    inner class `findDailyRecords` {

        @Test
        fun `KST 자정 경계 안의 본인 기록만 eatenAt 오름차순으로 조회한다`() {
            mealRecordRepository.save(newRecord(LocalDateTime.of(2026, 6, 11, 23, 59, 59)))
            mealRecordRepository.save(newRecord(LocalDateTime.of(2026, 6, 11, 0, 0, 0)))
            mealRecordRepository.save(newRecord(LocalDateTime.of(2026, 6, 12, 0, 0, 0))) // 익일 00:00 제외
            mealRecordRepository.save(newRecord(LocalDateTime.of(2026, 6, 10, 23, 59, 59))) // 전날 제외
            mealRecordRepository.save(newRecord(LocalDateTime.of(2026, 6, 11, 12, 0, 0), userId = otherUserId)) // 타 유저 제외
            em.flush()
            em.clear()

            val from = LocalDateTime.of(2026, 6, 11, 0, 0, 0)
            val to = LocalDateTime.of(2026, 6, 12, 0, 0, 0)
            val result = mealRecordRepository.findDailyRecords(userId, from, to)

            assertThat(result.map { it.eatenAt }).containsExactly(
                LocalDateTime.of(2026, 6, 11, 0, 0, 0),
                LocalDateTime.of(2026, 6, 11, 23, 59, 59),
            )
        }

        @Test
        fun `해당 날짜에 기록이 없으면 빈 리스트를 반환한다`() {
            val from = LocalDateTime.of(2026, 6, 11, 0, 0, 0)
            val to = LocalDateTime.of(2026, 6, 12, 0, 0, 0)

            assertThat(mealRecordRepository.findDailyRecords(userId, from, to)).isEmpty()
        }
    }

    @Nested
    inner class `단건 조회와 소유 검증` {

        @Test
        fun `본인 기록은 externalId로 조회되고 타인 기록은 조회되지 않는다`() {
            val saved = mealRecordRepository.save(newRecord(LocalDateTime.of(2026, 6, 11, 12, 30)))
            em.flush()
            val externalId = saved.externalId!!
            em.clear()

            assertThat(mealRecordRepository.findByExternalIdAndUserId(externalId, userId)).isNotNull()
            assertThat(mealRecordRepository.findByExternalIdAndUserId(externalId, otherUserId)).isNull()
        }

        @Test
        fun `끼니 키 존재 여부를 본인 소유 기준으로 판단한다`() {
            val groupId = UUID.randomUUID()
            mealRecordRepository.save(newRecord(LocalDateTime.of(2026, 6, 11, 12, 30), mealGroupId = groupId))
            em.flush()
            em.clear()

            assertThat(mealRecordRepository.existsByUserIdAndMealGroupId(userId, groupId)).isTrue()
            assertThat(mealRecordRepository.existsByUserIdAndMealGroupId(otherUserId, groupId)).isFalse()
            assertThat(mealRecordRepository.existsByUserIdAndMealGroupId(userId, UUID.randomUUID())).isFalse()
        }
    }

    @Nested
    inner class `soft delete` {

        @Test
        fun `삭제하면 조회에서 제외된다`() {
            val saved = mealRecordRepository.save(newRecord(LocalDateTime.of(2026, 6, 11, 12, 30)))
            em.flush()
            val externalId = saved.externalId!!

            mealRecordRepository.delete(saved)
            em.flush()
            em.clear()

            assertThat(mealRecordRepository.findByExternalIdAndUserId(externalId, userId)).isNull()
        }
    }

    @Nested
    inner class `judgedGrade 스냅샷` {

        @Test
        fun `등급 enum을 저장하고 복원한다`() {
            val saved = mealRecordRepository.save(
                newRecord(LocalDateTime.of(2026, 6, 11, 12, 30), judgedGrade = JudgmentGrade.CAUTION),
            )
            em.flush()
            val externalId = saved.externalId!!
            em.clear()

            val found = mealRecordRepository.findByExternalIdAndUserId(externalId, userId)
            assertThat(found?.judgedGrade).isEqualTo(JudgmentGrade.CAUTION)
        }

        @Test
        fun `등급이 null이어도 저장된다`() {
            val saved = mealRecordRepository.save(
                newRecord(LocalDateTime.of(2026, 6, 11, 12, 30), judgedGrade = null),
            )
            em.flush()
            val externalId = saved.externalId!!
            em.clear()

            val found = mealRecordRepository.findByExternalIdAndUserId(externalId, userId)
            assertThat(found?.judgedGrade).isNull()
        }
    }
}
