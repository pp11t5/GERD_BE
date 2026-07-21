package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.CachedJudgment
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * 판정 결과를 24시간 동안 캐시
 * 키는 (음식 × 사용자 건강 상태)의 해시값이라, 온보딩이나 grounding이 바뀌면 키도 달라져 자동으로 무효화됨
 */
@Component
class JudgmentCache {

    private val cache: Cache<String, CachedJudgment> = Caffeine.newBuilder()
        .maximumSize(MAX_ENTRIES)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .recordStats()
        .build()

    // 캐시에 없으면 loader를 호출해 채우며, loader가 null을 반환하면 캐시에 저장하지 않음
    fun get(key: String, loader: (String) -> CachedJudgment?): CachedJudgment? {
        cache.getIfPresent(key)?.let { return it }
        val result = loader(key) ?: return null
        cache.put(key, result)
        return result
    }

    companion object {
        // 일 사용자 수 × 평균 판정 음식 수 기준 여유 있게 잡은 상한
        // 이 수치에 근접하면 키 설계를 점검해야 함
        private const val MAX_ENTRIES = 10_000L
    }
}
