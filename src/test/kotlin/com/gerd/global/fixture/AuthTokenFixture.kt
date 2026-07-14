package com.gerd.global.fixture

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.entity.enums.UserRole

object AuthTokenFixture {

    fun userTokenResponse(
        accessToken: String = "access.token",
        refreshToken: String = "refresh.token",
        userId: String = "1",
        role: UserRole = UserRole.USER,
    ) = AuthTokenResponseDTO(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        role = role,
    )

    fun adminTokenResponse(
        accessToken: String = "admin.access.token",
        refreshToken: String = "admin.refresh.token",
    ) = userTokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = "3",
        role = UserRole.ADMIN,
    )
}
