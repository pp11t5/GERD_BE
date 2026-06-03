package com.gerd.domain.food.service

import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 음식 이름 검색 서비스
 *
 * - 검색어 검증(빈값/길이) 후 공백 제거 → 공백 무시 ILIKE 검색
 * - 노출 범위·정렬은 리포지토리(QueryDSL)에서 처리, 카테고리는 배치 로딩으로 조립
 */
@Service
@Transactional(readOnly = true)
class FoodSearchService(
    private val foodRepository: FoodRepository,
    private val foodCategoryReader: FoodCategoryReader,
) {

    fun search(rawQuery: String?, rawSize: Int?, userId: Long): List<FoodSummaryDTO> {
        val trimmed = rawQuery?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.length > MAX_QUERY_LENGTH) {
            throw GeneralException(FoodErrorCode.INVALID_SEARCH_QUERY)
        }
        // 공백만으로 이뤄진 검색어는 정규화 시 빈 문자열 → 전체 매칭 방지
        val normalized = trimmed.replace(" ", "")
        if (normalized.isEmpty()) {
            throw GeneralException(FoodErrorCode.INVALID_SEARCH_QUERY)
        }

        val size = (rawSize ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE)
        val foods = foodRepository.search(normalized, size, userId)

        val categories = foodCategoryReader.loadPrimaryByFoodIds(foods.mapNotNull { it.id })
        return foods.map { food ->
            FoodSummaryDTO(
                externalId = food.externalId.toString(),
                name = food.name,
                category = categories[food.id],
            )
        }
    }

    companion object {
        const val DEFAULT_SIZE = 10
        const val MAX_SIZE = 50
        const val MAX_QUERY_LENGTH = 100
    }
}
