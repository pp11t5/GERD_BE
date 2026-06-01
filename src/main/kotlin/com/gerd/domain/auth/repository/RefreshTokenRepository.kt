package com.gerd.domain.auth.repository

import com.gerd.domain.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByTokenHash(tokenHash: String): RefreshToken?
    fun deleteAllByUserId(userId: Long)

    // 만료된 토큰 일괄 정리 (스케줄러용)
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    fun deleteAllExpiredBefore(now: LocalDateTime)
}
