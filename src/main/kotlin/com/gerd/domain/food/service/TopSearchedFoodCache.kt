package com.gerd.domain.food.service

import com.gerd.domain.food.dto.FoodSummaryDTO
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * 인기 검색어 상위 3개는 전체 유저 공통 값이라 홈 진입마다 재계산할 필요 없음
 * 누적 카운트 기반 랭킹이라 분 단위로는 안 바뀜 — TTL만으로 충분함
 */
@Component
class TopSearchedFoodCache {

    private val cache: Cache<String, List<FoodSummaryDTO>> = Caffeine.newBuilder()
        .expireAfterWrite(TTL_MINUTES, TimeUnit.MINUTES)
        .build()

    fun get(): List<FoodSummaryDTO>? = cache.getIfPresent(KEY)

    fun put(foods: List<FoodSummaryDTO>) {
        cache.put(KEY, foods)
    }

    companion object {
        private const val KEY = "top3"
        private const val TTL_MINUTES = 30L
    }
}
