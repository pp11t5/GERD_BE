package com.gerd.domain.streak.repository

import com.gerd.domain.streak.entity.UserStreak
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserStreakRepository : JpaRepository<UserStreak, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT us FROM UserStreak us WHERE us.userId = :userId")
    fun findByUserIdForUpdate(@Param("userId") userId: Long): UserStreak?
}
