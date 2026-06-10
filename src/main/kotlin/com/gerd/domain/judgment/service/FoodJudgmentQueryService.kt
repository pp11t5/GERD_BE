package com.gerd.domain.judgment.service

import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.judgment.client.GeminiClient
import com.gerd.domain.judgment.dto.CachedJudgment
import com.gerd.domain.judgment.dto.JudgmentContext
import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import org.springframework.stereotype.Service

/**
 * 신호등 판정 파이프라인 오케스트레이션 (spec §4)
 *
 * ⓪ 출처 게이트 → 캐시 조회 → ① LLM 판정 → ② 안전 오버라이드 → ③ 조립·캐시 → 닉네임 치환
 *
 * 클래스에 @Transactional을 두지 않는다 — 캐시 미스 시 LLM 호출(수 초) 동안
 * DB 커넥션을 점유하면 안 되며, DB 읽기는 Reader의 짧은 트랜잭션으로 분리돼 있다
 */
@Service
class FoodJudgmentQueryService(
    private val judgmentContextReader: JudgmentContextReader,
    private val judgmentSnapshotFactory: JudgmentSnapshotFactory,
    private val judgmentCacheKeyFactory: JudgmentCacheKeyFactory,
    private val judgmentCache: JudgmentCache,
    private val judgmentPromptBuilder: JudgmentPromptBuilder,
    private val geminiClient: GeminiClient,
    private val safetyOverrideRule: SafetyOverrideRule,
    private val judgmentResponseAssembler: JudgmentResponseAssembler,
) {

    fun getJudgment(foodExternalId: String, userId: Long, nickname: String?): JudgmentResponseDTO {
        val context = judgmentContextReader.load(foodExternalId, userId)

        // ⓪ 출처 게이트: 유저 입력 음식은 검수 라벨·grounding이 없어 LLM에 줄 근거가 없다 → 환각 방지 위해 즉시 ⚪ (ADR-0003)
        if (context.food.source == FoodSource.USER) {
            return judgmentResponseAssembler.assembleUnknownFallback(context)
        }

        val snapshot = judgmentSnapshotFactory.create(context)
        val key = judgmentCacheKeyFactory.createKey(requireNotNull(context.food.id) { "영속 음식은 id를 가진다" }, snapshot)

        // loader 실행 여부로 캐시 HIT를 판별한다 (실행됐다면 MISS)
        var loaderRan = false
        val cached = judgmentCache.get(key) {
            loaderRan = true
            judge(context, snapshot)
        } ?: return judgmentResponseAssembler.assembleUnknownFallback(context)

        return judgmentResponseAssembler.toResponse(cached, nickname, cachedFlag = !loaderRan)
    }

    // ① LLM → ② 안전 오버라이드 → ③ 조립. 실패는 null로 반환해 캐시에 남기지 않는다
    private fun judge(context: JudgmentContext, snapshot: LlmInputSnapshotDTO): CachedJudgment? {
        val llmJudgment = geminiClient.generateJudgment(
            systemInstruction = judgmentPromptBuilder.buildSystemInstruction(),
            userContent = judgmentPromptBuilder.buildUserContent(snapshot),
            responseSchema = judgmentPromptBuilder.buildResponseSchema(),
        ) ?: return null

        val override = safetyOverrideRule.apply(
            llmGrade = llmJudgment.grade,
            foodTriggers = context.foodTriggers,
            foodAllergens = context.foodAllergens,
            userTriggers = context.userTriggers,
            userAllergens = context.userAllergens,
        )

        // 대체식단은 CAUTION/RISK에만 노출 — LLM 호출이 끝난 뒤 별도 짧은 트랜잭션으로 조회한다
        val substitutes = if (override.grade == JudgmentGrade.CAUTION || override.grade == JudgmentGrade.RISK) {
            judgmentContextReader.loadSubstitutes(requireNotNull(context.food.id) { "영속 음식은 id를 가진다" })
        } else {
            emptyList()
        }

        return judgmentResponseAssembler.assembleCacheable(context, llmJudgment, override, substitutes)
    }
}
