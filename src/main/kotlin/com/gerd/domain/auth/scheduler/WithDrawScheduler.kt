package com.gerd.domain.auth.scheduler

import com.gerd.domain.auth.service.WithdrawService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("prod")
class WithDrawScheduler(
    private val withdrawService: WithdrawService,
) {
    private val log = KotlinLogging.logger {}

    // 매일 자정 — 유예기간이 지난 탈퇴 유저를 배치로 물리 삭제
    @Scheduled(cron = "0 0 0 * * *")
    fun withDraw() {
        var lastId = 0L
        while (true) {
            val userIds = withdrawService.findUsersForHardDelete(lastId, BATCH_SIZE)
            if (userIds.isEmpty()) break
            userIds.forEach { userId ->
                runCatching { withdrawService.withdrawHardDelete(userId) }
                    .onFailure { log.error(it) { "하드 삭제 실패 userId=$userId" } }
            }
            lastId = userIds.last()
        }
    }

    companion object {
        private const val BATCH_SIZE = 100
    }
}
