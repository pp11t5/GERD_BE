package com.gerd.domain.meal.service

import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.service.FoodCategoryReader
import com.gerd.domain.meal.dto.MealGroupDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordSummaryDTO
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * 식사 기록 ↔ DTO 변환 + 시각/식별자 표현 변환
 *
 * - food/카테고리 배치 로딩으로 목록 조립 시 N+1 회피
 * - food는 삭제분 포함 조회(D5) — 기록은 음식 soft-delete 후에도 음식 정보를 보존한다
 * - 시각은 저장(KST LocalDateTime) ↔ API(ISO-8601 offset 포함) 사이를 변환
 */
@Component
class MealRecordAssembler(
    private val foodRepository: FoodRepository,
    private val foodCategoryReader: FoodCategoryReader,
) {

    // 목록 — food/카테고리 배치 로딩 후 조립 (삭제 food 포함)
    fun toSummaries(records: List<MealRecord>): List<MealRecordSummaryDTO> {
        if (records.isEmpty()) return emptyList()
        val foodIds = records.map { it.foodId }.distinct()
        val foods = loadFoodsIncludingDeleted(foodIds).associateBy { it.id }
        val categories = foodCategoryReader.loadPrimaryByFoodIds(foodIds)
        return records.map { record ->
            val food = foods[record.foodId]
                ?: error("meal record ${record.id} references missing food ${record.foodId}")
            toSummaryDto(record, food, categories[record.foodId])
        }
    }

    // 생성 — 직전에 조회한 음식을 재사용해 카테고리만 추가 로딩
    fun toSummary(record: MealRecord, food: Food): MealRecordSummaryDTO {
        val category = foodCategoryReader.loadPrimaryByFoodIds(listOf(food.id!!))[food.id]
        return toSummaryDto(record, food, category)
    }

    // 상세 — 삭제 food 포함 단건 조회 + description
    fun toDetail(record: MealRecord): MealRecordDetailDTO {
        val food = loadFoodsIncludingDeleted(listOf(record.foodId)).firstOrNull()
            ?: error("meal record ${record.id} references missing food ${record.foodId}")
        val category = foodCategoryReader.loadPrimaryByFoodIds(listOf(record.foodId))[record.foodId]
        return MealRecordDetailDTO(
            mealId = record.externalId.toString(),
            mealGroupId = record.mealGroupId.toString(),
            eatenAt = formatEatenAt(record.eatenAt),
            memo = record.memo,
            judgedGrade = record.judgedGrade,
            food = MealRecordDetailDTO.MealFoodDetailDTO(
                externalId = food.externalId.toString(),
                name = food.name,
                category = category,
                description = food.description,
            ),
            stateRecords = emptyList(),
        )
    }

    // 끼니 단위 그룹핑 — records가 eatenAt asc라 그룹 등장 순서 = 대표 시각(최솟값) asc, 그룹 내도 asc
    fun toGroups(summaries: List<MealRecordSummaryDTO>): List<MealGroupDTO> =
        summaries.groupBy { it.mealGroupId }
            .map { (groupId, records) -> MealGroupDTO(groupId, records.first().eatenAt, records) }

    // 미전달 시 서버 현재 시각(KST). offset 포함 ISO-8601만 허용 — 형식 오류는 MEAL400_2
    fun parseEatenAt(raw: String?): LocalDateTime =
        if (raw == null) {
            LocalDateTime.now(SEOUL)
        } else {
            try {
                OffsetDateTime.parse(raw).atZoneSameInstant(SEOUL).toLocalDateTime()
            } catch (e: DateTimeParseException) {
                throw GeneralException(MealErrorCode.INVALID_DATE_TIME)
            }
        }

    // 미전달 시 오늘(KST). 형식 오류는 MEAL400_2
    fun parseDate(raw: String?): LocalDate =
        if (raw == null) {
            LocalDate.now(SEOUL)
        } else {
            try {
                LocalDate.parse(raw)
            } catch (e: DateTimeParseException) {
                throw GeneralException(MealErrorCode.INVALID_DATE_TIME)
            }
        }

    // 형식이 잘못된 UUID는 null로 — 호출부에서 존재하지 않는 리소스와 동일 취급(열거 단서 차단)
    fun parseUuid(raw: String): UUID? =
        try {
            UUID.fromString(raw.trim())
        } catch (e: IllegalArgumentException) {
            null
        }

    fun toDayRange(date: LocalDate): Pair<LocalDateTime, LocalDateTime> =
        date.atStartOfDay() to date.plusDays(1).atStartOfDay()

    // PostgreSQL native IN () 구문 오류 방지 — 빈 컬렉션은 DB 조회 없이 빈 리스트로 단락
    private fun loadFoodsIncludingDeleted(ids: Collection<Long>): List<Food> =
        if (ids.isEmpty()) emptyList() else foodRepository.findAllByIdsIncludingDeleted(ids)

    private fun formatEatenAt(eatenAt: LocalDateTime): String =
        eatenAt.atZone(SEOUL).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    private fun toSummaryDto(record: MealRecord, food: Food, category: String?) =
        MealRecordSummaryDTO(
            mealId = record.externalId.toString(),
            mealGroupId = record.mealGroupId.toString(),
            eatenAt = formatEatenAt(record.eatenAt),
            food = FoodSummaryDTO(
                externalId = food.externalId.toString(),
                name = food.name,
                category = category,
            ),
            judgedGrade = record.judgedGrade,
        )

    companion object {
        private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
