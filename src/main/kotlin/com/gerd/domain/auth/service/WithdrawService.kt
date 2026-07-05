package com.gerd.domain.auth.service

import com.gerd.domain.auth.client.KakaoApiClient
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.RefreshTokenRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

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
    // 탈퇴 유예기간 — prod 14일, staging 10분 (application*.yml에서 주입)
    @Value("\${withdraw.grace-period}") private val gracePeriod: Duration,
) {

    // status = DELETED로 접근 차단 + deleted_at 기록 + 모든 기기 로그아웃
    fun withdraw(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        user.withdraw()

        // 모든 기기 refresh token 즉시 만료
        refreshTokenRepository.deleteAllByUserId(userId)
    }

    // 유예기간 후 스케줄러에서 호출 — 카카오 연동이 있으면 unlink 후 물리 삭제
    fun withdrawHardDelete(userId: Long) {
        authAccountRepository.findById(userId)
            .filter { it.provider == AuthProvider.KAKAO }
            .ifPresent { kakaoApiClient.unlink(it.providerAccountId) }

        userRepository.hardDelete(userId)
    }

    // 유예기간이 지난 DELETED 유저 ID를 keyset 페이지네이션으로 배치 조회
    fun findUsersForHardDelete(lastId: Long, limit: Int): List<Long> {
        val threshold = LocalDateTime.now().minus(gracePeriod)
        return userRepository.findIdsForHardDelete(threshold, lastId, limit)
    }
}
