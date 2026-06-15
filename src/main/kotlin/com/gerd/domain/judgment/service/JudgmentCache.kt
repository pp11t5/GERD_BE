package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.CachedJudgment
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * 판정 결과 24h 인메모리 캐시 (ADR-0012, spec §7)
 *
 * - 명시적 무효화 없음 — 온보딩 변경·grounding 수정은 입력 스냅샷 해시(키)가 바뀌어 자연 무효화된다
 * - 동일 키 동시 요청 시 loader가 한 번만 실행돼 LLM 중복 호출(RPD 낭비)과 스탬피드를 막는다
 */
@Component
class JudgmentCache {

    private val cache: Cache<String, CachedJudgment> = Caffeine.newBuilder()
        .maximumSize(MAX_ENTRIES)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .recordStats()
        .build()

    // loader가 null을 반환하면 저장 없이 null을 돌려준다 — LLM 실패를 캐시에 남기지 않는 계약.
    // Caffeine은 loader가 예외를 던지면 매핑을 기록하지 않으므로 내부 sentinel 예외로 null을 표현한다
    fun get(key: String, loader: (String) -> CachedJudgment?): CachedJudgment? =
        try {
            cache.get(key) { k -> loader(k) ?: throw LoadFailedException() }
        } catch (e: LoadFailedException) {
            null
        }

    // 제어 흐름 전용이라 스택트레이스를 채우지 않는다
    private class LoadFailedException : RuntimeException(null, null, false, false)

    companion object {
        // LLM RPD 1,000 캡 + TTL 24h로 정상 상태 라이브 엔트리는 ≤1,000개 — 10,000은 닿지 않는
        // 가드레일이며, 축출이 관측되면 키 폭발(해시 입력 불안정) 버그 신호다 (spec §7)
        private const val MAX_ENTRIES = 10_000L
    }
}
