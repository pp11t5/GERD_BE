package com.gerd.domain.auth.service

import com.gerd.domain.auth.client.KakaoApiClient
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.RefreshTokenRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 탈퇴를 처리하는 서비스
 */
@Service
@Transactional
class WithdrawService(
    private val userRepository: UserRepository,
    private val authAccountRepository: AuthAccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val kakaoApiClient: KakaoApiClient,
) {

    // status = DELETED로 접근 차단 + deleted_at 기록 + 모든 기기 로그아웃
    fun withdraw(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        user.withdraw()

        // 모든 기기 refresh token 즉시 만료
        refreshTokenRepository.deleteAllByUserId(userId)
    }

    // 14일 유예 후 스케줄러에서 호출 — 카카오 연동이 있으면 unlink 후 물리 삭제
    fun withdrawHardDelete(userId: Long) {
        authAccountRepository.findById(userId)
            .filter { it.provider == AuthProvider.KAKAO }
            .ifPresent { kakaoApiClient.unlink(it.providerAccountId) }

        userRepository.hardDelete(userId)
    }
}
