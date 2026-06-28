package com.gerd.domain.streak.repository

import com.gerd.domain.streak.entity.UserStreak
import org.springframework.data.jpa.repository.JpaRepository

interface UserStreakRepository : JpaRepository<UserStreak, Long> {
    fun findByUser_Id(userId: Long): UserStreak?
}
