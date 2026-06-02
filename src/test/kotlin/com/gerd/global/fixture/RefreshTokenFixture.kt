package com.gerd.global.fixture

import com.gerd.domain.auth.entity.RefreshToken
import com.gerd.domain.auth.util.HashUtils
import java.time.LocalDateTime

object RefreshTokenFixture {

    fun storedToken(
        jti: String = "test-jti",
        tokenValue: String = "refresh.token",
        userId: Long = 1L,
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(3),
    ) = RefreshToken(
        jti = jti,
        tokenHash = HashUtils.sha256(tokenValue),
        userId = userId,
        expiresAt = expiresAt,
    )

    fun storedAdminToken(
        jti: String = "admin-jti",
        tokenValue: String = "admin.refresh.token",
    ) = storedToken(jti = jti, tokenValue = tokenValue, userId = 3L)
}
