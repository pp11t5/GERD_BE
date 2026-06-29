package com.gerd.domain.report.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReportScheduler(
    private val reportBatchProcessor: ReportBatchProcessor,
) {
    // 매주 일요일 오전 7시 — 전체 유저 지난주 리포트 일괄 생성
    @Scheduled(cron = "0 0 7 * * SUN")
    fun createWeeklyReports() {
        reportBatchProcessor.createAllReports()
    }
}