package com.gerd.domain.report.service

import com.gerd.domain.auth.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

/**
 * 전체 유저 지난주 리포트 일괄 생성 오케스트레이션.
 *
 * - 트랜잭션은 유저 1명 단위(ReportService.getOrCreate)로 분리 → 한 명 실패가 전체 롤백으로 번지지 않음
 * - 마지막 userId 기준으로 순회 → 유저 전체를 메모리에 올리지 않고, 실행 중 row 변경에 따른 offset 밀림 방지
 * - 단일 스레드
 */
@Component
class ReportBatchProcessor(
    private val reportService: ReportService,
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun createAllReports() {
        var lastUserId = 0L
        var success = 0
        var failureCount = 0
        val sampledFailedUserIds = mutableListOf<Long>()
        val startedAt = System.currentTimeMillis()

        while (true) {
            val userIds = userRepository.findIdsAfter(lastUserId, PageRequest.of(0, PAGE_SIZE))
            if (userIds.isEmpty()) break

            userIds.forEach { userId ->
                try {
                    reportService.getOrCreate(userId)
                    success++
                } catch (e: Exception) {
                    failureCount++
                    if (sampledFailedUserIds.size < FAILURE_SAMPLE_SIZE) {
                        sampledFailedUserIds += userId
                    }
                    log.error("주간 리포트 생성 실패 - userId={}", userId, e)
                }
            }
            lastUserId = userIds.last()
        }

        val elapsedMs = System.currentTimeMillis() - startedAt
        if (failureCount == 0) {
            log.info("주간 리포트 일괄 생성 완료 - 성공 {}건, 실패 0건 ({}ms)", success, elapsedMs)
        } else {
            log.warn(
                "주간 리포트 일괄 생성 완료 - 성공 {}건, 실패 {}건 ({}ms), 실패 userId 샘플={}",
                success, failureCount, elapsedMs, sampledFailedUserIds,
            )
        }
    }

    companion object {
        private const val PAGE_SIZE = 200
        private const val FAILURE_SAMPLE_SIZE = 100
    }
}
