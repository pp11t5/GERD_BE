package com.gerd.domain.auth.repository

import com.gerd.domain.auth.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun findByNickname(nickname: String): Optional<User>

    // @SQLRestriction 우회 — 탈퇴 유예(DELETED) 유저 조회 전용 (복구 흐름)
    @Query(value = "SELECT * FROM users WHERE user_id = :userId", nativeQuery = true)
    fun findByIdIncludingDeleted(@Param("userId") userId: Long): Optional<User>

    // @SQLRestriction 우회해 DB에서 물리 삭제 — 14일 유예 후 스케줄러에서 호출
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :userId")
    fun hardDelete(@Param("userId") userId: Long)
}
