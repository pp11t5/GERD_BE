package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.domain.judgment.dto.UserContext
import com.gerd.global.fixture.FoodFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JudgmentSnapshotFactoryTest {

    private val factory = JudgmentSnapshotFactory()

    private val caffeine = TagDTO("caffeine", "카페인")
    private val milk = TagDTO("milk", "우유")
    private val fat = TagDTO("fat", "고지방")

    @Nested
    inner class `ID 기반 스냅샷 생성` {

        @Test
        fun `음식·사용자 태그가 모두 있으면 스냅샷에 그대로 담긴다`() {
            val context = baseContext(
                foodTriggers = listOf(caffeine, fat),
                foodAllergens = listOf(milk),
                userTriggers = listOf(caffeine),
                userAllergens = listOf(milk),
            )

            val snapshot = factory.create(context)

            assertThat(snapshot.food.name).isEqualTo("된장찌개")
            assertThat(snapshot.food.triggerTags).containsExactly(caffeine, fat)
            assertThat(snapshot.food.allergenTags).containsExactly(milk)
            assertThat(snapshot.user.triggerFoods).containsExactly(caffeine)
            assertThat(snapshot.user.allergies).containsExactly(milk)
        }

        @Test
        fun `triggerTags·allergenTags가 비어있으면 빈 리스트로 스냅샷에 담긴다`() {
            val context = baseContext()

            val snapshot = factory.create(context)

            assertThat(snapshot.food.triggerTags).isEmpty()
            assertThat(snapshot.food.allergenTags).isEmpty()
            assertThat(snapshot.food.knownAttributes).isEmpty()
        }

        @Test
        fun `food description이 있으면 knownAttributes에 포함되고 없으면 빈 리스트다`() {
            val withDesc = baseContext(description = "고나트륨 발효식품")
            val withoutDesc = baseContext(description = null)

            assertThat(factory.create(withDesc).food.knownAttributes).containsExactly("고나트륨 발효식품")
            assertThat(factory.create(withoutDesc).food.knownAttributes).isEmpty()
        }

        @Test
        fun `태그 리스트는 code 기준 오름차순 정렬된다 — 캐시 키 안정성`() {
            val context = baseContext(
                foodTriggers = listOf(fat, caffeine),   // fat > caffeine 역순
                userTriggers = listOf(milk, caffeine),   // milk > caffeine 역순
            )

            val snapshot = factory.create(context)

            assertThat(snapshot.food.triggerTags.map { it.code }).isSorted
            assertThat(snapshot.user.triggerFoods.map { it.code }).isSorted
        }
    }

    @Nested
    inner class `텍스트 기반 스냅샷 생성` {

        @Test
        fun `음식 이름만 있고 태그는 모두 비어있는 스냅샷을 생성한다`() {
            val snapshot = factory.createForText("된장찌개", userContext())

            assertThat(snapshot.food.name).isEqualTo("된장찌개")
            assertThat(snapshot.food.category).isNull()
            assertThat(snapshot.food.triggerTags).isEmpty()
            assertThat(snapshot.food.allergenTags).isEmpty()
            assertThat(snapshot.food.knownAttributes).isEmpty()
        }

        @Test
        fun `사용자 컨텍스트는 텍스트 기반에서도 그대로 담긴다`() {
            val ctx = userContext(triggers = listOf(caffeine), allergens = listOf(milk))

            val snapshot = factory.createForText("아메리카노", ctx)

            assertThat(snapshot.user.triggerFoods).containsExactly(caffeine)
            assertThat(snapshot.user.allergies).containsExactly(milk)
        }
    }

    private fun baseContext(
        foodTriggers: List<TagDTO> = emptyList(),
        foodAllergens: List<TagDTO> = emptyList(),
        userTriggers: List<TagDTO> = emptyList(),
        userAllergens: List<TagDTO> = emptyList(),
        description: String? = null,
    ) = com.gerd.domain.judgment.dto.JudgmentContext(
        food = FoodFixture.food(name = "된장찌개", description = description),
        category = "soup_stew",
        foodTriggers = foodTriggers,
        foodAllergens = foodAllergens,
        userTriggers = userTriggers,
        userAllergens = userAllergens,
        medications = emptyList(),
        symptomCodes = emptyList(),
    )

    private fun userContext(
        triggers: List<TagDTO> = emptyList(),
        allergens: List<TagDTO> = emptyList(),
    ) = UserContext(
        userTriggers = triggers,
        userAllergens = allergens,
        medications = emptyList(),
        symptomCodes = emptyList(),
    )
}
