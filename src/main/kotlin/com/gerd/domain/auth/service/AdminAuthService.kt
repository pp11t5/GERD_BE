package com.gerd.domain.auth.service

import com.gerd.domain.auth.dto.AdminLoginResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.config.properties.AdminProperties
import org.springframework.stereotype.Service

@Service
class AdminAuthService(
    private val adminProperties: AdminProperties,
    private val jwtProvider: JwtProvider,
) {

    fun login(email: String, secret: String): AdminLoginResponseDTO {
        if (adminProperties.email.isBlank() || adminProperties.secret.isBlank()) {
            throw GeneralException(AuthErrorCode.INVALID_ADMIN_CREDENTIALS)
        }
        if (email != adminProperties.email || secret != adminProperties.secret) {
            throw GeneralException(AuthErrorCode.INVALID_ADMIN_CREDENTIALS)
        }
        return AdminLoginResponseDTO(accessToken = jwtProvider.createAdminAccessToken(email))
    }
}
