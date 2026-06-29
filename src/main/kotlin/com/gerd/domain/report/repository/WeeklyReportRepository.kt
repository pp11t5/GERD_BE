package com.gerd.domain.report.repository

import com.gerd.domain.report.entity.WeeklyReport
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface WeeklyReportRepository : JpaRepository<WeeklyReport, Long> {

    fun findByUserIdAndStartDate(userId: Long, startDate: LocalDate): WeeklyReport?
}
