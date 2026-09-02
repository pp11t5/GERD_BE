package com.gerd.domain.food.service

import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.dto.RecentFoodDTO
import com.gerd.domain.food.entity.FoodSearchHistory
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodSearchHistoryRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 최근 검색어 CRUD 서비스
 *
 * - 기록: 음식 검색 API 호출 시 FoodSearchService가 호출 — (user_id, query) upsert(재검색 시 searchedAt만 갱신) + 보관 상한(10) 정리
 * - 조회/삭제: 본인 항목만
 */
@Service
@Transactional(readOnly = true)
class RecentFoodService(
    private val foodSearchHistoryRepository: FoodSearchHistoryRepository,
    private val topSearchedFoodCache: TopSearchedFoodCache,
) {

    fun getTopSearched(): List<FoodSummaryDTO> =
        topSearchedFoodCache.get() ?: loadTopSearched().also { topSearchedFoodCache.put(it) }

    private fun loadTopSearched(): List<FoodSummaryDTO> =
        foodSearchHistoryRepository.findTop3MostSearched().map {
            FoodSummaryDTO(externalId = it.getExternalId(), name = it.getName(), category = it.getCategory())
        }

    fun getRecent(rawSize: Int?, userId: Long): List<RecentFoodDTO> {
        val size = (rawSize ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE)
        return foodSearchHistoryRepository.findRecentByUserId(userId, PageRequest.of(0, size)).map { it.toDTO() }
    }

    // FoodSearchService.search() 내부에서 호출 — 검색어 자체를 최근 검색 기록으로 남긴다
    @Transactional
    fun record(query: String, userId: Long) {
        val now = LocalDateTime.now()
        foodSearchHistoryRepository.findByUserIdAndQuery(userId, query)
            ?.apply { touch(now) }
            ?: foodSearchHistoryRepository.save(FoodSearchHistory(userId = userId, query = query, searchedAt = now))

        enforceLimit(userId)
    }

    @Transactional
    fun deleteRecent(id: Long, userId: Long) {
        val history = foodSearchHistoryRepository.findByIdAndUserId(id, userId)
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

    private fun FoodSearchHistory.toDTO() =
        RecentFoodDTO(
            id = id!!,
            query = query,
            searchedAt = searchedAt,
        )

    companion object {
        const val DEFAULT_SIZE = 10
        const val MAX_SIZE = 50
        const val RETENTION_LIMIT = 10
    }
}
