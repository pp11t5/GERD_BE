package com.gerd.domain.report.entity

import com.gerd.domain.auth.entity.User
import com.gerd.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "weekly_reports",
    uniqueConstraints = [UniqueConstraint(name = "uq_weekly_report_user_start", columnNames = ["user_id", "start_date"])],
    indexes = [Index(name = "weekly_reports_user_start_idx", columnList = "user_id, start_date")],
)
class WeeklyReport(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,

    @Column(name = "summary_json", columnDefinition = "TEXT", nullable = false)
    val summaryJson: String,   // WeeklySummaryResponseDTO 직렬화

    @Column(name = "report_json", columnDefinition = "TEXT", nullable = false)
    val reportJson: String,    // WeeklyReportResponseDTO 직렬화

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weekly_report_id")
    val id: Long? = null,
) : BaseTimeEntity()
