package com.gerd.domain.food.service

import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.UserFood
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.repository.UserFoodRepository
import com.gerd.global.apiPayload.GeneralException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * 음식 이름 검색 서비스
 *
 * - 검색어 검증(빈값/길이) 후 공백 제거 → 공백 무시 LIKE 검색
 * - 노출 범위·정렬은 리포지토리(QueryDSL)에서 처리, 카테고리는 배치 로딩으로 조립
 * - 완전 일치 결과 없을 때 USER 음식 + UserFood 생성 (관리자 검토·신호등 증상 조회용)
 */
@Service
@Transactional(readOnly = true)
class FoodSearchService(
    private val foodRepository: FoodRepository,
    private val foodCategoryReader: FoodCategoryReader,
    private val userFoodRepository: UserFoodRepository,
) {

    @Transactional
    fun search(rawQuery: String?, rawSize: Int?, userId: Long): List<FoodSummaryDTO> {
        val trimmed = rawQuery?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.length > MAX_QUERY_LENGTH) {
            throw GeneralException(FoodErrorCode.INVALID_SEARCH_QUERY)
        }

        // 검색어 정규화
        val normalized = trimmed.replace(" ", "")
        if (normalized.isEmpty()) {
            throw GeneralException(FoodErrorCode.INVALID_SEARCH_QUERY)
        }
        val size = (rawSize ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE)
        val foods = foodRepository.search(normalized, size, userId).toMutableList()

        val hasExactMatch = foods.any { it.name.replace(" ", "").equals(normalized, ignoreCase = true) }
        if (!hasExactMatch) {
            foods.add(resolveOrCreateWithUserFood(trimmed, userId))
        }

        val categories = foodCategoryReader.loadPrimaryByFoodIds(foods.mapNotNull { it.id })
        return foods.map { food ->
            FoodSummaryDTO(
                externalId = food.externalId.toString(),
                name = food.name,
                category = categories[food.id],
            )
        }
    }

    // 검색어 완전 일치 음식이 없을 때 USER 음식 생성 + UserFood 생성
    // 검색어 미일치할 경우, 새로운 사용자 음식을 생성
    private fun resolveOrCreateWithUserFood(name: String, userId: Long): Food {
        foodRepository.findByNameAndOwnerUserIdAndSource(name, userId, FoodSource.USER)?.let { existing ->
            log.debug { "검색 미일치 - 기존 USER 음식 재사용: name=$name userId=$userId foodId=${existing.id}" }
            ensureUserFoodExists(userId, existing)
            return existing
        }
        val food = runCatching {
            foodRepository.save(Food(name = name, source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = userId))
                .also { log.info { "검색 미일치 - 새 USER 음식 생성: name=$name userId=$userId foodId=${it.id}" } }
        }.getOrElse { e ->
            if (e !is DataIntegrityViolationException) throw e
            foodRepository.findByNameAndOwnerUserIdAndSource(name, userId, FoodSource.USER)
                ?.also { log.warn { "검색 미일치 - 동시 생성 경합 복구: name=$name userId=$userId foodId=${it.id}" } }
                ?: throw e
        }
        ensureUserFoodExists(userId, food)
        return food
    }

    private fun ensureUserFoodExists(userId: Long, food: Food) {
        val foodId = requireNotNull(food.id)
        if (!userFoodRepository.existsByUserIdAndFoodId(userId, foodId)) {
            userFoodRepository.save(UserFood(userId = userId, food = food, isUnknown = true))
            log.debug { "UserFood 생성: userId=$userId foodId=$foodId" }
        }
    }

    companion object {
        const val DEFAULT_SIZE = 10
        const val MAX_SIZE = 50
        const val MAX_QUERY_LENGTH = 100
    }
}
