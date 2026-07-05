package com.gerd.domain.auth.scheduler

import com.gerd.domain.auth.service.WithdrawService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["withdraw.scheduler.enabled"], havingValue = "true", matchIfMissing = false)
class WithDrawScheduler(
    private val withdrawService: WithdrawService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 매일 자정 — 14일 유예가 지난 탈퇴 유저를 100명씩 물리 삭제
    @Scheduled(cron = "0 0 0 * * *")
    fun withDraw() {
        var lastId = 0L
        while (true) {
            val userIds = withdrawService.findUsersForHardDelete(lastId, BATCH_SIZE)
            if (userIds.isEmpty()) break

            userIds.forEach { userId ->
                runCatching { withdrawService.withdrawHardDelete(userId) }
                    .onFailure { log.error("하드 삭제 실패 userId=$userId", it) }
            }
            // 실패해 남은 유저까지 커서를 넘겨 무한 루프 방지
            lastId = userIds.last()
        }
    }

    companion object {
        private const val BATCH_SIZE = 100
    }
}
