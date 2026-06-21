package com.gerd.domain.timeline.service

import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.domain.timeline.dto.TimeLineItemDTO
import com.gerd.domain.timeline.dto.TimeLineResponseDTO
import com.gerd.domain.timeline.dto.WeeklyJudgementResponseDTO
import com.gerd.domain.timeline.enums.TimeLineType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class TimeLineService(
    private val symptomRepository: SymptomRepository,
    private val mealRecordRepository: MealRecordRepository,
    private val mealFoodRepository: MealFoodRepository,
    private val foodRepository: FoodRepository,
) {

    fun getTimeLine(userId: Long, date: LocalDate): TimeLineResponseDTO {
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()

        val mealRecords = mealRecordRepository.findByUser_IdAndEatenAtBetween(userId, start, end)
        val symptoms = symptomRepository.findByUser_IdAndOccurredAtBetween(userId, start, end)

        if (mealRecords.isEmpty() && symptoms.isEmpty()) return TimeLineResponseDTO(emptyList())

        val mealRecordIds = mealRecords.mapNotNull { it.id }
        val mealRecordById = mealRecords.associateBy { requireNotNull(it.id) { "MealRecord.id must not be null" } }

        val mealFoodsByRecordId = mealFoodRepository
            .findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(mealRecordIds)
            .groupBy { it.mealRecordId }

        val foodIds = mealFoodsByRecordId.values.flatten().map { it.foodId }.distinct()
        val foodNameById = if (foodIds.isEmpty()) emptyMap()
        else foodRepository.findAllByIdsIncludingDeleted(foodIds).associate { it.id!! to it.name }

        val mealItems = mealRecords.map { record ->
            val foods = mealFoodsByRecordId[record.id] ?: emptyList()
            if (foods.size == 1) {
                val food = foods.first()
                TimeLineItemDTO.Single(
                    timeLineType = TimeLineType.SINGLE,
                    mealRecordId = record.externalId.toString(),
                    mealRecordDateTime = record.eatenAt.toString(),
                    mealFoodName = foodNameById[food.foodId] ?: "",
                    grade = food.judgedGrade ?: JudgmentGrade.UNKNOWN,
                    etcCount = 0,
                )
            } else {
                TimeLineItemDTO.Group(
                    timeLineType = TimeLineType.GROUP,
                    mealRecordId = record.externalId.toString(),
                    mealRecordDateTime = record.eatenAt.toString(),
                    representativeFoods = foods.take(2).map { foodNameById[it.foodId] ?: "" },
                    etcCount = maxOf(0, foods.size - 2),
                )
            }
        }

        val symptomItems = symptoms.map { s ->
            val linkedRecord = mealRecordById[s.mealRecordId]
            val afterMealMinutes = linkedRecord?.let {
                ChronoUnit.MINUTES.between(it.eatenAt, s.occurredAt).toInt()
            } ?: 0
            TimeLineItemDTO.Symptom(
                timeLineType = TimeLineType.SYMPTOM,
                symptomState = s.symptomState,
                afterMealMinutes = afterMealMinutes,
                occurredAt = s.occurredAt.toString(),
            )
        }

        val items = (mealItems + symptomItems).sortedBy { item ->
            when (item) {
                is TimeLineItemDTO.Single -> item.mealRecordDateTime
                is TimeLineItemDTO.Group -> item.mealRecordDateTime
                is TimeLineItemDTO.Symptom -> item.occurredAt
            }
        }

        return TimeLineResponseDTO(items = items)
    }

    fun getWeeklyJudgements(userId: Long, date: LocalDate): List<WeeklyJudgementResponseDTO> {
        val sunday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val saturday = sunday.plusDays(6)

        val mealFoods = mealFoodRepository.findByUser_IdAndEatenAtBetween(
            userId, sunday.atStartOfDay(), saturday.plusDays(1).atStartOfDay()
        )

        val gradesByDate = mealFoods.groupBy { it.eatenAt.toLocalDate() }

        // 각 날짜별로 판정 등급 리스트 생성
        // day로 key값 조회, null이 아닌 것들만 가져오고 신호등 등급이 없는 건 빈 리스트로 반환
        // safe call로 null이면 null반환하도록 설정
        return (0..6).map { i ->
            val day = sunday.plusDays(i.toLong())
            WeeklyJudgementResponseDTO(
                date = day,
                dayOfWeek = day.dayOfWeek.name,
                judgementList = gradesByDate[day]?.mapNotNull { it.judgedGrade } ?: emptyList()
            )
        }
    }
}