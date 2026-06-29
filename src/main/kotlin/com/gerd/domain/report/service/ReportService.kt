package com.gerd.domain.report.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.mypage.dto.MealCount
import com.gerd.domain.mypage.dto.WeeklyReportResponseDTO
import com.gerd.domain.mypage.dto.WeeklySummaryResponseDTO
import com.gerd.domain.report.entity.WeeklyReport
import com.gerd.domain.report.repository.WeeklyReportRepository
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomRepository
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@Service
@Transactional(readOnly = true)
class ReportService(
    private val weeklyReportRepository: WeeklyReportRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
    private val mealRecordRepository: MealRecordRepository,
    private val symptomRepository: SymptomRepository,
) {

    // 리포트 조회 (없으면 null)
    fun getReport(userId: Long): WeeklyReportResponseDTO? {
        val (start, _) = lastWeekRange()
        return weeklyReportRepository.findByUserIdAndStartDate(userId, start)
            ?.let { objectMapper.readValue(it.reportJson, WeeklyReportResponseDTO::class.java) }
    }

    // 지난주 요약 조회 (없으면 null)
    fun getWeeklySummary(userId: Long): WeeklySummaryResponseDTO? {
        val (start, _) = lastWeekRange()
        return weeklyReportRepository.findByUserIdAndStartDate(userId, start)
            ?.let { objectMapper.readValue(it.summaryJson, WeeklySummaryResponseDTO::class.java) }
    }

    // 지난주 리포트 조회 또는 생성 — 유저 1명 단위의 독립 트랜잭션
    // (배치 오케스트레이션은 ReportBatchProcessor 가 담당. 한 명 실패가 다른 유저에 영향 주지 않도록 분리)
    @Transactional
    fun getOrCreate(userId: Long): WeeklyReport {
        val (start, end) = lastWeekRange()
        val user: User = userRepository.getReferenceById(userId)

        weeklyReportRepository.findByUserIdAndStartDate(userId, start)?.let { return it }

        // 끼니 기록 집계
        val mealsByDate = mealRecordRepository.findGradesByUserAndPeriod(
            userId, start.atStartOfDay(), end.atTime(LocalTime.MAX)
        ).groupBy { it.date }

        // 증상 기록 집계
        val symptomsByDate = symptomRepository.findStatesByUserAndPeriod(
            userId, start.atStartOfDay(), end.atTime(LocalTime.MAX)
        ).groupBy { it.date }

        var streak = 0
        var symptomDays = 0
        var comfortableDays = 0

        // 스트릭 계산
        for (day in start.datesUntil(end.plusDays(1))) {
            val daySymptoms = symptomsByDate[day] ?: emptyList()
            if (daySymptoms.isNotEmpty()) {
                // 증상 기록이 있는지 확인
                symptomDays++
                if (daySymptoms.any { it.state == SymptomState.COMFORTABLE }) {
                    // 편안한 날인지 확인
                    comfortableDays++
                    streak++
                } else {
                    streak = 0
                }
            } else {
                streak = 0
            }
        }

        // 등급 카운트
        val allMeals = mealsByDate.values.flatten()
        val gradeCounts = allMeals.groupingBy { it.grade }.eachCount()
        val recommendCount = gradeCounts[JudgmentGrade.RECOMMEND] ?: 0
        val cautionCount   = gradeCounts[JudgmentGrade.CAUTION] ?: 0
        val riskCount      = gradeCounts[JudgmentGrade.RISK] ?: 0
        val percentage = if (symptomDays > 0)
            (comfortableDays * 1000.0 / symptomDays).roundToInt() / 10.0
        else 0.0

        // 식단 분포
        val mealCount = MealCount(recommendCount, cautionCount, riskCount)

        val summaryDto = WeeklySummaryResponseDTO(
            mealRecordCount = allMeals.size,
            recentSymptomCount = symptomsByDate.values.sumOf { it.size },
            streakCount = streak,
            mealCount = mealCount,
        )
        val reportDto = WeeklyReportResponseDTO(
            startDate = start.toString(),
            endDate = end.toString(),
            weekLabel = weekLabel(start),
            comfortableState = WeeklyReportResponseDTO.ComfortableState(
                streakCount = streak,
                recommendedMealCount = recommendCount,
                percentage = percentage,
            ),
            mealCount = mealCount,
        )

        return weeklyReportRepository.save(
            WeeklyReport(
                user = user,
                startDate = start,
                endDate = end,
                summaryJson = objectMapper.writeValueAsString(summaryDto),
                reportJson = objectMapper.writeValueAsString(reportDto),
            )
        )
    }

    // 주별로 매핑
    private fun weekLabel(date: LocalDate): String {
        val ordinals = listOf("첫째", "둘째", "셋째", "넷째", "다섯째")
        val ordinal = ordinals.getOrElse((date.dayOfMonth - 1) / 7) { "${(date.dayOfMonth - 1) / 7 + 1}번째" }
        return "${date.year}년 ${date.monthValue}월 ${ordinal}주"
    }

    // 주 범위 날짜 계산
    private data class WeekRange(val start: LocalDate, val end: LocalDate)

    private fun lastWeekRange(): WeekRange {
        val thisWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return WeekRange(
            start = thisWeekStart.minusWeeks(1),
            end = thisWeekStart.minusDays(1),
        )
    }
}
