package com.gerd.domain.report.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReportScheduler(
    private val reportBatchProcessor: ReportBatchProcessor,
) {
    @Scheduled(cron = "\${report.weekly.generation-cron}")
    fun createWeeklyReports() {
        reportBatchProcessor.createAllReports()
    }
}
