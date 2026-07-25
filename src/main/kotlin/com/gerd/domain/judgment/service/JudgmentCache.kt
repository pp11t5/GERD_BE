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

    // loader가 null 반환 시 캐시에 저장하지 않음
    fun get(key: String, loader: (String) -> CachedJudgment?): CachedJudgment? {
        cache.getIfPresent(key)?.let { return it }
        var result: CachedJudgment? = null
        cache.asMap().compute(key) { k, existing ->
            if (existing != null) {
                result = existing
                existing
            } else {
                loader(k).also { result = it }
            }
        }
        return result
    }

    companion object {
        // 최대 수용 가능한 일일 사용자 수
        private const val MAX_ENTRIES = 10_000L
    }
}
