package com.gerd.domain.streak.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.streak.entity.UserStreak
import com.gerd.domain.streak.repository.UserStreakRepository
import com.gerd.global.fixture.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class UserStreakServiceTest {

    @Mock
    private lateinit var userStreakRepository: UserStreakRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var mealRecordRepository: MealRecordRepository

    private val service by lazy {
        UserStreakService(
            userStreakRepository = userStreakRepository,
            userRepository = userRepository,
            mealRecordRepository = mealRecordRepository,
        )
    }

    private val user = UserFixture.user()
    private val userId = 1L

    @Nested
    inner class `스트릭 조회` {

        @Test
        fun `스트릭 row가 없으면 0을 반환한다`() {
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(null)

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(0)
        }

        @Test
        fun `마지막 기록일이 오늘이면 현재 스트릭을 반환한다`() {
            val today = LocalDate.now()
            val userStreak = UserStreak(user = user, currentStreak = 3, lastRecordDate = today)
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(userStreak)

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(3)
        }

        @Test
        fun `마지막 기록일이 어제이면 현재 스트릭을 반환한다`() {
            val yesterday = LocalDate.now().minusDays(1)
            val userStreak = UserStreak(user = user, currentStreak = 3, lastRecordDate = yesterday)
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(userStreak)

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(3)
        }

        @Test
        fun `마지막 기록일이 이틀 전이면 0을 반환한다`() {
            val twoDaysAgo = LocalDate.now().minusDays(2)
            val userStreak = UserStreak(user = user, currentStreak = 3, lastRecordDate = twoDaysAgo)
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(userStreak)

            val result = service.getStreak(userId)

            assertThat(result.streak).isEqualTo(0)
        }
    }

    @Nested
    inner class `스트릭 갱신` {

        @Test
        fun `기존 row가 없으면 생성하고 1일 스트릭으로 시작한다`() {
            val recordDate = LocalDate.of(2026, 6, 28)
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(null)
            whenever(userRepository.getReferenceById(userId)).thenReturn(user)
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            val result = service.updateOnMealRecorded(userId, recordDate)

            assertThat(result.currentStreak).isEqualTo(1)
            assertThat(result.lastRecordDate).isEqualTo(recordDate)
            verify(userRepository).getReferenceById(userId)
        }

        @Test
        fun `마지막 기록일이 어제이면 스트릭을 1 증가시킨다`() {
            val recordDate = LocalDate.of(2026, 6, 28)
            val userStreak = UserStreak(user = user, currentStreak = 2, lastRecordDate = recordDate.minusDays(1))
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(userStreak)
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            val result = service.updateOnMealRecorded(userId, recordDate)

            assertThat(result.currentStreak).isEqualTo(3)
            assertThat(result.lastRecordDate).isEqualTo(recordDate)
            verify(userRepository, never()).getReferenceById(any())
        }

        @Test
        fun `오늘 이미 기록했다면 스트릭을 중복 증가시키지 않는다`() {
            val recordDate = LocalDate.of(2026, 6, 28)
            val userStreak = UserStreak(user = user, currentStreak = 2, lastRecordDate = recordDate)
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(userStreak)
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            val result = service.updateOnMealRecorded(userId, recordDate)

            assertThat(result.currentStreak).isEqualTo(2)
            assertThat(result.lastRecordDate).isEqualTo(recordDate)
        }

        @Test
        fun `마지막 기록일이 어제가 아니면 1일 스트릭으로 재시작한다`() {
            val recordDate = LocalDate.of(2026, 6, 28)
            val userStreak = UserStreak(user = user, currentStreak = 5, lastRecordDate = recordDate.minusDays(2))
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(userStreak)
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            val result = service.updateOnMealRecorded(userId, recordDate)

            assertThat(result.currentStreak).isEqualTo(1)
            assertThat(result.lastRecordDate).isEqualTo(recordDate)
        }
    }

    @Nested
    inner class `식사 삭제 후 스트릭 재계산` {

        @Test
        fun `남은 식사 기록 날짜 기준으로 현재 스트릭을 다시 계산한다`() {
            val userStreak = UserStreak(
                user = user,
                currentStreak = 3,
                lastRecordDate = LocalDate.of(2026, 6, 29),
            )
            val mealRecords = listOf(
                mealRecord(LocalDate.of(2026, 6, 28)),
                mealRecord(LocalDate.of(2026, 6, 28)),
                mealRecord(LocalDate.of(2026, 6, 27)),
                mealRecord(LocalDate.of(2026, 6, 25)),
            )
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(userStreak)
            whenever(mealRecordRepository.findByUser_IdOrderByEatenAtDesc(userId)).thenReturn(mealRecords)
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            val result = service.refreshAfterMealDeleted(userId)

            assertThat(result?.currentStreak).isEqualTo(2)
            assertThat(result?.lastRecordDate).isEqualTo(LocalDate.of(2026, 6, 28))
        }

        @Test
        fun `남은 식사 기록이 없으면 스트릭을 0으로 초기화한다`() {
            val userStreak = UserStreak(
                user = user,
                currentStreak = 1,
                lastRecordDate = LocalDate.of(2026, 6, 28),
            )
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(userStreak)
            whenever(mealRecordRepository.findByUser_IdOrderByEatenAtDesc(userId)).thenReturn(emptyList())
            whenever(userStreakRepository.save(any())).thenAnswer { it.arguments[0] as UserStreak }

            val result = service.refreshAfterMealDeleted(userId)

            assertThat(result?.currentStreak).isEqualTo(0)
            assertThat(result?.lastRecordDate).isNull()
        }

        @Test
        fun `스트릭 row가 없으면 재계산하지 않는다`() {
            whenever(userStreakRepository.findByUser_Id(userId)).thenReturn(null)

            val result = service.refreshAfterMealDeleted(userId)

            assertThat(result).isNull()
            verify(mealRecordRepository, never()).findByUser_IdOrderByEatenAtDesc(any())
            verify(userStreakRepository, never()).save(any())
        }
    }

    private fun mealRecord(date: LocalDate): MealRecord =
        MealRecord(user = user, eatenAt = date.atTime(12, 0))
}
