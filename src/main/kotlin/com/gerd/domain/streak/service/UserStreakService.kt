package com.gerd.domain.streak.service

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.streak.dto.StreakCharacterStatus
import com.gerd.domain.streak.dto.UserStreakResponseDTO
import com.gerd.domain.streak.entity.UserStreak
import com.gerd.domain.streak.repository.UserStreakRepository
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UserStreakService(
    private val userStreakRepository: UserStreakRepository,
    private val userRepository: UserRepository,
    private val symptomRepository: SymptomRepository,
    private val mealRecordRepository: MealRecordRepository,
) {

    // 한번에 스트릭을 계산할 batch size
    companion object {
        private const val REBUILD_BATCH_SIZE = 50
        private const val GRATEFUL_STREAK_DAYS = 7
    }

    // 사용자의 현재 편안한 증상 기록 연속일과 캐릭터 상태를 조회
    fun getStreak(userId: Long): UserStreakResponseDTO {
        val today = LocalDate.now()
        val userStreak = userStreakRepository.findByIdOrNull(userId)
        val streak = userStreak?.currentStreakOn(today) ?: 0

        return UserStreakResponseDTO(
            streak = streak,
            characterStatus = resolveCharacterStatus(userId, streak),
        )
    }

    // 스트릭 값으로 캐릭터 상태 분기, 스트릭이 0일 때만 최근 증상을 확인
    private fun resolveCharacterStatus(
        userId: Long,
        streak: Int,
    ): StreakCharacterStatus {
        if (streak >= GRATEFUL_STREAK_DAYS) return StreakCharacterStatus.GRATEFUL
        if (streak > 0) return StreakCharacterStatus.SATISFIED

        val latestSymptom = symptomRepository.findFirstByUser_IdOrderByOccurredAtDescIdDesc(userId)
        if (latestSymptom == null && !mealRecordRepository.existsByUser_Id(userId)) {
            return StreakCharacterStatus.GREETING
        }

        return when (latestSymptom?.symptomState) {
            SymptomState.UNCOMFORTABLE, SymptomState.SEVERE -> StreakCharacterStatus.TERRIBLE
            else -> StreakCharacterStatus.SATISFIED
        }
    }

    // 사용자가 오늘 편안한 증상 기록을 남겼을 때 연속일을 업데이트
    @Transactional
    fun updateOnComfortableRecorded(userId: Long, recordDate: LocalDate) {
        if (recordDate != LocalDate.now()) {
            rebuildCurrentStreak(userId)
            return
        }
        // 스트릭이 없으면 생성, 있으면 update
        val userStreak = findOrCreateForUpdate(userId)
        // 오늘 편안한 증상 기록을 남겼으므로 연속일을 업데이트
        userStreak.recordComfortableOn(recordDate)
        userStreakRepository.save(userStreak)
    }

    // 사용자가 오늘 편안한 증상 기록을 삭제했을 때 연속일을 재계산
    @Transactional
    fun rebuildCurrentStreak(userId: Long) {
        symptomRepository.flush()
        val today = LocalDate.now()
        val result = calculateCurrentStreak(userId, today)
        val userStreak = findOrCreateForUpdate(userId)

        userStreak.replace(
            currentStreak = result.currentStreak,
            lastComfortableDate = result.lastComfortableDate,
        )
        userStreakRepository.save(userStreak)
    }

    private fun findOrCreateForUpdate(userId: Long): UserStreak {
        val user = userRepository.findByIdForUpdate(userId)
            ?: throw GeneralException(AuthErrorCode.USER_NOT_FOUND)

        return userStreakRepository.findByUserIdForUpdate(userId)
            ?: UserStreak(user = user)
    }

    // 현재 스트릭 계산, 재계산이 필요할 때만 호출
    private fun calculateCurrentStreak(userId: Long, today: LocalDate): StreakCalculationResult {
        var beforeDate = today.plusDays(1)
        var expectedDate: LocalDate? = null
        var latestDate: LocalDate? = null
        var streak = 0

        // 스트릭 계산을 위해 편안한 증상 기록 날짜를 배치 단위로 조회, 중간에 streak이 끊기면 바로 반환
        while (true) {
            val dates = symptomRepository.findComfortableRecordDatesBefore(userId, beforeDate, REBUILD_BATCH_SIZE)
            if (dates.isEmpty()) {
                return StreakCalculationResult(currentStreak = streak, lastComfortableDate = latestDate)
            }

            if (expectedDate == null) {
                latestDate = dates.first()
                if (latestDate != today && latestDate != today.minusDays(1)) {
                    return StreakCalculationResult(currentStreak = 0, lastComfortableDate = latestDate)
                }
                expectedDate = latestDate
            }

            for (date in dates) {
                if (date != expectedDate) {
                    return StreakCalculationResult(currentStreak = streak, lastComfortableDate = latestDate)
                }
                streak += 1
                expectedDate = expectedDate.minusDays(1)
            }

            beforeDate = dates.last()
        }
    }

    // 오늘 기준으로 현재 스트릭 계산
    private fun UserStreak.currentStreakOn(today: LocalDate): Int =
        if (lastComfortableDate == today || lastComfortableDate == today.minusDays(1)) {
            currentStreak
        } else {
            0
        }

    // 스트릭 계산
    private data class StreakCalculationResult(
        val currentStreak: Int,
        val lastComfortableDate: LocalDate?,
    )

}
