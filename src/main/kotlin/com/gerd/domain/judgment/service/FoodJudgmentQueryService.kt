package com.gerd.domain.judgment.service

import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.TriggerCode
import com.gerd.domain.judgment.dto.CachedJudgment
import com.gerd.domain.judgment.dto.JudgmentContext
import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.domain.judgment.dto.TextJudgmentResponseDTO
import com.gerd.domain.judgment.dto.UserContext
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import org.springframework.stereotype.Service

/**
 * 신호등 판정 파이프라인 오케스트레이션 (spec §4)
 *
 * ⓪ 출처 게이트 → 캐시 조회 → ① LLM 판정 → ② 안전 오버라이드 → ③ 조립·캐시
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
    private val judgmentGeminiAdapter: JudgmentGeminiAdapter,
    private val safetyOverrideRule: SafetyOverrideRule,
    private val judgmentResponseAssembler: JudgmentResponseAssembler,
) {

    fun getJudgment(foodExternalId: String, userId: Long): Pair<JudgmentResponseDTO, Boolean> {
        val context = judgmentContextReader.load(foodExternalId, userId)

        // ⓪ 출처 게이트: 유저 입력 음식은 검수 라벨·grounding이 없어 LLM에 줄 근거가 없다 → 환각 방지 위해 즉시 폴백(CAUTION) (ADR-0003)
        if (context.food.source == FoodSource.USER) {
            return judgmentResponseAssembler.assembleFallback(context) to false
        }

        val snapshot = judgmentSnapshotFactory.create(context)
        val key = judgmentCacheKeyFactory.createKey(requireNotNull(context.food.id) { "영속 음식은 id를 가진다" }, snapshot)

        // loader 실행 여부로 캐시 HIT를 판별한다 (실행됐다면 MISS)
        var loaderRan = false
        val cached = judgmentCache.get(key) {
            loaderRan = true
            judge(context, snapshot)
        } ?: return judgmentResponseAssembler.assembleFallback(context) to false

        return judgmentResponseAssembler.toResponse(cached) to !loaderRan
    }

    fun getJudgmentByText(foodText: String, userId: Long): Pair<TextJudgmentResponseDTO, Boolean> {
        val userContext = judgmentContextReader.loadUserContext(userId)
        val snapshot = judgmentSnapshotFactory.createForText(foodText, userContext)
        val key = judgmentCacheKeyFactory.createTextKey(snapshot)

        var loaderRan = false
        val cached = judgmentCache.get(key) {
            loaderRan = true
            judgeText(foodText, snapshot, userContext)
        } ?: return judgmentResponseAssembler.assembleTextFallback(foodText) to false

        return judgmentResponseAssembler.toTextResponse(cached) to !loaderRan
    }

    private fun judgeText(foodText: String, snapshot: LlmInputSnapshotDTO, userContext: UserContext): CachedJudgment? {
        val llmJudgment = judgmentGeminiAdapter.generateJudgment(
            systemInstruction = judgmentPromptBuilder.buildSystemInstruction(),
            userContent = judgmentPromptBuilder.buildUserContent(snapshot),
            responseSchema = judgmentPromptBuilder.buildResponseSchema(),
        ) ?: return null

        // 텍스트 음식은 DB 태그가 없어, LLM이 추출한 코드(스키마 enum 제한)를 음식 태그로 써서 안전 오버라이드(②)를 발동한다.
        // 스키마 밖 코드는 방어적으로 한 번 더 거른다.
        // TODO: 이 매칭은 user_allergens / allergens(+ trigger_labels) 마스터가 실DB에 시딩돼야 실제로 잡힌다 — 시드 데이터 채우기
        val override = safetyOverrideRule.apply(
            llmGrade = llmJudgment.grade,
            foodTriggers = llmJudgment.triggerTags.toValidTags(TRIGGER_CODES),
            foodAllergens = llmJudgment.allergenTags.toValidTags(ALLERGEN_CODES),
            userTriggers = userContext.userTriggers,
            userAllergens = userContext.userAllergens,
        )

        return judgmentResponseAssembler.assembleTextCacheable(foodText, llmJudgment, override)
    }

    // ① LLM → ② 안전 오버라이드 → ③ 조립. 실패는 null로 반환해 캐시에 남기지 않는다
    private fun judge(context: JudgmentContext, snapshot: LlmInputSnapshotDTO): CachedJudgment? {
        val llmJudgment = judgmentGeminiAdapter.generateJudgment(
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
            // 정적 큐레이션은 원 음식의 태그 회피만 보장한다 — "안심되는 대체 식단"이려면
            // 이 사용자의 트리거·알레르겐을 보유한 후보를 노출 전에 제외해야 한다.
            // 사용자 태그는 캐시 키(입력 스냅샷)에 포함돼 있어 필터 결과도 키와 함께 자연 무효화된다
            val userCodes = (context.userTriggers + context.userAllergens).map { it.code }.toSet()
            judgmentContextReader.loadSubstituteCandidates(requireNotNull(context.food.id) { "영속 음식은 id를 가진다" })
                .filter { candidate -> candidate.tagCodes.none(userCodes::contains) }
                .map { SubstituteDTO(it.foodExternalId, it.name) }
        } else {
            emptyList()
        }

        return judgmentResponseAssembler.assembleCacheable(context, llmJudgment, override, substitutes)
    }

    // LLM이 추출한 code를 안전 룰 입력 TagDTO로 변환 — 허용 code만 통과시키고 중복 제거.
    // label은 매칭(코드 기준)·텍스트 응답 카피에 쓰이지 않아 code로 채운다
    private fun List<String>.toValidTags(validCodes: Set<String>): List<TagDTO> =
        filter { it in validCodes }.distinct().map { TagDTO(it, it) }

    companion object {
        private val TRIGGER_CODES = TriggerCode.entries.map { it.code }.toSet()
        private val ALLERGEN_CODES = AllergenCode.entries.map { it.code }.toSet()
    }
}
