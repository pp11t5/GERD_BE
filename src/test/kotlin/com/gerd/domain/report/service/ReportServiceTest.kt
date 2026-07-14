package com.gerd.domain.report.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.mypage.dto.MealCount
import com.gerd.domain.mypage.dto.WeeklySummaryResponseDTO
import com.gerd.domain.report.entity.WeeklyReport
import com.gerd.domain.report.repository.WeeklyReportRepository
import com.gerd.domain.symptom.repository.SymptomRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.databind.ObjectMapper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@ExtendWith(MockitoExtension::class)
class ReportServiceTest {

    @Mock
    private lateinit var weeklyReportRepository: WeeklyReportRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var mealRecordRepository: MealRecordRepository

    @Mock
    private lateinit var symptomRepository: SymptomRepository

    private val service by lazy {
        ReportService(
            weeklyReportRepository,
            userRepository,
            objectMapper,
            mealRecordRepository,
            symptomRepository,
        )
    }

    private val userId = 1L

    @Nested
    inner class `주간 리포트 생성` {

        @Test
        fun `기존 리포트가 있으면 집계하지 않고 그대로 반환한다`() {
            val (start, end) = lastWeekRange()
            val existing = weeklyReport(start, end)
            whenever(userRepository.getReferenceById(userId)).thenReturn(user())
            whenever(weeklyReportRepository.findByUserIdAndStartDate(userId, start)).thenReturn(existing)

            val result = service.getOrCreate(userId)

            assertThat(result).isSameAs(existing)
            verify(mealRecordRepository, never()).findGradesByUserAndPeriod(any(), any(), any())
            verify(weeklyReportRepository, never()).save(any())
        }

    }

    @Nested
    inner class `주간 리포트 조회` {

        @Test
        fun `저장된 리포트가 없으면 null을 반환한다`() {
            val (start, _) = lastWeekRange()
            whenever(weeklyReportRepository.findByUserIdAndStartDate(userId, start)).thenReturn(null)

            val result = service.getReport(userId)

            assertThat(result).isNull()
        }

        @Test
        fun `저장된 요약 JSON을 DTO로 역직렬화한다`() {
            val (start, end) = lastWeekRange()
            val report = weeklyReport(start, end, summaryJson = "summary-json")
            val summary = WeeklySummaryResponseDTO(1, 2, 3, MealCount(1, 0, 0, 0))
            whenever(weeklyReportRepository.findByUserIdAndStartDate(userId, start)).thenReturn(report)
            whenever(objectMapper.readValue("summary-json", WeeklySummaryResponseDTO::class.java)).thenReturn(summary)

            val result = service.getWeeklySummary(userId)

            assertThat(result).isEqualTo(summary)
        }
    }

    private fun user() = User(
        id = userId,
        email = "user@test.com",
        nickname = "위장이",
    )

    private fun weeklyReport(
        start: LocalDate,
        end: LocalDate,
        summaryJson: String = "{}",
        reportJson: String = "{}",
    ) = WeeklyReport(
        user = user(),
        startDate = start,
        endDate = end,
        summaryJson = summaryJson,
        reportJson = reportJson,
    )

    private fun lastWeekRange(): Pair<LocalDate, LocalDate> {
        val thisWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return thisWeekStart.minusWeeks(1) to thisWeekStart.minusDays(1)
    }
}
