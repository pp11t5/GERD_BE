package com.gerd.domain.auth.service

import com.gerd.domain.auth.entity.AuthAccount
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * OAuth 인증 시 사용자 및 연동 계정 생성 및 조회
 * REQUIRES_NEW로 독립적인 트랜잭션 실행, 실패해도 롤백 보장
 */
@Service
class UserAccountRegistrar(
    private val userRepository: UserRepository,
    private val authAccountRepository: AuthAccountRepository,
    private val notificationSettingRepository: UserNotificationSettingRepository,
) {

    // 필요 시에만 사용자 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun findOrRegister(
        email: String,
        provider: AuthProvider,
        providerAccountId: String,
        buildUser: () -> User,
    ): Long {
        val user = findOrCreateUser(email, buildUser)
        findOrCreateAuthAccount(user, provider, providerAccountId)
        return user.id!!
    }

    // 사용자 검색 또는 새로 저장 — 신규 가입이면 알림 설정 기본값도 함께 생성
    private fun findOrCreateUser(email: String, buildUser: () -> User): User {
        return userRepository.findByEmail(email).orElseGet {
            val user = userRepository.save(buildUser())
            notificationSettingRepository.save(UserNotificationSetting(user = user))
            user
        }
    }

    // 사용자 계정 엔티티 생성
    private fun findOrCreateAuthAccount(user: User, provider: AuthProvider, providerAccountId: String) {
        if (authAccountRepository.existsByProviderAndProviderAccountId(provider, providerAccountId)) return
        authAccountRepository.save(AuthAccount(user = user, provider = provider, providerAccountId = providerAccountId))
    }
}
