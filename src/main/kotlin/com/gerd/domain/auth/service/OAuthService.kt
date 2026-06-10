package com.gerd.domain.auth.service

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.entity.enums.UserStatus
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.oidc.OidcClaims
import com.gerd.domain.auth.oidc.OidcVerifierRegistry
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * OAuth 관련 비즈니스 로직을 처리하는 서비스
 * - 로그인 처리
 * - 로그인 시 상태분기해서 예외처리
 * - 사용자 생성 처리
 */
@Service
@Transactional(readOnly = true)
class OAuthService(
    private val userRepository: UserRepository,
    private val authAccountRepository: AuthAccountRepository,
    private val authService: AuthService,
    private val oidcVerifierRegistry: OidcVerifierRegistry,
    private val userAccountRegistrar: UserAccountRegistrar,
) {

    // provider별 검증기로 분기 → 기존 계정이면 로그인, 신규면 가입 후 로그인
    @Transactional
    fun socialLogin(
        provider: AuthProvider,
        idToken: String,
    ): AuthTokenResponseDTO {
        val claims = oidcVerifierRegistry.resolve(provider).verify(idToken)

        val authAccount = authAccountRepository
            .findByProviderAndProviderAccountId(provider, claims.sub)
            .orElse(null)

        if (authAccount != null) {
            // 삭제된 사용자도 포함해서 조회
            val user = userRepository.findByIdIncludingDeleted(authAccount.userId!!)
                .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
            checkUserStatus(user)
            // 최근 업로드 시점 저장
            user.updateLastLoginAt()
            return authService.issueTokens(user)
        }

        val email = claims.email ?: throw GeneralException(AuthErrorCode.EMAIL_REQUIRED)
        val nickname = claims.nickname ?: throw GeneralException(AuthErrorCode.NICKNAME_REQUIRED)

        // REQUIRES_NEW 트랜잭션에서 user + authAccount 생성 — 실패 시 이 트랜잭션만 롤백
        val userId = userAccountRegistrar.findOrRegister(
            email = email,
            provider = provider,
            providerAccountId = claims.sub,
        ) { buildUser(claims, email, nickname) }

        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        checkUserStatus(user)
        user.updateLastLoginAt()
        return authService.issueTokens(user)
    }

    // 탈퇴 유예기간 중인 계정 복구 — 같은 idToken으로 바로 재사용 가능
    @Transactional
    fun recoverAccount(
        provider: AuthProvider,
        idToken: String,
    ): AuthTokenResponseDTO {
        val claims = oidcVerifierRegistry.resolve(provider).verify(idToken)

        val authAccount = authAccountRepository
            .findByProviderAndProviderAccountId(provider, claims.sub)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        // @SQLRestriction 우회 — 탈퇴 유예 유저는 일반 조회에서 필터됨
        val user = userRepository.findByIdIncludingDeleted(authAccount.userId!!)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        if (user.status != UserStatus.DELETED) {
            throw GeneralException(AuthErrorCode.USER_NOT_FOUND)
        }

        user.recover()
        return authService.issueTokens(user)
    }

    // 유저 상태 검증 — 로그인 시점에 차단
    // 로그인 시 상태를 검증해서 삭제된 사용자이지만 유예 기간이라면 복구가 가능함을 예외로 던짐
    private fun checkUserStatus(user: User) {
        when (user.status) {
            UserStatus.INACTIVE  -> throw GeneralException(AuthErrorCode.ACCOUNT_INACTIVE)
            UserStatus.DELETED   -> throw GeneralException(AuthErrorCode.ACCOUNT_RECOVERABLE)
            else -> {}
        }
    }

    private fun buildUser(claims: OidcClaims, email: String, nickname: String) = User(
        email = email,
        nickname = nickname,
        profileImage = claims.picture,
    )
}
