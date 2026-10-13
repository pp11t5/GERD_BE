package com.gerd.domain.streak.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.streak.dto.StreakCharacterStatus
import com.gerd.domain.streak.entity.UserStreak
import com.gerd.domain.streak.repository.UserStreakRepository
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.fixture.SymptomFixture
import com.gerd.global.fixture.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserStreakServiceTest {

    @Mock
    private lateinit var userStreakRepository: UserStreakRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var symptomRepository: SymptomRepository

    @Mock
    private lateinit var mealRecordRepository: MealRecordRepository

    private val service by lazy {
        UserStreakService(
            userStreakRepository = userStreakRepository,
            userRepository = userRepository,
            symptomRepository = symptomRepository,
            mealRecordRepository = mealRecordRepository,
        )
    }

    private val user = UserFixture.user()
    private val userId = 1L

    @Nested
    inner class `스트릭 조회` {

        @Test
        fun `스트릭 row가 없으면 0을 반환한다`() {
            whenever(userStreakRepository.findById(userId)).thenReturn(Optional.empty())

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(0)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GREETING)
        }

        @Test
        fun `마지막 편안 기록일이 오늘이면 현재 스트릭을 반환한다`() {
            val today = LocalDate.now()
            val userStreak = UserStreak(user = user, currentStreak = 3, lastComfortableDate = today)
            whenever(userStreakRepository.findById(userId)).thenReturn(Optional.of(userStreak))

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(3)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `마지막 편안 기록일이 이틀 전이면 0을 반환한다`() {
            val twoDaysAgo = LocalDate.now().minusDays(2)
            val userStreak = UserStreak(user = user, currentStreak = 3, lastComfortableDate = twoDaysAgo)
            whenever(userStreakRepository.findById(userId)).thenReturn(Optional.of(userStreak))

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(0)
        }
    }

    @Nested
    inner class `캐릭터 상태 - 스트릭이 있는 경우` {

        private fun stubStreak(
            currentStreak: Int,
            lastComfortableDate: LocalDate = LocalDate.now(),
        ) {
            val userStreak =
                UserStreak(
                    user = user,
                    currentStreak = currentStreak,
                    lastComfortableDate = lastComfortableDate,
                )
            whenever(userStreakRepository.findById(userId)).thenReturn(Optional.of(userStreak))
        }

        @Test
        fun `스트릭이 1일이면 SATISFIED를 반환한다`() {
            stubStreak(currentStreak = 1)

            val result = service.getStreak(userId)

            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `스트릭이 6일이면 SATISFIED를 반환한다`() {
            stubStreak(currentStreak = 6)

            val result = service.getStreak(userId)

            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `스트릭이 7일이면 GRATEFUL을 반환한다`() {
            stubStreak(currentStreak = 7)

            val result = service.getStreak(userId)

            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GRATEFUL)
        }

        @Test
        fun `스트릭이 7일을 넘어도 GRATEFUL을 반환한다`() {
            stubStreak(currentStreak = 30)

            val result = service.getStreak(userId)

            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GRATEFUL)
        }

        @Test
        fun `마지막 편안 기록일이 어제여도 스트릭이 7일이면 GRATEFUL을 반환한다`() {
            stubStreak(currentStreak = 7, lastComfortableDate = LocalDate.now().minusDays(1))

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(7)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GRATEFUL)
        }

        @Test
        fun `스트릭이 1일 이상이면 최근 증상과 식사 기록을 조회하지 않는다`() {
            stubStreak(currentStreak = 1)

            service.getStreak(userId)

            verify(symptomRepository, never()).findFirstByUser_IdOrderByOccurredAtDescIdDesc(any())
            verify(mealRecordRepository, never()).existsByUser_Id(any())
        }

        @Test
        fun `스트릭이 7일 이상이어도 최근 증상과 식사 기록을 조회하지 않는다`() {
            stubStreak(currentStreak = 7)

            service.getStreak(userId)

            verify(symptomRepository, never()).findFirstByUser_IdOrderByOccurredAtDescIdDesc(any())
            verify(mealRecordRepository, never()).existsByUser_Id(any())
        }
    }

    @Nested
    inner class `캐릭터 상태 - 스트릭이 0인 경우` {

        private fun stubLatestSymptom(state: SymptomState) {
            whenever(symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(SymptomFixture.symptom(symptomState = state))
        }

        @Test
        fun `최근 증상이 불편이면 TERRIBLE을 반환한다`() {
            stubLatestSymptom(SymptomState.UNCOMFORTABLE)

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(0)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.TERRIBLE)
        }

        @Test
        fun `최근 증상이 심함이면 TERRIBLE을 반환한다`() {
            stubLatestSymptom(SymptomState.SEVERE)

            val result = service.getStreak(userId)

            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.TERRIBLE)
        }

        @ParameterizedTest
        @EnumSource(
            value = SymptomState::class,
            names = ["COMFORTABLE", "GOOD", "NORMAL"],
        )
        fun `최근 증상이 불편과 심함이 아니면 SATISFIED를 반환한다`(state: SymptomState) {
            stubLatestSymptom(state)

            val result = service.getStreak(userId)

            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `최근 증상이 있으면 식사 기록 존재 여부는 조회하지 않는다`() {
            stubLatestSymptom(SymptomState.NORMAL)

            service.getStreak(userId)

            verify(mealRecordRepository, never()).existsByUser_Id(any())
        }

        @Test
        fun `증상 기록은 없고 식사 기록만 있으면 SATISFIED를 반환한다`() {
            whenever(symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(userId)).thenReturn(null)
            whenever(mealRecordRepository.existsByUser_Id(userId)).thenReturn(true)

            val result = service.getStreak(userId)

            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `증상 기록과 식사 기록이 모두 없으면 GREETING을 반환한다`() {
            whenever(symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(userId)).thenReturn(null)
            whenever(mealRecordRepository.existsByUser_Id(userId)).thenReturn(false)

            val result = service.getStreak(userId)

            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GREETING)
        }

        @Test
        fun `스트릭 row가 없고 기록도 없는 신규 사용자는 GREETING을 반환한다`() {
            whenever(userStreakRepository.findById(userId)).thenReturn(Optional.empty())

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(0)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GREETING)
        }
    }

    @Nested
    inner class `캐릭터 상태 - 만료된 스트릭` {

        private fun stubExpiredStreak(currentStreak: Int) {
            val userStreak =
                UserStreak(
                    user = user,
                    currentStreak = currentStreak,
                    lastComfortableDate = LocalDate.now().minusDays(2),
                )
            whenever(userStreakRepository.findById(userId)).thenReturn(Optional.of(userStreak))
        }

        @Test
        fun `저장된 스트릭이 7일이어도 이틀 전 기록이면 GRATEFUL이 아니다`() {
            stubExpiredStreak(currentStreak = 7)

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(0)
            assertThat(result.characterStatus).isNotEqualTo(StreakCharacterStatus.GRATEFUL)
        }

        @Test
        fun `만료된 스트릭에서 최근 증상이 불편이면 TERRIBLE을 반환한다`() {
            stubExpiredStreak(currentStreak = 7)
            whenever(symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(SymptomFixture.symptom(symptomState = SymptomState.UNCOMFORTABLE))

            val result = service.getStreak(userId)

            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.TERRIBLE)
        }

        @Test
        fun `만료된 스트릭에서 최근 증상이 편안이면 SATISFIED를 반환한다`() {
            stubExpiredStreak(currentStreak = 3)
            whenever(symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(SymptomFixture.symptom(symptomState = SymptomState.COMFORTABLE))

            val result = service.getStreak(userId)

            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }
    }

    @Nested
    inner class `편안 기록 생성 반영` {

        @Test
        fun `오늘 첫 편안 기록이면 스트릭 row를 생성하고 1일로 시작한다`() {
            val today = LocalDate.now()
            whenever(userRepository.findByIdForUpdate(userId)).thenReturn(user)
            whenever(userStreakRepository.findByUserIdForUpdate(userId)).thenReturn(null)
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            service.updateOnComfortableRecorded(userId, today)

            val userStreakCaptor = argumentCaptor<UserStreak>()
            verify(userStreakRepository).save(userStreakCaptor.capture())
            verify(userRepository).findByIdForUpdate(userId)
            assertThat(userStreakCaptor.firstValue.currentStreak).isEqualTo(1)
            assertThat(userStreakCaptor.firstValue.lastComfortableDate).isEqualTo(today)
        }

        @Test
        fun `어제까지 이어진 상태에서 오늘 기록하면 스트릭을 1 증가시킨다`() {
            val today = LocalDate.now()
            val userStreak = UserStreak(user = user, currentStreak = 2, lastComfortableDate = today.minusDays(1))
            whenever(userRepository.findByIdForUpdate(userId)).thenReturn(user)
            whenever(userStreakRepository.findByUserIdForUpdate(userId)).thenReturn(userStreak)
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            service.updateOnComfortableRecorded(userId, today)

            assertThat(userStreak.currentStreak).isEqualTo(3)
            assertThat(userStreak.lastComfortableDate).isEqualTo(today)
            verify(userRepository, never()).getReferenceById(any())
        }

        @Test
        fun `과거 날짜 기록이면 현재 편안 기록 날짜 기준으로 재계산한다`() {
            val today = LocalDate.now()
            val userStreak = UserStreak(user = user, currentStreak = 1, lastComfortableDate = today)
            whenever(symptomRepository.findComfortableRecordDatesBefore(userId, today.plusDays(1), 50))
                .thenReturn(listOf(today, today.minusDays(1), today.minusDays(2)))
            whenever(symptomRepository.findComfortableRecordDatesBefore(userId, today.minusDays(2), 50))
                .thenReturn(emptyList())
            whenever(userRepository.findByIdForUpdate(userId)).thenReturn(user)
            whenever(userStreakRepository.findByUserIdForUpdate(userId)).thenReturn(userStreak)
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            service.updateOnComfortableRecorded(userId, today.minusDays(1))

            assertThat(userStreak.currentStreak).isEqualTo(3)
            assertThat(userStreak.lastComfortableDate).isEqualTo(today)
        }
    }

    @Nested
    inner class `스트릭 재계산` {

        @Test
        fun `오늘이나 어제부터 이어지는 편안 기록이 없으면 0으로 저장한다`() {
            val today = LocalDate.now()
            val userStreak = UserStreak(user = user, currentStreak = 5, lastComfortableDate = today.minusDays(1))
            whenever(symptomRepository.findComfortableRecordDatesBefore(userId, today.plusDays(1), 50))
                .thenReturn(listOf(today.minusDays(2)))
            whenever(userRepository.findByIdForUpdate(userId)).thenReturn(user)
            whenever(userStreakRepository.findByUserIdForUpdate(userId)).thenReturn(userStreak)
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            service.rebuildCurrentStreak(userId)

            assertThat(userStreak.currentStreak).isEqualTo(0)
            assertThat(userStreak.lastComfortableDate).isEqualTo(today.minusDays(2))
        }

        @Test
        fun `중간 날짜에서 끊기면 끊기기 전까지의 스트릭만 저장한다`() {
            val today = LocalDate.now()
            val userStreak = UserStreak(user = user, currentStreak = 5, lastComfortableDate = today)
            whenever(symptomRepository.findComfortableRecordDatesBefore(userId, today.plusDays(1), 50))
                .thenReturn(listOf(today, today.minusDays(1), today.minusDays(3)))
            whenever(userRepository.findByIdForUpdate(userId)).thenReturn(user)
            whenever(userStreakRepository.findByUserIdForUpdate(userId)).thenReturn(userStreak)
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            service.rebuildCurrentStreak(userId)

            assertThat(userStreak.currentStreak).isEqualTo(2)
            assertThat(userStreak.lastComfortableDate).isEqualTo(today)
        }
    }

    @Nested
    inner class `캐릭터 상태 - 스트릭 누적 흐름` {

        private var stored: UserStreak? = null

        private fun stubStore(initial: UserStreak?) {
            stored = initial
            lenient().`when`(userRepository.findByIdForUpdate(userId)).thenReturn(user)
            lenient().`when`(userStreakRepository.findByUserIdForUpdate(userId)).thenAnswer { stored }
            lenient().`when`(userStreakRepository.save(any())).thenAnswer {
                stored = it.arguments[0] as UserStreak
                stored
            }
            whenever(userStreakRepository.findById(userId)).thenAnswer { Optional.ofNullable(stored) }
        }

        private fun stubComfortableDates(
            today: LocalDate,
            dayCount: Int,
            brokenAfter: Int? = null,
        ) {
            val dates = (0 until dayCount).map { today.minusDays(it.toLong()) }
            val streakDates = if (brokenAfter == null) dates else dates.take(brokenAfter) + today.minusDays(brokenAfter + 1L)
            whenever(symptomRepository.findComfortableRecordDatesBefore(userId, today.plusDays(1), 50))
                .thenReturn(streakDates)
            if (brokenAfter == null) {
                whenever(symptomRepository.findComfortableRecordDatesBefore(userId, streakDates.last(), 50))
                    .thenReturn(emptyList())
            }
        }

        @Test
        fun `7일 연속으로 하루씩 기록하면 7일째에 GRATEFUL이 된다`() {
            val today = LocalDate.now()
            val tracked = UserStreak(user = user)
            (6L downTo 0L).forEach { daysAgo ->
                tracked.recordComfortableOn(today.minusDays(daysAgo))
            }
            stubStore(tracked)

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(7)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GRATEFUL)
        }

        @Test
        fun `6일 연속으로 기록하면 아직 SATISFIED이다`() {
            val today = LocalDate.now()
            val tracked = UserStreak(user = user)
            (5L downTo 0L).forEach { daysAgo ->
                tracked.recordComfortableOn(today.minusDays(daysAgo))
            }
            stubStore(tracked)

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(6)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `어제까지 6일이던 사용자가 오늘 편안 기록을 남기면 GRATEFUL이 된다`() {
            val today = LocalDate.now()
            stubStore(UserStreak(user = user, currentStreak = 6, lastComfortableDate = today.minusDays(1)))

            service.updateOnComfortableRecorded(userId, today)
            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(7)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GRATEFUL)
        }

        @Test
        fun `어제까지 5일이던 사용자가 오늘 편안 기록을 남겨도 6일이면 SATISFIED이다`() {
            val today = LocalDate.now()
            stubStore(UserStreak(user = user, currentStreak = 5, lastComfortableDate = today.minusDays(1)))

            service.updateOnComfortableRecorded(userId, today)
            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(6)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `오늘 이미 기록한 사용자가 다시 기록해도 스트릭이 늘지 않아 SATISFIED이다`() {
            val today = LocalDate.now()
            stubStore(UserStreak(user = user, currentStreak = 6, lastComfortableDate = today))

            service.updateOnComfortableRecorded(userId, today)
            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(6)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `편안 기록 7일 연속을 재계산하면 GRATEFUL이 된다`() {
            val today = LocalDate.now()
            stubStore(UserStreak(user = user, currentStreak = 1, lastComfortableDate = today))
            stubComfortableDates(today, dayCount = 7)

            service.rebuildCurrentStreak(userId)
            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(7)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.GRATEFUL)
        }

        @Test
        fun `편안 기록 6일 연속을 재계산하면 SATISFIED이다`() {
            val today = LocalDate.now()
            stubStore(UserStreak(user = user, currentStreak = 7, lastComfortableDate = today))
            stubComfortableDates(today, dayCount = 6)

            service.rebuildCurrentStreak(userId)
            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(6)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `7일째에 기록이 비면 6일까지만 인정되어 SATISFIED이다`() {
            val today = LocalDate.now()
            stubStore(UserStreak(user = user, currentStreak = 7, lastComfortableDate = today))
            stubComfortableDates(today, dayCount = 7, brokenAfter = 6)

            service.rebuildCurrentStreak(userId)
            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(6)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `7일 연속이던 기록 중 하루가 삭제되어 재계산되면 GRATEFUL이 해제된다`() {
            val today = LocalDate.now()
            stubStore(UserStreak(user = user, currentStreak = 7, lastComfortableDate = today))
            stubComfortableDates(today, dayCount = 3, brokenAfter = 3)

            service.rebuildCurrentStreak(userId)
            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(3)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.SATISFIED)
        }

        @Test
        fun `7일 연속이던 편안 기록이 모두 사라져 스트릭이 0이 되면 최근 증상으로 분기한다`() {
            val today = LocalDate.now()
            stubStore(UserStreak(user = user, currentStreak = 7, lastComfortableDate = today))
            whenever(symptomRepository.findComfortableRecordDatesBefore(userId, today.plusDays(1), 50))
                .thenReturn(emptyList())
            whenever(symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(SymptomFixture.symptom(symptomState = SymptomState.UNCOMFORTABLE))

            service.rebuildCurrentStreak(userId)
            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(0)
            assertThat(result.characterStatus).isEqualTo(StreakCharacterStatus.TERRIBLE)
        }
    }
}
