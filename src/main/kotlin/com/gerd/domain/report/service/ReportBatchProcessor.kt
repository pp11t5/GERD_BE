package com.gerd.domain.report.service

import com.gerd.domain.auth.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

/**
 * 전체 유저 지난주 리포트 일괄 생성 오케스트레이션.
 *
 * - 트랜잭션은 유저 1명 단위(ReportService.getOrCreate)로 분리 → 한 명 실패가 전체 롤백으로 번지지 않음
 * - 페이징으로 순회 → 유저 전체를 메모리에 올리지 않고, 영속성 컨텍스트 누적도 방지
 * - 단일 스레드
 */
@Component
class ReportBatchProcessor(
    private val reportService: ReportService,
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun createAllReports() {
        var pageNumber = 0
        var success = 0
        val failedUserIds = mutableListOf<Long>()
        val startedAt = System.currentTimeMillis()

        do {
            val page = userRepository.findAll(PageRequest.of(pageNumber, PAGE_SIZE))
            page.content.forEach { user ->
                val userId = user.id ?: return@forEach
                try {
                    reportService.getOrCreate(userId)
                    success++
                } catch (e: Exception) {
                    failedUserIds += userId
                    log.error("주간 리포트 생성 실패 - userId={}", userId, e)
                }
            }
            pageNumber++
        } while (page.hasNext())

        val elapsedMs = System.currentTimeMillis() - startedAt
        if (failedUserIds.isEmpty()) {
            log.info("주간 리포트 일괄 생성 완료 - 성공 {}건, 실패 0건 ({}ms)", success, elapsedMs)
        } else {
            log.warn(
                "주간 리포트 일괄 생성 완료 - 성공 {}건, 실패 {}건 ({}ms), 실패 userId={}",
                success, failedUserIds.size, elapsedMs, failedUserIds,
            )
        }
    }

    companion object {
        private const val PAGE_SIZE = 200
    }
}
