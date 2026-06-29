package com.gerd.domain.timeline.service

import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.domain.timeline.dto.TimeLineItemDTO
import com.gerd.domain.timeline.dto.TimeLineResponseDTO
import com.gerd.domain.timeline.dto.WeeklyJudgementResponseDTO
import com.gerd.domain.timeline.enums.TimeLineIcon
import com.gerd.domain.timeline.enums.TimeLineType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
            .groupBy { it.mealRecord.id!! }

        val foodIds = mealFoodsByRecordId.values.flatten().map { it.foodId }.distinct()
        val foodNameById = if (foodIds.isEmpty()) emptyMap()
        else foodRepository.findAllByIdsIncludingDeleted(foodIds).associate { it.id!! to it.name }

        val linkedSymptomsByMealId: Map<Long, List<Symptom>> = symptoms
            .filter { it.mealRecordId != null }
            .groupBy { it.mealRecordId!! }

        // Group 식사에 연결된 증상 ID 집합 — 이 증상들은 카드에 임베드되므로 standalone 목록에서 제외
        val groupEmbeddedMealIds = mutableSetOf<Long>()

        val mealItems = mealRecords.map { record ->
            val foods = mealFoodsByRecordId[record.id] ?: emptyList()

            if (foods.size <= 1) {
                val food = foods.firstOrNull()
                TimeLineItemDTO.Single(
                    timeLineType = TimeLineType.SINGLE,
                    timeIcon = timeIcon(record.eatenAt),
                    mealRecordId = record.externalId.toString(),
                    mealRecordDateTime = record.eatenAt.toString(),
                    mealFoodName = food?.let { foodNameById[it.foodId] } ?: "",
                    grade = food?.judgedGrade ?: JudgmentGrade.CAUTION,
                    etcCount = 0,
                )
            } else {
                val linkedSymptoms = linkedSymptomsByMealId[record.id] ?: emptyList()
                val connectedSymptomDTO = if (linkedSymptoms.isEmpty()) null
                else {
                    record.id?.let { groupEmbeddedMealIds.add(it) }
                    buildConnectedSymptom(record.eatenAt, linkedSymptoms)
                }

                TimeLineItemDTO.Group(
                    timeLineType = TimeLineType.GROUP,
                    timeIcon = timeIcon(record.eatenAt),
                    mealRecordId = record.externalId.toString(),
                    mealRecordDateTime = record.eatenAt.toString(),
                    representativeFoods = foods.take(2).map { foodNameById[it.foodId] ?: "" },
                    etcCount = maxOf(0, foods.size - 2),
                    connectedSymptoms = connectedSymptomDTO,
                )
            }
        }

        // Single 연결 증상 + 미연결 증상만 standalone으로 표시 (Group 임베드 증상 제외)
        val standaloneSymptoms = symptoms.filter { s ->
            s.mealRecordId == null || s.mealRecordId !in groupEmbeddedMealIds
        }

        val symptomItems = standaloneSymptoms.map { s ->
            val linkedRecord = s.mealRecordId?.let { mealRecordById[it] }
            val afterMealMinutes = linkedRecord?.let {
                ChronoUnit.MINUTES.between(it.eatenAt, s.occurredAt).toInt()
            } ?: 0

            TimeLineItemDTO.Symptom(
                timeLineType = TimeLineType.SYMPTOM,
                timeIcon = timeIcon(s.occurredAt),
                symptomId = s.externalId?.toString() ?: "",
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

    private fun buildConnectedSymptom(
        mealEatenAt: LocalDateTime,
        symptoms: List<Symptom>,
    ): TimeLineItemDTO.ConnectedSymptom {
        val mostRecent = symptoms.maxBy { it.occurredAt }
        val allTypes = symptoms.flatMap { it.symptomTypes }.distinct()
        val afterMealMinutes = ChronoUnit.MINUTES.between(mealEatenAt, mostRecent.occurredAt).toInt()
        return TimeLineItemDTO.ConnectedSymptom(
            symptomId = mostRecent.externalId?.toString() ?: "",
            symptomState = mostRecent.symptomState,
            afterMealMinutes = afterMealMinutes,
            representativeSymptoms = allTypes.take(2),
            etcCount = maxOf(0, allTypes.size - 2),
        )
    }

    private fun timeIcon(dateTime: LocalDateTime): TimeLineIcon {
        val time = dateTime.toLocalTime()
        return if (!time.isBefore(DAY_START) && time.isBefore(NIGHT_START)) {
            TimeLineIcon.SUN
        } else {
            TimeLineIcon.MOON
        }
    }

    fun getWeeklyJudgements(userId: Long, date: LocalDate): List<WeeklyJudgementResponseDTO> {
        val sunday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val saturday = sunday.plusDays(6)

        val mealFoods = mealFoodRepository.findByUser_IdAndEatenAtBetween(
            userId, sunday.atStartOfDay(), saturday.plusDays(1).atStartOfDay()
        )

        val gradesByDate = mealFoods.groupBy { it.eatenAt.toLocalDate() }

        return (0..6).map { i ->
            val day = sunday.plusDays(i.toLong())
            WeeklyJudgementResponseDTO(
                date = day,
                dayOfWeek = day.dayOfWeek.name,
                judgementList = gradesByDate[day]?.mapNotNull { it.judgedGrade } ?: emptyList()
            )
        }
    }

    companion object {
        private val DAY_START: LocalTime = LocalTime.of(6, 0)
        private val NIGHT_START: LocalTime = LocalTime.of(18, 0)
    }
}
