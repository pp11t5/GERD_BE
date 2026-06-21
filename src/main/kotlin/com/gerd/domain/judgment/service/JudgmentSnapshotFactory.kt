package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.JudgmentContext
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO
import com.gerd.domain.judgment.dto.UserContext
import org.springframework.stereotype.Component

/**
 * 판정 컨텍스트 → LLM 입력 스냅샷 변환
 *
 * 스냅샷이 곧 캐시 키의 원천이라, 같은 상태면 항상 같은 직렬화 결과가 나오도록
 * 모든 리스트를 여기서 code(또는 값) 기준으로 정렬한다 — DB 조회 순서에 키가 흔들리면 안 된다
 */
@Component
class JudgmentSnapshotFactory {

    fun createForText(foodText: String, userContext: UserContext): LlmInputSnapshotDTO =
        LlmInputSnapshotDTO(
            food = LlmInputSnapshotDTO.FoodSnapshotDTO(
                name = foodText,
                category = null,
                knownAttributes = emptyList(),
                triggerTags = emptyList(),
                allergenTags = emptyList(),
            ),
            user = LlmInputSnapshotDTO.UserSnapshotDTO(
                symptoms = userContext.symptomCodes.sorted(),
                triggerFoods = userContext.userTriggers.sortedBy { it.code },
                allergies = userContext.userAllergens.sortedBy { it.code },
                meds = userContext.medications.sorted(),
            ),
        )

    fun create(context: JudgmentContext): LlmInputSnapshotDTO =
        LlmInputSnapshotDTO(
            food = LlmInputSnapshotDTO.FoodSnapshotDTO(
                name = context.food.name,
                category = context.category,
                knownAttributes = listOfNotNull(context.food.description?.takeIf { it.isNotBlank() }),
                triggerTags = context.foodTriggers.sortedBy { it.code },
                allergenTags = context.foodAllergens.sortedBy { it.code },
            ),
            user = LlmInputSnapshotDTO.UserSnapshotDTO(
                symptoms = context.symptomCodes.sorted(),
                triggerFoods = context.userTriggers.sortedBy { it.code },
                allergies = context.userAllergens.sortedBy { it.code },
                meds = context.medications.sorted(),
            ),
            history = context.history,
        )
}
