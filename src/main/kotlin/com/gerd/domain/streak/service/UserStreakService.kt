package com.gerd.domain.streak.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.streak.dto.UserStreakResponseDTO
import com.gerd.domain.streak.entity.UserStreak
import com.gerd.domain.streak.repository.UserStreakRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class UserStreakService(
    private val userStreakRepository: UserStreakRepository,
    private val userRepository: UserRepository,
    private val mealRecordRepository: MealRecordRepository,
) {

    fun getStreak(userId: Long): UserStreakResponseDTO {
        val today = LocalDate.now()
        val userStreak = userStreakRepository.findByUser_Id(userId)

        return UserStreakResponseDTO(
            streak = userStreak?.streakCountOn(today) ?: 0,
        )
    }

    @Transactional
    fun updateOnMealRecorded(userId: Long, recordDate: LocalDate): UserStreak {
        val userStreak = userStreakRepository.findByUser_Id(userId)
            ?: UserStreak(user = userRepository.getReferenceById(userId))

        userStreak.recordOn(recordDate)

        return userStreakRepository.save(userStreak)
    }

    @Transactional
    fun refreshAfterMealDeleted(userId: Long): UserStreak? {
        val userStreak = userStreakRepository.findByUser_Id(userId) ?: return null
        val recordDates = mealRecordRepository.findByUser_IdOrderByEatenAtDesc(userId)
            .map { it.eatenAt.toLocalDate() }
            .distinct()

        if (recordDates.isEmpty()) {
            userStreak.replace(streak = 0, lastRecordDate = null)
            return userStreakRepository.save(userStreak)
        }

        val latestDate = recordDates.first()
        val streak = recordDates
            .withIndex()
            .takeWhile { (index, date) -> date == latestDate.minusDays(index.toLong()) }
            .count()

        userStreak.replace(streak = streak, lastRecordDate = latestDate)

        return userStreakRepository.save(userStreak)
    }
}
