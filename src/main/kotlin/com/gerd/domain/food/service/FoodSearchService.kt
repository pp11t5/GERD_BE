package com.gerd.domain.food.service

import com.gerd.domain.food.dto.FoodSearchResultDTO
import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer

@Service
@Transactional(readOnly = true)
class FoodSearchService(
    private val foodRepository: FoodRepository,
    private val foodCategoryReader: FoodCategoryReader,
) {

    fun search(rawQuery: String?, rawSize: Int?, userId: Long): FoodSearchResultDTO {
        // 검색어 전처리: 공백 제거, 정규화, 길이 제한, NFC 타입으로 정규화
        val trimmed = Normalizer.normalize(rawQuery?.trim().orEmpty(), Normalizer.Form.NFC)
        if (trimmed.isEmpty() || trimmed.length > MAX_QUERY_LENGTH) {
            throw GeneralException(FoodErrorCode.INVALID_SEARCH_QUERY)
        }

        val normalized = trimmed.replace(" ", "")
        if (normalized.isEmpty()) {
            throw GeneralException(FoodErrorCode.INVALID_SEARCH_QUERY)
        }
        val size = (rawSize ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE)
        val exactResults = foodRepository.search(normalized, size, userId)
        val hasExactMatch = exactResults.any { it.name.replace(" ", "").equals(normalized, ignoreCase = true) }

        // 완전 일치가 없으면 자모 유사도 기반 오타 보정 후보를 자동으로 붙임
        // 임계값 미만인 경우 오타보정 메서드가 빈 리스트 반환
        val foods = if (hasExactMatch) {
            exactResults
        } else {
            val remaining = size - exactResults.size
            val jamoMatches = if (remaining > 0) foodRepository.findSimilarByJamo(normalized, userId, remaining) else emptyList()
            val existingIds = exactResults.mapNotNull { it.id }.toSet()
            exactResults + jamoMatches.filterNot { it.id in existingIds }
        }

        val categories = foodCategoryReader.loadPrimaryByFoodIds(foods.mapNotNull { it.id })
        return FoodSearchResultDTO(
            foods = foods.map { food ->
                FoodSummaryDTO(
                    externalId = food.externalId.toString(),
                    name = food.name,
                    category = categories[food.id],
                )
            },
            hasExactMatch = hasExactMatch, // 완전 일치하는 검색어 존재 여부 반환
        )
    }

    companion object {
        const val DEFAULT_SIZE = 10
        const val MAX_SIZE = 50
        const val MAX_QUERY_LENGTH = 100
    }
}
