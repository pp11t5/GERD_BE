package com.gerd.domain.streak.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.domain.streak.dto.StreakCharacterStatus
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
@Import(QuerydslTestConfig::class, UserStreakService::class)
class UserStreakServiceIntegrationTest
    @Autowired
    constructor(
        private val userStreakService: UserStreakService,
        private val em: EntityManager,
    ) {
        private val today = LocalDate.now()

        @Nested
        inner class `편안 기록이 쌓였을 때 캐릭터 상태` {
            @Test
            fun `편안 기록이 7일 연속이면 GRATEFUL이다`() {
                val user = saveUser()
                (0L..6L).forEach { saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(it)) }

                val result = rebuildAndGet(user)

                assertThat(result.streak).isEqualTo(7)
                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GRATEFUL)
            }

            @Test
            fun `편안 기록이 어제까지 7일 연속이어도 GRATEFUL이다`() {
                val user = saveUser()
                (1L..7L).forEach { saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(it)) }

                val result = rebuildAndGet(user)

                assertThat(result.streak).isEqualTo(7)
                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GRATEFUL)
            }

            @Test
            fun `편안 기록이 6일 연속이면 SATISFIED이다`() {
                val user = saveUser()
                (0L..5L).forEach { saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(it)) }

                val result = rebuildAndGet(user)

                assertThat(result.streak).isEqualTo(6)
                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
            }

            @Test
            fun `7일 중 하루에 편안 기록이 없으면 스트릭이 끊겨 SATISFIED이다`() {
                val user = saveUser()
                (0L..6L).filter { it != 3L }.forEach { saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(it)) }

                val result = rebuildAndGet(user)

                assertThat(result.streak).isEqualTo(3)
                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
            }

            @Test
            fun `7일 중 하루가 불편 기록뿐이면 스트릭이 끊겨 SATISFIED이다`() {
                val user = saveUser()
                (0L..6L).forEach {
                    val state = if (it == 3L) SymptomState.UNCOMFORTABLE else SymptomState.COMFORTABLE
                    saveSymptom(user, state, today.minusDays(it))
                }

                val result = rebuildAndGet(user)

                assertThat(result.streak).isEqualTo(3)
                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
            }

            @Test
            fun `같은 날 편안 기록과 불편 기록이 함께 있으면 그날은 편안으로 센다`() {
                val user = saveUser()
                (0L..6L).forEach { saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(it)) }
                saveSymptom(user, SymptomState.UNCOMFORTABLE, today.minusDays(3), hour = 18)

                val result = rebuildAndGet(user)

                assertThat(result.streak).isEqualTo(7)
                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GRATEFUL)
            }

            @Test
            fun `7일 연속 기록 중 하루를 삭제하고 재계산하면 GRATEFUL이 해제된다`() {
                val user = saveUser()
                val symptoms = (0L..6L).map { saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(it)) }
                assertThat(rebuildAndGet(user).characterStatus).isEqualTo(StreakCharacterStatus.GRATEFUL)

                em.remove(em.find(Symptom::class.java, symptoms[3].id))
                userStreakService.rebuildCurrentStreak(user.id!!)
                em.flush()
                em.clear()
                val result = userStreakService.getStreak(user.id!!)

                assertThat(result.streak).isEqualTo(3)
                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
            }
        }

        @Nested
        inner class `스트릭이 0일 때 캐릭터 상태` {
            @Test
            fun `최근 편안 기록이 이틀 전이고 가장 최근 증상이 불편이면 TERRIBLE이다`() {
                val user = saveUser()
                saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(2))
                saveSymptom(user, SymptomState.UNCOMFORTABLE, today.minusDays(1))

                val result = rebuildAndGet(user)

                assertThat(result.streak).isEqualTo(0)
                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.TERRIBLE)
            }

            @Test
            fun `가장 최근 증상이 심함이면 TERRIBLE이다`() {
                val user = saveUser()
                saveSymptom(user, SymptomState.COMFORTABLE, today.minusDays(5))
                saveSymptom(user, SymptomState.SEVERE, today)

                val result = rebuildAndGet(user)

                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.TERRIBLE)
            }

            @Test
            fun `삭제된 불편 기록은 무시하고 그 이전 증상으로 판단한다`() {
                val user = saveUser()
                saveSymptom(user, SymptomState.NORMAL, today.minusDays(3))
                saveSymptom(user, SymptomState.SEVERE, today.minusDays(1), deletedAt = LocalDateTime.now())

                val result = rebuildAndGet(user)

                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
            }

            @Test
            fun `증상 기록은 없고 식사 기록만 있으면 SATISFIED이다`() {
                val user = saveUser()
                em.persist(MealRecord(user = user, eatenAt = LocalDateTime.now()))
                em.flush()
                em.clear()

                val result = userStreakService.getStreak(user.id!!)

                assertThat(result.streak).isEqualTo(0)
                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
            }

            @Test
            fun `증상 기록과 식사 기록이 모두 없으면 GREETING이다`() {
                val user = saveUser()
                em.flush()
                em.clear()

                val result = userStreakService.getStreak(user.id!!)

                assertThat(result.streak).isEqualTo(0)
                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GREETING)
            }

            @Test
            fun `삭제된 기록만 있으면 기록이 없는 것으로 보고 GREETING이다`() {
                val user = saveUser()
                saveSymptom(user, SymptomState.COMFORTABLE, today, deletedAt = LocalDateTime.now())
                em.persist(MealRecord(user = user, eatenAt = LocalDateTime.now(), deletedAt = LocalDateTime.now()))
                em.flush()
                em.clear()

                val result = userStreakService.getStreak(user.id!!)

                assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GREETING)
            }
        }

        private fun rebuildAndGet(user: User) =
            run {
                userStreakService.rebuildCurrentStreak(user.id!!)
                em.flush()
                em.clear()
                userStreakService.getStreak(user.id!!)
            }

        private fun saveUser(email: String = "user@test.com"): User =
            User(email = email, nickname = email.substringBefore("@"), role = UserRole.USER).also {
                em.persist(it)
                em.flush()
            }

        private fun saveSymptom(
            user: User,
            state: SymptomState,
            date: LocalDate,
            hour: Int = 12,
            deletedAt: LocalDateTime? = null,
        ): Symptom =
            Symptom(
                user = user,
                symptomState = state,
                symptomTypes = emptySet(),
                occurredAt = date.atTime(hour, 0),
                mealRecordId = null,
                deletedAt = deletedAt,
            ).also { em.persist(it) }
    }
