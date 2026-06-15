package com.gerd.domain.judgment.service

import com.gerd.domain.auth.util.HashUtils
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO
import org.springframework.stereotype.Component
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper

/**
 * 캐시 키 = foodId + LLM 입력 스냅샷 해시 (spec §7)
 *
 * "입력이 같으면 출력 재사용이 정당하다"가 키 정의에서 보장된다 — 온보딩 변경·grounding 수정은
 * 스냅샷이 바뀌어 자연 무효화되므로 명시적 무효화가 없다
 */
@Component
class JudgmentCacheKeyFactory {

    // 프로퍼티 순서까지 고정해 직렬화 결과를 결정적으로 만든다 (리스트 정렬은 SnapshotFactory 책임)
    private val canonicalMapper: JsonMapper = JsonMapper.builder()
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .build()

    fun createKey(foodId: Long, snapshot: LlmInputSnapshotDTO): String =
        "$foodId:${HashUtils.sha256(canonicalMapper.writeValueAsString(snapshot))}"

    fun createTextKey(snapshot: LlmInputSnapshotDTO): String =
        "text:${HashUtils.sha256(canonicalMapper.writeValueAsString(snapshot))}"
}
