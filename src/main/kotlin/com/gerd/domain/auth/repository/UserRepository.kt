package com.gerd.domain.auth.repository

import com.gerd.domain.auth.entity.User
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun findByNickname(nickname: String): Optional<User>

    // @SQLRestriction 우회 — 탈퇴 유예(DELETED) 유저 조회 전용 (복구 흐름)
    @Query(value = "SELECT * FROM users WHERE user_id = :userId", nativeQuery = true)
    fun findByIdIncludingDeleted(@Param("userId") userId: Long): Optional<User>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    fun findByIdForUpdate(@Param("userId") userId: Long): User?

    @Query("SELECT u.id FROM User u WHERE u.id > :lastId ORDER BY u.id")
    fun findIdsAfter(@Param("lastId") lastId: Long, pageable: Pageable): List<Long>

    // @SQLRestriction 우회 — 14일 유예 지난 DELETED 유저 ID를 keyset 페이지네이션으로 배치 조회
    @Query(
        value = "SELECT user_id FROM users WHERE status = 'DELETED' AND deleted_at < :threshold AND user_id > :lastId ORDER BY user_id LIMIT :limit",
        nativeQuery = true,
    )
    fun findIdsForHardDelete(
        @Param("threshold") threshold: LocalDateTime,
        @Param("lastId") lastId: Long,
        @Param("limit") limit: Int,
    ): List<Long>

    // @SQLRestriction 우회해 DB에서 물리 삭제 — 14일 유예 후 스케줄러에서 호출
    // 외부 API 호출(unlink)과 트랜잭션을 분리하기 위해 물리 삭제만 자체 트랜잭션으로 처리
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM users WHERE user_id = :userId", nativeQuery = true)
    fun hardDelete(@Param("userId") userId: Long)
}
