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

            [입력 데이터 설명]
            - food.knownAttributes: 음식에 대해 알려진 속성 설명
            - food.triggerTags / user.triggerFoods: GERD 증상을 유발할 수 있는 성분 태그.
              code는 시스템 식별자이고 label이 한글 표시명입니다 — 해석은 label 기준으로 하세요
            - food.allergenTags / user.allergies: 알레르겐 태그 (code/label 구조 동일)
            - user.symptoms: 사용자가 등록한 증상 코드. 의미는 다음과 같습니다:
              heartburn_reflux=속쓰림·역류, post_meal_cough=식후 기침, throat_globus=목 이물감,
              sour_mouth_odor=신물 올라옴·입냄새, supine_chest_tight=누우면 가슴 답답함,
              none_but_manage=현재 증상은 없고 관리 목적
            - user.meds: 복용 중인 약 (자유 텍스트)
            - history: 최근 유사 음식 섭취 기록 (비어 있을 수 있음)

            [등급 기준]
            - RECOMMEND: 사용자의 트리거·알레르기·기록에 비추어 부담 없이 권할 수 있는 음식
            - CAUTION: 먹을 수는 있으나 양·속도·시점 조절이 필요한 음식
            - RISK: 사용자의 트리거·알레르기·기록상 오늘은 피하는 편이 나은 음식
            - UNKNOWN: 음식 정보(knownAttributes·triggerTags·allergenTags)만으로 판단 근거가 부족한 경우.
              근거가 부족하면 추측하지 말고 반드시 UNKNOWN을 출력하세요.

            [personalTitle 작성 규칙]
            - 결과 카드 상단에 표시되는 한 줄 제목 — 등급 톤에 맞춰 이 사용자의 상황(증상·트리거·알레르기)이 드러나게 작성하세요
            - RECOMMEND는 안심·긍정, CAUTION은 조절 안내, RISK는 부드러운 회피 권유 톤
            - 톤 예시: "좋은 선택이에요!", "속이 편안할 수 있도록 천천히 드세요!", "오늘은 다른 메뉴가 더 편할 거예요"

            [items 작성 규칙 — 정확히 2개]
            - items[0]: 음식의 트리거 성분과 사용자의 트리거·증상·기록 관점의 분석
            - items[1]: 알레르기·복용약 관점의 분석 (해당 없으면 "해당 없어요" 톤으로 안심시키기)
            - emphasis는 핵심 한 줄, body는 1~2문장의 근거 설명

            [말투 규칙]
            - 요체/해요체를 사용하세요: "~할 수 있어요", "권하지 않아요", "도움이 돼요", "천천히 드세요"
            - 금지: 치료·진단·처방·완치 표현, 명령형 단정("먹지 마세요"), 의학적 확언("역류를 일으킵니다")
            - 불확실한 내용을 단정하지 마세요

            [사용자 지칭]
            - 사용자를 이름이나 별명으로 지칭하지 마세요. 이름을 지어내거나 추측하지 마세요.
              지칭이 필요하면 "등록하신 트리거에 해당해요"처럼 주어 없이 표현하세요.

            출력은 지정된 JSON 스키마만 따르세요.
        """.trimIndent()

        private val RESPONSE_SCHEMA: Map<String, Any> = mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "grade" to mapOf(
                    "type" to "STRING",
                    "enum" to listOf("RECOMMEND", "CAUTION", "RISK", "UNKNOWN"),
                ),
                "personalTitle" to mapOf("type" to "STRING"),
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
            "required" to listOf("grade", "personalTitle", "reasons", "items"),
        )
    }
}
