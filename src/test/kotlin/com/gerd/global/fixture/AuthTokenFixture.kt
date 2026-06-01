package com.gerd.global.fixture

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.entity.enums.UserRole

object AuthTokenFixture {

    fun userTokenResponse(
        accessToken: String = "access.token",
        refreshToken: String = "refresh.token",
        userId: String = "1",
        email: String = "user@test.com",
        role: UserRole = UserRole.USER,
    ) = AuthTokenResponseDTO(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        email = email,
        role = role,
    )

    fun adminTokenResponse(
        accessToken: String = "admin.access.token",
        refreshToken: String = "admin.refresh.token",
    ) = userTokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = "3",
        email = "admin@test.com",
        role = UserRole.ADMIN,
    )
}
