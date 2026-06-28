package com.gerd.domain.streak.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.streak.entity.UserStreak
import com.gerd.domain.streak.repository.UserStreakRepository
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.fixture.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
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

    private val service by lazy {
        UserStreakService(
            userStreakRepository = userStreakRepository,
            userRepository = userRepository,
            symptomRepository = symptomRepository,
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
        }

        @Test
        fun `마지막 편안 기록일이 오늘이면 현재 스트릭을 반환한다`() {
            val today = LocalDate.now()
            val userStreak = UserStreak(user = user, currentStreak = 3, lastComfortableDate = today)
            whenever(userStreakRepository.findById(userId)).thenReturn(Optional.of(userStreak))

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(3)
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
}
