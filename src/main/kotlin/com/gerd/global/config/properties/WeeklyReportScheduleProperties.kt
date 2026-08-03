package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "report.weekly")
data class WeeklyReportScheduleProperties(
    var generationCron: String = "0 0 10 * * MON",
    var notificationCron: String = Scheduled.CRON_DISABLED,
) {
    fun hasNotificationSchedule(): Boolean = notificationCron != Scheduled.CRON_DISABLED
}
