package com.gerd.global.ratelimit

import com.gerd.global.config.properties.RateLimitProperties.Policy
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bucket
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Component
class RateLimitBuckets {
    private val cachesByPolicy = ConcurrentHashMap<String, Cache<String, Bucket>>()

    fun tryConsume(
        policy: Policy,
        key: String,
    ): RateLimitResult {
        val cache = cachesByPolicy.computeIfAbsent(policy.name) { newCache(policy) }
        val bucket = cache.get(key) { newBucket(policy) }
        val probe = bucket.tryConsumeAndReturnRemaining(1)
        if (probe.isConsumed) {
            return RateLimitResult(allowed = true)
        }
        val waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.nanosToWaitForRefill) + 1
        return RateLimitResult(allowed = false, retryAfterSeconds = waitSeconds)
    }

    // 한 주기 동안 요청이 없으면 버킷은 가득 찬 상태라 지워도 동작이 같음
    private fun newCache(policy: Policy): Cache<String, Bucket> {
        val builder = Caffeine.newBuilder()
        builder.expireAfterAccess(policy.refreshPeriod)
        builder.maximumSize(MAX_KEYS_PER_POLICY)
        return builder.build()
    }

    private fun newBucket(policy: Policy): Bucket {
        val limit = policy.limitForPeriod.toLong()
        val builder = Bucket.builder()
        builder.addLimit { it.capacity(limit).refillGreedy(limit, policy.refreshPeriod) }
        return builder.build()
    }

    companion object {
        private const val MAX_KEYS_PER_POLICY = 100_000L
    }
}
