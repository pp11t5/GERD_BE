package com.gerd.domain.report.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.mypage.dto.MealCount
import com.gerd.domain.mypage.dto.WeeklyReportResponseDTO
import com.gerd.domain.mypage.dto.WeeklySummaryResponseDTO
import com.gerd.domain.report.entity.WeeklyReport
import com.gerd.domain.report.repository.WeeklyReportRepository
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
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
    @Transactional
    fun getOrCreate(userId: Long): WeeklyReport {
        val (start, end) = lastWeekRange()
        val user: User = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        weeklyReportRepository.findByUserIdAndStartDate(userId, start)?.let { return it }

        // 끼니 기록 집계
        val allMeals = mealRecordRepository.findGradesByUserAndPeriod(
            userId, start.atStartOfDay(), end.atTime(LocalTime.MAX)
        )

        // 증상 기록 집계
        val symptoms = symptomRepository.findByUserAndPeriod(
            userId, start.atStartOfDay(), end.atTime(LocalTime.MAX)
        )
        val symptomsByDate = symptoms.groupBy { it.occurredAt.toLocalDate() }

        var streak = 0
        var symptomDays = 0
        var comfortableDays = 0

        // 스트릭 계산
        start.datesUntil(end.plusDays(1)).forEach { day ->
            val daySymptoms = symptomsByDate[day] ?: emptyList()
            if (daySymptoms.isNotEmpty()) {
                // 증상 기록이 있는지 확인
                symptomDays++
                if (daySymptoms.all { it.symptomState == SymptomState.COMFORTABLE }) {
                    // 그날 기록이 전부 편안함일 때만 편안한 날로 인정
                    comfortableDays++
                    streak++
                } else {
                    streak = 0
                }
            } else {
                streak = 0
            }
        }

        // 편안한 날 비율 (소수점 첫째 자리)
        val percentage = if (symptomDays > 0)
            (comfortableDays * 1000.0 / symptomDays).roundToInt() / 10.0
        else 0.0

        // 등급 카운트
        val gradeCounts = allMeals.groupingBy { it.grade }.eachCount()
        val recommendCount = gradeCounts[JudgmentGrade.RECOMMEND] ?: 0
        val cautionCount   = gradeCounts[JudgmentGrade.CAUTION] ?: 0
        val riskCount      = gradeCounts[JudgmentGrade.RISK] ?: 0
        val unknownCount   = gradeCounts[JudgmentGrade.UNKNOWN] ?: 0

        // 식단 분포
        val mealCount = MealCount(recommendCount, cautionCount, riskCount, unknownCount)

        // 증상 횟수
        val recentSymptomCount = symptoms.size

        // 평균 시간 (증상 발생 시각의 평균, 기록 없으면 00:00)
        val averageTime = symptoms
            .map { it.occurredAt.toLocalTime().toSecondOfDay().toLong() }
            .takeIf { it.isNotEmpty() }
            ?.let { LocalTime.ofSecondOfDay(it.average().toLong()) }
            .let { (it ?: LocalTime.of(0, 0)).atOffset(ZoneOffset.ofHours(9)) }

        // 평균 강도 (기록 없으면 0)
        val averageLevel = symptoms
            .map { scoreOf(it.symptomState) }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()
            ?: 0

        // 이물감, 신물, 기침, 답답함 집계 (증상 하나가 여러 타입 가질 수 있음)
        val typeCounts = symptoms
            .flatMap { it.symptomTypes }
            .groupingBy { it }
            .eachCount()

        // 각 증상 타입별 카운트, 없으면 0
        val throatForeignBodyCount = typeCounts[SymptomType.THROAT_FOREIGN_BODY] ?: 0
        val acidRefluxCount = typeCounts[SymptomType.ACID_REFLUX] ?: 0
        val coughCount = typeCounts[SymptomType.COUGH] ?: 0
        val chestTightnessCount = typeCounts[SymptomType.CHEST_TIGHTNESS] ?: 0

        val summaryDto = WeeklySummaryResponseDTO(
            mealRecordCount = allMeals.size,
            recentSymptomCount = recentSymptomCount,
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
            recordedSymptom = WeeklyReportResponseDTO.RecordedSymptom(
                symptomCount = recentSymptomCount,
                averageTime = averageTime,
                averageLevel = averageLevel,
                throatForeignBodyCount = throatForeignBodyCount,
                acidRefluxCount = acidRefluxCount,
                coughCount = coughCount,
                chestTightnessCount = chestTightnessCount,
            )
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

    // 증상 점수 매핑 (리포트 전용 산식 — 상태 추가 시 컴파일 에러로 감지)
    private fun scoreOf(state: SymptomState): Int = when (state) {
        SymptomState.COMFORTABLE -> 1
        SymptomState.GOOD -> 2
        SymptomState.NORMAL -> 3
        SymptomState.UNCOMFORTABLE -> 4
        SymptomState.SEVERE -> 5
    }

    // 주별로 매핑
    private fun weekLabel(date: LocalDate): String {
        val weekOfMonth = date.get(WeekFields.of(Locale.KOREAN).weekOfMonth())
        val ordinals = listOf("첫째", "둘째", "셋째", "넷째", "다섯째")
        val ordinal = ordinals.getOrElse(weekOfMonth - 1) { "${weekOfMonth}번째" }
        return "${date.year}년 ${date.monthValue}월 ${ordinal}주"
    }

    // 주 범위 날짜 계산
    private data class WeekRange(val start: LocalDate, val end: LocalDate)

    private fun lastWeekRange(): WeekRange {
        val thisWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return WeekRange(
            start = thisWeekStart.minusWeeks(1),
            end = thisWeekStart.minusDays(1),
        )
    }
}
