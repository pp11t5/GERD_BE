package com.gerd.domain.judgment.service

import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.food.entity.enums.TriggerCode
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Gemini 호출용 프롬프트·스키마 빌더 (spec §5, §6)
 *
 * - 말투 규칙은 system instruction에 명시
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
            - history: 최근 14일 기준 이 음식 또는 비슷한 카테고리 음식과 연결된 증상 기록 요약.
              comfortCount/discomfortCount와 similarFoodRecords의 수치를 근거로 사용하세요.

            [등급 기준]
            - RECOMMEND: 사용자의 트리거·알레르기·기록에 비추어 부담 없이 권할 수 있는 음식
            - CAUTION: 먹을 수는 있으나 양·속도·시점 조절이 필요한 음식
            - RISK: 사용자의 트리거·알레르기·기록상 오늘은 피하는 편이 나은 음식
            - triggerTags·allergenTags·knownAttributes가 비어있어도 음식 이름으로 판단 가능하면 일반 식품 지식을 활용해 판정하세요.

            [personalTitle 작성 규칙]
            - 결과 카드 상단에 표시되는 한 줄 제목 — 등급 톤에 맞춰 이 사용자의 상황(증상·트리거·알레르기·최근 기록)이 드러나게 작성하세요
            - RECOMMEND는 안심·긍정, CAUTION은 조절 안내, RISK는 부드러운 회피 권유 톤
            - 사용자 이름·닉네임을 포함하지 마세요.
            - 톤 예시: "좋은 선택이에요!", "속이 편안할 수 있도록 천천히 드세요!", "오늘은 다른 메뉴가 더 편할 거예요"

            [items 작성 규칙 — 정확히 2개]
            - items[0]: 음식의 트리거 성분과 사용자의 트리거·증상·기록 관점의 분석
              history에 기록이 있으면 "최근 비슷한 음식을 먹고 편안/불편했어요"처럼 수치 근거를 반영하세요.
            - items[1]: 알레르기·복용약 관점의 분석. allergenTags가 비어있으면 음식 이름 기반으로 주요 알레르겐을 안내하되,
              불확실한 경우 "성분표를 확인해 보세요" 톤으로 작성하세요. 해당 없으면 안심시키기
            - emphasis는 핵심 한 줄, body는 1~2문장의 근거 설명

            [말투 규칙]
            - 요체/해요체를 사용하세요: "~할 수 있어요", "권하지 않아요", "도움이 돼요", "천천히 드세요"
            - 금지: 치료·진단·처방·완치 표현, 명령형 단정("먹지 마세요"), 의학적 확언("역류를 일으킵니다")
            - 불확실한 내용을 단정하지 마세요

            [사용자 지칭]
            - 이름이나 닉네임을 지어내거나 추측하지 마세요.
            - "등록하신 트리거에 해당해요"처럼 주어 없이 표현하세요.

            [triggerTags / allergenTags 추출 규칙]
            - 이 음식에 포함된 것으로 볼 수 있는 트리거·알레르겐 성분의 code를 각 배열에 담으세요.
            - 반드시 아래 허용 code에서만 고르세요. 목록에 없는 값이나 자유 텍스트는 절대 넣지 마세요.
              · triggerTags 허용: ${TriggerCode.entries.joinToString { it.code }}
              · allergenTags 허용: ${AllergenCode.entries.joinToString { it.code }}
            - 음식명·속성으로 일반적으로 포함된다고 알려진 성분만 넣으세요. 근거가 불확실하면 넣지 말고 빈 배열로 두세요.
            - 이 값은 서버의 안전 판정에 사용되니 추측으로 채우지 마세요.
            - food.triggerTags / food.allergenTags가 입력으로 이미 주어진 경우(검수된 음식)에는 그 값을 그대로 반영하면 됩니다.

            출력은 지정된 JSON 스키마만 따르세요.
        """.trimIndent()

        private val RESPONSE_SCHEMA: Map<String, Any> = mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "grade" to mapOf(
                    "type" to "STRING",
                    "enum" to listOf("RECOMMEND", "CAUTION", "RISK"),
                ),
                "personalTitle" to mapOf("type" to "STRING"),
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
                // 음식에서 추출한 트리거/알레르겐 코드 — 텍스트 판정의 안전 오버라이드 입력. enum으로 코드 집합을 강제
                "triggerTags" to mapOf(
                    "type" to "ARRAY",
                    "items" to mapOf("type" to "STRING", "enum" to TriggerCode.entries.map { it.code }),
                ),
                "allergenTags" to mapOf(
                    "type" to "ARRAY",
                    "items" to mapOf("type" to "STRING", "enum" to AllergenCode.entries.map { it.code }),
                ),
            ),
            "required" to listOf("grade", "personalTitle", "items", "triggerTags", "allergenTags"),
        )
    }
}
