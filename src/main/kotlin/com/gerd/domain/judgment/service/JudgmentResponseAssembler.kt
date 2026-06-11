package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.CachedJudgment
import com.gerd.domain.judgment.dto.JudgmentContext
import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.JudgmentItemDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.LlmJudgmentDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.judgment.service.SafetyOverrideRule.OverrideResult
import org.springframework.stereotype.Component

// 판정 응답 조립 — LLM 제목 채택/폴백 판단(spec §3)과 고정 폴백 카피를 담당한다
@Component
class JudgmentResponseAssembler {

    // ② 오버라이드까지 끝난 최종 등급으로 캐시 value를 조립한다
    fun assembleCacheable(
        context: JudgmentContext,
        llmJudgment: LlmJudgmentDTO,
        override: OverrideResult,
        substitutes: List<SubstituteDTO>,
    ): CachedJudgment {
        // LLM이 UNKNOWN을 낸 경우 같이 생성된 items는 버린다 — "왜 모르는지"를 LLM이 창작하지 않게(환각 차단, spec §4)
        val baseItems = if (llmJudgment.grade == JudgmentGrade.UNKNOWN) {
            UNKNOWN_FALLBACK_ITEMS
        } else {
            llmJudgment.items.map { JudgmentItemDTO(it.emphasis, it.body) }
        }
        // 알레르겐 매치는 LLM이 언급을 놓쳐도 슬롯 [1](알레르기·복용약)을 결정적 카피로 보장한다
        val items = if (override.allergenMatches.isNotEmpty()) {
            val labels = override.allergenMatches.joinToString(", ") { it.label }
            baseItems.toMutableList().apply {
                this[ALLERGY_SLOT] = JudgmentItemDTO(
                    emphasis = "알레르기 성분이 들어 있어요",
                    body = "등록하신 알레르기($labels)에 해당하는 성분이 포함돼 있어요. 권하지 않아요.",
                )
            }
        } else {
            baseItems
        }

        return CachedJudgment(
            foodExternalId = context.foodExternalId,
            foodName = context.food.name,
            category = context.category,
            grade = override.grade,
            personalTitle = resolveTitle(llmJudgment, override),
            items = items,
            substitutes = substitutes,
        )
    }

    // LLM 제목은 LLM이 판정한 등급의 톤으로 쓰였다 — 오버라이드로 등급이 바뀌면 톤이 어긋나므로 고정 제목으로 폴백.
    // UNKNOWN은 items와 같은 이유(왜 모르는지 창작 금지)로 LLM 제목을 쓰지 않는다
    private fun resolveTitle(llmJudgment: LlmJudgmentDTO, override: OverrideResult): String =
        llmJudgment.personalTitle
            ?.takeIf { it.isNotBlank() && llmJudgment.grade != JudgmentGrade.UNKNOWN && override.grade == llmJudgment.grade }
            ?: FALLBACK_TITLES.getValue(override.grade)

    fun toResponse(cached: CachedJudgment, cachedFlag: Boolean): JudgmentResponseDTO =
        JudgmentResponseDTO(
            foodExternalId = cached.foodExternalId,
            foodName = cached.foodName,
            category = cached.category,
            grade = cached.grade,
            personalTitle = cached.personalTitle,
            items = cached.items,
            stateRecords = emptyList(),
            substitutes = cached.substitutes,
            disclaimer = DISCLAIMER,
            cached = cachedFlag,
        )

    // ⓪ 출처 게이트(유저 입력 음식)와 LLM 호출 실패가 공유하는 UNKNOWN 폴백 — 캐시하지 않는다
    fun assembleUnknownFallback(context: JudgmentContext): JudgmentResponseDTO =
        JudgmentResponseDTO(
            foodExternalId = context.foodExternalId,
            foodName = context.food.name,
            category = context.category,
            grade = JudgmentGrade.UNKNOWN,
            personalTitle = FALLBACK_TITLES.getValue(JudgmentGrade.UNKNOWN),
            items = UNKNOWN_FALLBACK_ITEMS,
            stateRecords = emptyList(),
            substitutes = emptyList(),
            disclaimer = DISCLAIMER,
            cached = false,
        )

    companion object {
        const val DISCLAIMER = "본 앱은 진단·치료 서비스가 아닙니다."

        // items 2슬롯 고정: [0]=트리거·증상, [1]=알레르기·복용약 (기획 PItem-1 / PItem-2)
        private const val ALLERGY_SLOT = 1

        // LLM 제목을 쓸 수 없는 경우(누락·공백·UNKNOWN·등급 강등)의 등급별 고정 제목
        private val FALLBACK_TITLES = mapOf(
            JudgmentGrade.RECOMMEND to "좋은 선택이에요!",
            JudgmentGrade.CAUTION to "속이 편안할 수 있도록 천천히 드세요!",
            JudgmentGrade.RISK to "오늘은 다른 메뉴가 더 편할 거예요",
            JudgmentGrade.UNKNOWN to "이 음식은 정보가 충분하지 않아요",
        )

        // UNKNOWN의 items는 LLM 생성 없이 고정 카피 — 두 발생 경로(⓪게이트·LLM UNKNOWN) 공용 (spec §4)
        private val UNKNOWN_FALLBACK_ITEMS = listOf(
            JudgmentItemDTO(
                emphasis = "정보가 부족해요",
                body = "이 음식은 아직 분석할 수 있는 정보가 충분하지 않아요. 처음이라면 소량부터 천천히 드셔보는 게 좋아요.",
            ),
            JudgmentItemDTO(
                emphasis = "알레르기 확인이 어려워요",
                body = "등록하신 알레르기 성분이 포함됐는지 확인할 수 없어요. 성분표를 한 번 확인해 보세요.",
            ),
        )
    }
}
