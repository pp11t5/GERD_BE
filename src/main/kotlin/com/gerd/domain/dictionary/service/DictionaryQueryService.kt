package com.gerd.domain.dictionary.service

import com.gerd.domain.dictionary.dto.CautionRiskFoodItemDTO
import com.gerd.domain.dictionary.dto.DictionaryCountResponseDTO
import com.gerd.domain.dictionary.dto.SafeFoodItemDTO
import com.gerd.domain.dictionary.entity.UserFoodDictionary
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import com.gerd.global.common.response.CursorResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DictionaryQueryService(
    private val dictionaryRepository: UserFoodDictionaryRepository,
    private val foodCategoryMapRepository: FoodCategoryMapRepository,
) {

    // 도감 상태별 음식 개수 반환
    fun getCount(userId: Long): DictionaryCountResponseDTO {
        val safeCount = dictionaryRepository.countByUser_IdAndDictionaryType(userId, DictionaryType.SAFE)
        val cautionRiskCount = dictionaryRepository.countByUser_IdAndDictionaryTypeIn(
            userId,
            listOf(DictionaryType.CAUTION, DictionaryType.RISK),
        )
        return DictionaryCountResponseDTO(
            safeCount = safeCount.toInt(),
            cautionRiskCount = cautionRiskCount.toInt(),
        )
    }

    // 안전 음식 목록 반환 — 항목은 항상 SAFE라 type을 응답에 담지 않는다.
    fun getSafeFoods(rawSize: Int?, cursor: Long?, userId: Long): CursorResponse<SafeFoodItemDTO, Long> {
        val size = resolveSize(rawSize)
        val entries = dictionaryRepository.findWithFoodByCursorAndType(
            userId, DictionaryType.SAFE, cursor, PageRequest.of(0, size + 1),
        )
        return toCursorResponse(entries, size, ::toSafeItems)
    }

    // 주의·위험 음식 목록 반환 — CAUTION/RISK 구분을 위해 type을 함께 담는다.
    fun getCautionRiskFoods(rawSize: Int?, cursor: Long?, userId: Long): CursorResponse<CautionRiskFoodItemDTO, Long> {
        val size = resolveSize(rawSize)
        val entries = dictionaryRepository.findWithFoodByCursorAndTypeIn(
            userId, listOf(DictionaryType.CAUTION, DictionaryType.RISK), cursor, PageRequest.of(0, size + 1),
        )
        return toCursorResponse(entries, size, ::toCautionRiskItems)
    }

    private fun resolveSize(rawSize: Int?): Int = (rawSize ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE)

    // hasNext/nextCursor는 (size+1로 조회한) 엔티티의 id 기준으로 판정한다.
    // externalId 누락으로 매핑에서 일부가 빠져도 커서 연속성이 유지된다.
    private fun <T> toCursorResponse(
        entries: List<UserFoodDictionary>,
        size: Int,
        toItems: (List<UserFoodDictionary>) -> List<T>,
    ): CursorResponse<T, Long> {
        val hasNext = entries.size > size
        val pageEntries = if (hasNext) entries.take(size) else entries
        val nextCursor = if (hasNext) pageEntries.last().id else null
        return CursorResponse(toItems(pageEntries), nextCursor, hasNext)
    }

    private fun toSafeItems(entries: List<UserFoodDictionary>): List<SafeFoodItemDTO> {
        val codeMap = loadCodeMap(entries)
        return entries.mapNotNull { entry ->
            val foodId = entry.food.externalId?.toString() ?: return@mapNotNull null
            SafeFoodItemDTO(
                foodId = foodId,
                name = entry.food.name,
                code = codeMap[entry.food.id],
            )
        }
    }

    private fun toCautionRiskItems(entries: List<UserFoodDictionary>): List<CautionRiskFoodItemDTO> {
        val codeMap = loadCodeMap(entries)
        return entries.mapNotNull { entry ->
            val foodId = entry.food.externalId?.toString() ?: return@mapNotNull null
            CautionRiskFoodItemDTO(
                foodId = foodId,
                name = entry.food.name,
                code = codeMap[entry.food.id],
                type = entry.dictionaryType,
            )
        }
    }

    // foodId -> 대표 분류 code (음식별 첫 번째 view). 한 번에 조회해 N+1 회피.
    private fun loadCodeMap(entries: List<UserFoodDictionary>): Map<Long, String?> {
        val foodIds = entries.mapNotNull { it.food.id }
        if (foodIds.isEmpty()) return emptyMap()
        return foodCategoryMapRepository.findCategoryViewsByFoodIds(foodIds)
            .groupBy { it.foodId }
            .mapValues { (_, views) -> views.firstOrNull()?.code }
    }

    companion object {
        const val DEFAULT_SIZE = 20
        const val MAX_SIZE = 50
    }
}
