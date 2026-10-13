package com.gerd.domain.symptom.repository

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.global.config.QuerydslTestConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class)
class SymptomRepositoryStreakTest @Autowired constructor(
    private val symptomRepository: SymptomRepository,
    private val em: EntityManager,
) {

    @Nested
    inner class `편안한 증상 기록 날짜 조회` {

        @Test
        fun `편안한 기록 날짜를 최신순으로 중복 없이 제한 개수만 조회한다`() {
            val user = saveUser()
            val today = LocalDate.of(2026, 6, 28)
            saveSymptom(user, SymptomState.COMFORTABLE, today)
            saveSymptom(user, SymptomState.GOOD, today, today.atTime(18, 0))
            saveSymptom(user, SymptomState.GOOD, today.minusDays(1))
            saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(2))
            saveSymptom(user, SymptomState.UNCOMFORTABLE, today.minusDays(3))
            saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(4))
            flushAndClear()

            val result = symptomRepository.findComfortableRecordDatesBefore(
                userId = user.id!!,
                beforeDate = today.plusDays(1),
                limit = 3,
            )

            assertThat(result).containsExactly(today, today.minusDays(2), today.minusDays(4))
        }

        @Test
        fun `기준 날짜보다 이전의 편안한 기록만 조회한다`() {
            val user = saveUser("before@test.com")
            val today = LocalDate.of(2026, 6, 28)
            saveSymptom(user, SymptomState.COMFORTABLE, today)
            saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(1))
            saveSymptom(user, SymptomState.GOOD, today.minusDays(2))
            flushAndClear()

            val result = symptomRepository.findComfortableRecordDatesBefore(
                userId = user.id!!,
                beforeDate = today,
                limit = 10,
            )

            assertThat(result).containsExactly(today.minusDays(1))
        }

        @Test
        fun `다른 사용자의 편안한 기록은 제외한다`() {
            val user = saveUser("user-a@test.com")
            val otherUser = saveUser("user-b@test.com")
            val today = LocalDate.of(2026, 6, 28)
            saveSymptom(user, SymptomState.COMFORTABLE, today)
            saveSymptom(otherUser, SymptomState.COMFORTABLE, today.minusDays(1))
            saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(2))
            flushAndClear()

            val result = symptomRepository.findComfortableRecordDatesBefore(
                userId = user.id!!,
                beforeDate = today.plusDays(1),
                limit = 10,
            )

            assertThat(result).containsExactly(today, today.minusDays(2))
        }

        @Test
        fun `스트릭 대상이 아닌 기록은 조회하지 않는다`() {
            val user = saveUser("unsafe@test.com")
            val today = LocalDate.of(2026, 6, 28)
            saveSymptom(user, SymptomState.GOOD, today.minusDays(1))
            saveSymptom(user, SymptomState.UNCOMFORTABLE, today)
            saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(2))
            flushAndClear()

            val result = symptomRepository.findComfortableRecordDatesBefore(
                userId = user.id!!,
                beforeDate = today.plusDays(1),
                limit = 10,
            )

            assertThat(result).containsExactly(today.minusDays(2))
        }
    }

    @Nested
    inner class `가장 최근 증상 기록 조회` {

        @Test
        fun `가장 최근 occurredAt의 증상을 반환한다`() {
            val user = saveUser()
            val today = LocalDate.of(2026, 6, 28)
            saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(2))
            saveSymptom(user, SymptomState.SEVERE, today)
            saveSymptom(user, SymptomState.GOOD, today.minusDays(1))
            flushAndClear()

            val result = symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(user.id!!)

            assertThat(result?.symptomState).isEqualTo(SymptomState.SEVERE)
        }

        @Test
        fun `occurredAt이 같으면 id가 큰 증상을 반환한다`() {
            val user = saveUser()
            val occurredAt = LocalDateTime.of(2026, 6, 28, 12, 0)
            saveSymptom(user, SymptomState.COMFORTABLE, occurredAt.toLocalDate(), occurredAt)
            saveSymptom(user, SymptomState.UNCOMFORTABLE, occurredAt.toLocalDate(), occurredAt)
            flushAndClear()

            val result = symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(user.id!!)

            assertThat(result?.symptomState).isEqualTo(SymptomState.UNCOMFORTABLE)
        }

        @Test
        fun `삭제된 증상은 건너뛰고 그 이전 증상을 반환한다`() {
            val user = saveUser()
            val today = LocalDate.of(2026, 6, 28)
            saveSymptom(user, SymptomState.NORMAL, today.minusDays(1))
            saveSymptom(user, SymptomState.SEVERE, today, deletedAt = today.atTime(13, 0))
            flushAndClear()

            val result = symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(user.id!!)

            assertThat(result?.symptomState).isEqualTo(SymptomState.NORMAL)
        }

        @Test
        fun `삭제된 증상만 있으면 null을 반환한다`() {
            val user = saveUser()
            val today = LocalDate.of(2026, 6, 28)
            saveSymptom(user, SymptomState.SEVERE, today, deletedAt = today.atTime(13, 0))
            flushAndClear()

            val result = symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(user.id!!)

            assertThat(result).isNull()
        }

        @Test
        fun `다른 사용자의 증상은 반환하지 않는다`() {
            val user = saveUser("user-a@test.com")
            val otherUser = saveUser("user-b@test.com")
            val today = LocalDate.of(2026, 6, 28)
            saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(1))
            saveSymptom(otherUser, SymptomState.SEVERE, today)
            flushAndClear()

            val result = symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(user.id!!)

            assertThat(result?.symptomState).isEqualTo(SymptomState.COMFORTABLE)
        }

        @Test
        fun `증상이 없으면 null을 반환한다`() {
            val user = saveUser()
            flushAndClear()

            val result = symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(user.id!!)

            assertThat(result).isNull()
        }
    }

    private fun saveUser(email: String = "user@test.com"): User =
        User(email = email, nickname = email.substringBefore("@"), role = UserRole.USER).also {
            em.persist(it)
        }

    private fun saveSymptom(
        user: User,
        state: SymptomState,
        date: LocalDate,
        occurredAt: LocalDateTime = date.atTime(12, 0),
        deletedAt: LocalDateTime? = null,
    ) {
        em.persist(
            Symptom(
                user = user,
                symptomState = state,
                symptomTypes = emptySet(),
                occurredAt = occurredAt,
                mealRecordId = null,
                deletedAt = deletedAt,
            ),
        )
    }

    private fun flushAndClear() {
        em.flush()
        em.clear()
    }
}
