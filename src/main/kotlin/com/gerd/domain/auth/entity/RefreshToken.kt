package com.gerd.domain.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [Index(name = "idx_refresh_token_hash", columnList = "token_hash")]
)
class RefreshToken(
    // 단일 세션 — userId가 PK이므로 save()가 upsert로 동작
    @Id
    @Column(name = "user_id")
    val userId: Long,

    @Column(length = 36, nullable = false, unique = true)
    var jti: String,

    // sha256(tokenValue) — 원문 유출 방지
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,
)
