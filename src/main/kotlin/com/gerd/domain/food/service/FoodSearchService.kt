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
        val trimmed = Normalizer.normalize(rawQuery?.trim().orEmpty(), Normalizer.Form.NFC)
        if (trimmed.isEmpty() || trimmed.length > MAX_QUERY_LENGTH) {
            throw GeneralException(FoodErrorCode.INVALID_SEARCH_QUERY)
        }

        val normalized = trimmed.replace(" ", "")
        if (normalized.isEmpty()) {
            throw GeneralException(FoodErrorCode.INVALID_SEARCH_QUERY)
        }
        val size = (rawSize ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE)
        val foods = foodRepository.search(normalized, size, userId)

        val hasExactMatch = foods.any { it.name.replace(" ", "").equals(normalized, ignoreCase = true) }

        val categories = foodCategoryReader.loadPrimaryByFoodIds(foods.mapNotNull { it.id })
        return FoodSearchResultDTO(
            foods = foods.map { food ->
                FoodSummaryDTO(
                    externalId = food.externalId.toString(),
                    name = food.name,
                    category = categories[food.id],
                )
            },
            hasExactMatch = hasExactMatch,
        )
    }

    companion object {
        const val DEFAULT_SIZE = 10
        const val MAX_SIZE = 50
        const val MAX_QUERY_LENGTH = 100
    }
}
