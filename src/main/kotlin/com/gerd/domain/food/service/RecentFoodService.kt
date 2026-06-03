package com.gerd.domain.food.service

import com.gerd.domain.food.dto.RecentFoodDTO
import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.FoodSearchHistory
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.repository.FoodSearchHistoryRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/**
 * 최근 본 음식 CRUD 서비스
 *
 * - 추가: (user_id, food_id) upsert(재진입 시 searchedAt만 갱신) + 보관 상한(10) 정리
 * - 조회/삭제: 본인 항목만, soft-deleted 음식은 조회에서 제외
 */
@Service
@Transactional(readOnly = true)
class RecentFoodService(
    private val foodSearchHistoryRepository: FoodSearchHistoryRepository,
    private val foodRepository: FoodRepository,
    private val foodCategoryReader: FoodCategoryReader,
) {

    fun getRecent(rawSize: Int?, userId: Long): List<RecentFoodDTO> {
        val size = (rawSize ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE)
        val histories = foodSearchHistoryRepository.findRecentWithFood(userId, PageRequest.of(0, size))

        val categories = foodCategoryReader.loadPrimaryByFoodIds(histories.mapNotNull { it.food.id })
        return histories.map { it.toDTO(categories[it.food.id]) }
    }

    @Transactional
    fun addRecent(foodExternalId: String, userId: Long): RecentFoodDTO {
        // 형식이 잘못된 UUID는 존재할 수 없는 음식과 동일하게 취급(열거 단서 차단)
        val externalId = parseUuid(foodExternalId) ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        val food = foodRepository.findByExternalId(externalId)
            ?.takeIf { it.isVisibleTo(userId) }
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)

        val now = LocalDateTime.now()
        val foodId = food.id!!
        val history = foodSearchHistoryRepository.findByUserIdAndFoodId(userId, foodId)
            ?.apply { touch(now) }
            ?: foodSearchHistoryRepository.save(FoodSearchHistory(userId = userId, food = food, searchedAt = now))

        enforceLimit(userId)

        val category = foodCategoryReader.loadPrimaryByFoodIds(listOf(foodId))[foodId]
        return history.toDTO(category)
    }

    @Transactional
    fun deleteRecent(recentId: Long, userId: Long) {
        val history = foodSearchHistoryRepository.findByIdAndUserId(recentId, userId)
            ?: throw GeneralException(FoodErrorCode.RECENT_NOT_FOUND)
        foodSearchHistoryRepository.delete(history)
    }

    @Transactional
    fun deleteAllRecent(userId: Long) {
        foodSearchHistoryRepository.deleteByUserId(userId)
    }

    // 상한 초과 시 오래된 것부터 삭제 (id 목록은 최근순 → 상한 이후가 삭제 대상)
    private fun enforceLimit(userId: Long) {
        val ids = foodSearchHistoryRepository.findIdsByUserIdOrderByRecent(userId)
        if (ids.size > RETENTION_LIMIT) {
            foodSearchHistoryRepository.deleteAllById(ids.drop(RETENTION_LIMIT))
        }
    }

    private fun parseUuid(value: String): UUID? =
        try {
            UUID.fromString(value.trim())
        } catch (e: IllegalArgumentException) {
            null
        }

    // 노출 범위: 공개 카탈로그 ∪ 본인 비공개 음식
    private fun Food.isVisibleTo(userId: Long): Boolean =
        (source in PUBLIC_SOURCES && visibility == FoodVisibility.PUBLIC) ||
            (visibility == FoodVisibility.PRIVATE && ownerUserId == userId)

    private fun FoodSearchHistory.toDTO(category: String?) =
        RecentFoodDTO(
            recentId = id!!,
            foodExternalId = food.externalId.toString(),
            name = food.name,
            category = category,
            searchedAt = searchedAt,
        )

    companion object {
        const val DEFAULT_SIZE = 10
        const val MAX_SIZE = 50
        const val RETENTION_LIMIT = 10
        private val PUBLIC_SOURCES = setOf(FoodSource.SEED, FoodSource.CURATED)
    }
}
