package com.gerd.domain.auth.service

import com.gerd.domain.auth.entity.AuthAccount
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.domain.onboarding.repository.TermRepository
import com.gerd.domain.onboarding.repository.UserConsentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * OAuth 인증 시 사용자 및 연동 계정 생성 및 조회
 * 호출한 OAuth 로그인 트랜잭션에 참여해 계정 생성과 토큰 저장을 원자적으로 처리
 */
@Service
class UserAccountRegistrar(
    private val userRepository: UserRepository,
    private val authAccountRepository: AuthAccountRepository,
    private val notificationSettingRepository: UserNotificationSettingRepository,
    private val termRepository: TermRepository,
    private val userConsentRepository: UserConsentRepository,
) {
    // provider당 신규 유저 생성 — email은 여러 유저가 공유할 수 있어 유저 식별/병합 키로 쓰지 않음
    // (동일 provider 재로그인 여부는 호출 측에서 auth_accounts로 이미 판별한 뒤 이 경로를 탄다)
    @Transactional
    fun findOrRegister(
        email: String,
        provider: AuthProvider,
        providerAccountId: String,
        buildUser: () -> User,
    ): Long {
        val user = createUser(buildUser)
        findOrCreateAuthAccount(user, provider, providerAccountId)
        return user.id!!
    }

    // 신규 유저 저장 — 알림 설정·약관 동의 기본값도 함께 생성
    private fun createUser(buildUser: () -> User): User {
        val user = userRepository.save(buildUser())
        notificationSettingRepository.save(UserNotificationSetting(user = user))
        createDefaultConsents(user)
        return user
    }

    // 최신 약관 전체를 agreed=false로 초기화 — toggleMarketing 등이 레코드 부재로 실패하지 않도록 보장
    private fun createDefaultConsents(user: User) {
        val now = LocalDateTime.now()
        val consents =
            termRepository.findLatestAll().map { term ->
                UserConsent(UserConsentId(user.id!!, term.id), term, agreed = false, agreedAt = now)
            }
        userConsentRepository.saveAll(consents)
    }

    // 사용자 계정 엔티티 생성
    private fun findOrCreateAuthAccount(
        user: User,
        provider: AuthProvider,
        providerAccountId: String,
    ) {
        if (authAccountRepository.existsByProviderAndProviderAccountId(provider, providerAccountId)) return
        authAccountRepository.save(AuthAccount(user = user, provider = provider, providerAccountId = providerAccountId))
    }
}
