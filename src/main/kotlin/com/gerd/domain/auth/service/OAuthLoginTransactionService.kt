package com.gerd.domain.auth.service

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.entity.enums.UserStatus
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.oidc.OidcClaims
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OAuthLoginTransactionService(
    private val userRepository: UserRepository,
    private val authAccountRepository: AuthAccountRepository,
    private val authService: AuthService,
    private val userAccountRegistrar: UserAccountRegistrar,
    private val nicknameService: NicknameService,
) {

    @Transactional
    fun login(
        provider: AuthProvider,
        claims: OidcClaims,
        encryptedProviderRefreshToken: String? = null,
    ): AuthTokenResponseDTO {
        val response = loginWithClaims(provider, claims)

        if (encryptedProviderRefreshToken != null) {
            val authAccount = authAccountRepository
                .findByProviderAndProviderAccountId(provider, claims.sub)
                .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
            authAccount.updateProviderRefreshToken(encryptedProviderRefreshToken)
        }

        return response
    }

    @Transactional
    fun recover(
        provider: AuthProvider,
        claims: OidcClaims,
    ): AuthTokenResponseDTO {
        val authAccount = authAccountRepository
            .findByProviderAndProviderAccountId(provider, claims.sub)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        val user = userRepository.findByIdIncludingDeleted(authAccount.userId!!)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        if (user.status != UserStatus.DELETED) {
            throw GeneralException(AuthErrorCode.USER_NOT_FOUND)
        }

        user.recover()
        return authService.issueTokens(user)
    }

    private fun loginWithClaims(
        provider: AuthProvider,
        claims: OidcClaims,
    ): AuthTokenResponseDTO {
        val authAccount = authAccountRepository
            .findByProviderAndProviderAccountId(provider, claims.sub)
            .orElse(null)

        if (authAccount != null) {
            val user = userRepository.findByIdIncludingDeleted(authAccount.userId!!)
                .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
            checkUserStatus(user)
            user.updateLastLoginAt()
            return authService.issueTokens(user)
        }

        val email = claims.email ?: throw GeneralException(AuthErrorCode.EMAIL_REQUIRED)
        val userId = userAccountRegistrar.findOrRegister(
            email = email,
            provider = provider,
            providerAccountId = claims.sub,
        ) { buildUser(email) }

        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        checkUserStatus(user)
        user.updateLastLoginAt()
        return authService.issueTokens(user)
    }

    private fun checkUserStatus(user: User) {
        when (user.status) {
            UserStatus.INACTIVE -> throw GeneralException(AuthErrorCode.ACCOUNT_INACTIVE)
            UserStatus.DELETED -> throw GeneralException(AuthErrorCode.ACCOUNT_RECOVERABLE)
            else -> Unit
        }
    }

    private fun buildUser(email: String) =
        User(email = email, nickname = nicknameService.generateUniqueNickname())
}
