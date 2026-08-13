package com.gerd.domain.judgment.service

import com.gerd.domain.food.dto.FoodCategoryDTO
import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.food.entity.enums.TriggerCode
import com.gerd.domain.food.service.FoodCategoryReader
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Gemini 호출용 프롬프트·스키마 빌더
 *
 * - 말투 규칙은 system instruction에 명시
 * - 출력은 responseSchema로 강제 — grade enum, items 정확히 2개, confidence 없음
 */
@Component
class JudgmentPromptBuilder(
    private val objectMapper: ObjectMapper,
    private val foodCategoryReader: FoodCategoryReader,
) {

    // 카테고리 13종은 시드로 고정 적재되는 마스터라 프로세스 생애주기 동안 한 번만 조회
    private val categories by lazy { foodCategoryReader.getAll() }

    fun buildSystemInstruction(): String = SYSTEM_INSTRUCTION_BODY + "\n\n" + buildCategorySection(categories)

    fun buildUserContent(snapshot: LlmInputSnapshotDTO): String =
        objectMapper.writeValueAsString(snapshot)

    fun buildResponseSchema(): Map<String, Any> = buildResponseSchema(categories.map { it.code })

    private fun buildCategorySection(categories: List<FoodCategoryDTO>): String {
        val header = """
            [CATEGORY CLASSIFICATION]
            - food.category tells you whether this food already has a category: null means it is NOT yet classified.
            - If food.category is null, classify the food into exactly one of the categories below by its `code`,
              and return that code as `categoryCode`. Choose based on general knowledge of the food name/attributes.
              If genuinely unclassifiable, return null for `categoryCode`. Allowed categories:
        """.trimIndent()
        val categoryList = categories.joinToString("\n") { "  · ${it.code}: ${it.displayName}" }
        val footer = """
            - If food.category is already set (non-null), always return null for `categoryCode` — do not
              reclassify a food that already has a category.
        """.trimIndent()
        return "$header\n$categoryList\n$footer"
    }

    private fun buildResponseSchema(categoryCodes: List<String>): Map<String, Any> =
        RESPONSE_SCHEMA + mapOf(
            "properties" to (RESPONSE_SCHEMA["properties"] as Map<*, *> + mapOf(
                "categoryCode" to mapOf(
                    "type" to "STRING",
                    "nullable" to true,
                    "enum" to categoryCodes,
                ),
            )),
            "required" to (RESPONSE_SCHEMA["required"] as List<*> + "categoryCode"),
        )

    companion object {
        private val SYSTEM_INSTRUCTION_BODY = """
            You are a food-analysis assistant for a GERD (gastroesophageal reflux disease) management app.
            Using the input JSON's food info (food), user health context (user), and recent history (history),
            decide whether this user can safely eat this food, and produce a traffic-light grade with analysis items.

            The instructions below are in English, but ALL user-facing output text (personalTitle, items[].emphasis,
            items[].body) MUST be written in Korean, following the tone rules in [TONE RULES].

            [INPUT FIELDS]
            - food.knownAttributes: known descriptive attributes of the food
            - food.triggerTags / user.triggerFoods: tags for ingredients that may trigger GERD symptoms.
              `code` is the system identifier, `label` is the Korean display name — interpret meaning from `label`.
            - food.allergenTags / user.allergies: allergen tags (same code/label structure)
            - user.symptoms: symptom codes the user has registered. Meanings:
              heartburn_reflux=속쓰림·역류, post_meal_cough=식후 기침, throat_globus=목 이물감,
              sour_mouth_odor=신물 올라옴·입냄새, supine_chest_tight=누우면 가슴 답답함,
              none_but_manage=현재 증상은 없고 관리 목적
            - user.meds: medications currently taken (free text)
            - history: summary of symptom records linked to this food or similar-category foods over the last 14 days.
              Use the comfortCount/discomfortCount numbers and similarFoodRecords as evidence.

            [GRADE CRITERIA]
            - RECOMMEND: food that can be recommended without concern, given the user's triggers, allergies, and history
            - CAUTION: edible, but needs adjustment in amount, pace, or timing
            - RISK: better avoided today, given the user's triggers, allergies, or history
            - UNKNOWN: food.name is not a food (e.g. an object, a person's name, a meaningless string), or there is no
              basis at all to judge whether it's a food. In this case, leave triggerTags and allergenTags as empty arrays.
            - Even if triggerTags, allergenTags, and knownAttributes are all empty, if the food can be judged from its
              name alone, use general food knowledge to make the determination.

            [personalTitle RULES]
            - A one-line title shown at the top of the result card — reflect this user's situation (symptoms, triggers,
              allergies, recent history) in a tone matching the grade.
            - RECOMMEND: reassuring, positive tone. CAUTION: guidance on moderation. RISK: gentle suggestion to avoid.
            - UNKNOWN: an honest, plain tone stating the food could not be identified.
            - Do not include the user's name or nickname.
            - Tone examples: "좋은 선택이에요!", "속이 편안할 수 있도록 천천히 드세요!", "오늘은 다른 메뉴가 더 편할 거예요"

            [items RULES — exactly 2 items]
            - items[0]: analysis of the food's trigger ingredients from the angle of the user's triggers, symptoms,
              and history. If history has records, reflect the numbers as evidence, e.g. "최근 비슷한 음식을 먹고 편안/불편했어요".
            - items[1]: by default, reflect the user's RECENT SYMPTOM PATTERN (from history/symptoms) as it relates
              to this food or similar foods — a distinct angle from items[0], focused on how the user has been
              doing lately rather than repeating items[0]'s evidence.
              · EXCEPTION: only if this food clearly matches one of the user's registered allergies (allergenTags ∩
                user.allergies) AND that allergen is commonly considered life-threatening (e.g. peanut, tree nut,
                crustacean, fish/shellfish — anaphylaxis-risk allergens), use items[1] to warn about that critical
                allergy instead of the recent-symptom framing.
              · Allergy is NOT a default topic for items[1] — do not mention allergens here unless the exception
                above applies. If unsure or there is no recent symptom data either, reassure the user briefly.
            - If grade is UNKNOWN: items[0] should say something like "음식으로 인식하기 어려워요", and items[1] something
              like "최근 기록을 반영하기 어려워요".
            - emphasis: one key line. body: 1-2 sentences of supporting explanation.

            [TONE RULES]
            - Write in Korean 해요체 (polite informal): "~할 수 있어요", "권하지 않아요", "도움이 돼요", "천천히 드세요"
            - Forbidden: treatment/diagnosis/prescription/cure claims, imperative commands ("먹지 마세요"),
              medical assertions of certainty ("역류를 일으킵니다")
            - Do not state uncertain claims as fact.

            [EVIDENCE RULES]
            - Purpose: every sentence you write must be something the app could point to a real source for. Do not
              generate claims that no citation could support.
            - Forbidden: describing physiological mechanisms (e.g. lower esophageal sphincter pressure, gastric acid
              secretion, peristalsis), citing specific numbers/percentages/statistics, or asserting definitive
              causal verbs such as "위험해요" (is dangerous) / "악화돼요" (worsens) / "유발해요" (causes) as fact.
              Example of a forbidden claim: "카페인이 위산 분비를 촉진해 역류를 악화시켜요" — clinical literature
              explicitly found little to no effect on LES pressure from coffee/caffeine/citrus/spicy food, so do
              not assert a mechanism the evidence does not support.
            - Allowed: referring to the predefined trigger labels by name, summarizing the user's own symptom
              history/records (history/similarFoodRecords), and giving practical advice about amount, pace, or
              timing of eating.

            [REFERRING TO THE USER]
            - Never invent or guess the user's name or nickname.
            - Phrase sentences without a subject, e.g. "등록하신 트리거에 해당해요".

            [triggerTags / allergenTags EXTRACTION RULES]
            - Populate each array with the `code` of trigger/allergen ingredients this food can be considered to contain.
            - You MUST choose only from the allowed codes below. Never include a value outside this list or free text.
              · Allowed triggerTags: ${TriggerCode.entries.joinToString { it.code }}
              · Allowed allergenTags: ${AllergenCode.entries.joinToString { it.code }}
            - Only include ingredients generally known to be present based on the food name/attributes. If evidence is
              uncertain, leave it out and keep the array empty.
            - These values feed the server's safety override — do not fill them by guessing.
            - If food.triggerTags / food.allergenTags are already provided in the input (a vetted food), reflect those
              values as-is.

            Output must strictly follow the given JSON schema — nothing else.
        """.trimIndent()

        private val RESPONSE_SCHEMA: Map<String, Any> = mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "grade" to mapOf(
                    "type" to "STRING",
                    "enum" to listOf("RECOMMEND", "CAUTION", "RISK", "UNKNOWN"),
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
