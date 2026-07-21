package com.gerd.domain.auth.service

import com.gerd.domain.auth.client.KakaoApiClient
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.RefreshTokenRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class WithdrawService(
    private val userRepository: UserRepository,
    private val authAccountRepository: AuthAccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val kakaoApiClient: KakaoApiClient,
    private val taskScheduler: TaskScheduler,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // status = DELETED로 접근 차단 + deleted_at 기록 + 모든 기기 로그아웃 + 하드 삭제 예약
    @Transactional
    fun withdraw(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        user.withdraw()
        refreshTokenRepository.deleteAllByUserId(userId)

        taskScheduler.schedule(
            { runCatching { withdrawHardDelete(userId) }.onFailure { log.error("예약 하드 삭제 실패 userId=$userId", it) } },
            Instant.now().plus(GRACE_PERIOD),
        )
    }

    // 유예기간 후 스케줄러에서 호출 — 카카오 연동이 있으면 unlink 후 물리 삭제
    // 외부 API(unlink)가 DB 트랜잭션을 점유하지 않도록 의도적으로 비트랜잭션
    private fun withdrawHardDelete(userId: Long) {
        authAccountRepository.findById(userId)
            .filter { it.provider == AuthProvider.KAKAO }
            .ifPresent { kakaoApiClient.unlink(it.providerAccountId) }

        userRepository.hardDelete(userId)
    }

    companion object {
        // 탈퇴 유예기간 — 정책상 고정값
        val GRACE_PERIOD: Duration = Duration.ofMinutes(10)
    }
}
