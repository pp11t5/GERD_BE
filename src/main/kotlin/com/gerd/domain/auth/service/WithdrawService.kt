package com.gerd.domain.auth.service

import com.gerd.domain.auth.client.KakaoApiClient
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.RefreshTokenRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

@Service
class WithdrawService(
    private val userRepository: UserRepository,
    private val authAccountRepository: AuthAccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val kakaoApiClient: KakaoApiClient,
    @Qualifier("withdrawTaskScheduler") private val taskScheduler: TaskScheduler,
    @Value("\${app.withdraw.grace-period}") private val gracePeriod: Duration,
    @Value("\${app.withdraw.schedule-in-memory}") private val scheduleInMemory: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // status = DELETED로 접근 차단 + deleted_at 기록 + 모든 기기 로그아웃 + 하드 삭제 예약
    @Transactional
    fun withdraw(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        user.withdraw()
        refreshTokenRepository.deleteAllByUserId(userId)

        // prod는 WithDrawScheduler(DB 폴링)가 담당하므로 in-memory 예약 생략
        // afterCommit: 트랜잭션 롤백 시 예약이 남아 정상 유저를 삭제하는 것을 방지
        if (scheduleInMemory) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    taskScheduler.schedule(
                        { runCatching { withdrawHardDelete(userId) }.onFailure { log.error("예약 하드 삭제 실패 userId=$userId", it) } },
                        Instant.now().plus(gracePeriod),
                    )
                }
            })
        }
    }

    fun findUsersForHardDelete(lastId: Long, limit: Int): List<Long> =
        userRepository.findIdsForHardDelete(
            threshold = LocalDateTime.now().minus(gracePeriod),
            lastId = lastId,
            limit = limit,
        )

    // 유예기간 후 스케줄러에서 호출 — 카카오 연동이 있으면 unlink 후 물리 삭제
    // 외부 API(unlink)가 DB 트랜잭션을 점유하지 않도록 의도적으로 비트랜잭션
    internal fun withdrawHardDelete(userId: Long) {
        authAccountRepository.findById(userId)
            .filter { it.provider == AuthProvider.KAKAO }
            .ifPresent { kakaoApiClient.unlink(it.providerAccountId) }

        userRepository.hardDelete(userId)
    }
}
