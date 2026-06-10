package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Gemini 호출용 프롬프트·스키마 빌더 (spec §5, §6)
 *
 * - 말투 규칙과 UNKNOWN 출력 조건은 system instruction에 명시
 * - 출력은 responseSchema로 강제 — grade enum, items 정확히 2개, confidence 없음 (ADR-0013)
 */
@Component
class JudgmentPromptBuilder(
    private val objectMapper: ObjectMapper,
) {

    fun buildSystemInstruction(): String = SYSTEM_INSTRUCTION

    fun buildUserContent(snapshot: LlmInputSnapshotDTO): String =
        objectMapper.writeValueAsString(snapshot)

    fun buildResponseSchema(): Map<String, Any> = RESPONSE_SCHEMA

    companion object {
        private val SYSTEM_INSTRUCTION = """
            당신은 위식도역류질환(GERD) 관리 앱의 음식 분석 도우미입니다.
            입력 JSON의 음식 정보(food), 사용자 건강 컨텍스트(user), 최근 기록(history)을 근거로
            이 사용자가 이 음식을 먹어도 괜찮을지 신호등 등급과 분석 항목을 생성하세요.

            [등급 기준]
            - RECOMMEND: 사용자의 트리거·알레르기·기록에 비추어 부담 없이 권할 수 있는 음식
            - CAUTION: 먹을 수는 있으나 양·속도·시점 조절이 필요한 음식
            - RISK: 사용자의 트리거·알레르기·기록상 오늘은 피하는 편이 나은 음식
            - UNKNOWN: 음식 정보(knownAttributes·triggerTags·allergenTags)만으로 판단 근거가 부족한 경우.
              근거가 부족하면 추측하지 말고 반드시 UNKNOWN을 출력하세요.

            [items 작성 규칙 — 정확히 2개]
            - items[0]: 음식의 트리거 성분과 사용자의 트리거·증상·기록 관점의 분석
            - items[1]: 알레르기·복용약 관점의 분석 (해당 없으면 "해당 없어요" 톤으로 안심시키기)
            - emphasis는 핵심 한 줄, body는 1~2문장의 근거 설명

            [말투 규칙]
            - 요체/해요체를 사용하세요: "~할 수 있어요", "권하지 않아요", "도움이 돼요", "천천히 드세요"
            - 금지: 치료·진단·처방·완치 표현, 명령형 단정("먹지 마세요"), 의학적 확언("역류를 일으킵니다")
            - 불확실한 내용을 단정하지 마세요

            [사용자 지칭]
            - 사용자를 지칭할 때는 반드시 "{nickname}" 토큰을 그대로 사용하세요 (예: "{nickname}님이 등록한 트리거에 해당해요").
              실제 이름을 지어내거나 추측하지 마세요.

            출력은 지정된 JSON 스키마만 따르세요.
        """.trimIndent()

        private val RESPONSE_SCHEMA: Map<String, Any> = mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "grade" to mapOf(
                    "type" to "STRING",
                    "enum" to listOf("RECOMMEND", "CAUTION", "RISK", "UNKNOWN"),
                ),
                "reasons" to mapOf(
                    "type" to "ARRAY",
                    "items" to mapOf("type" to "STRING"),
                ),
                "items" to mapOf(
                    "type" to "ARRAY",
                    "minItems" to 2,
                    "maxItems" to 2,
                    "items" to mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "emphasis" to mapOf("type" to "STRING"),
                            "body" to mapOf("type" to "STRING"),
                        ),
                        "required" to listOf("emphasis", "body"),
                    ),
                ),
            ),
            "required" to listOf("grade", "reasons", "items"),
        )
    }
}
