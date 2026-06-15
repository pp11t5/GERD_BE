package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.JudgmentContext
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.global.fixture.FoodFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JudgmentCacheKeyFactoryTest {

    private val snapshotFactory = JudgmentSnapshotFactory()
    private val keyFactory = JudgmentCacheKeyFactory()

    private fun context(
        userTriggers: List<TagDTO> = listOf(TagDTO("caffeine", "카페인"), TagDTO("spicy", "매운 음식")),
        medications: List<String> = listOf("제산제"),
    ) = JudgmentContext(
        food = FoodFixture.food(name = "아메리카노"),
        category = "beverage",
        foodTriggers = listOf(TagDTO("caffeine", "카페인")),
        foodAllergens = emptyList(),
        userTriggers = userTriggers,
        userAllergens = emptyList(),
        medications = medications,
        symptomCodes = listOf("heartburn_reflux"),
    )

    @Test
    fun `같은 컨텍스트면 리스트 순서가 달라도 같은 키가 나온다`() {
        val key1 = keyFactory.createKey(
            1L,
            snapshotFactory.create(context(userTriggers = listOf(TagDTO("caffeine", "카페인"), TagDTO("spicy", "매운 음식")))),
        )
        val key2 = keyFactory.createKey(
            1L,
            snapshotFactory.create(context(userTriggers = listOf(TagDTO("spicy", "매운 음식"), TagDTO("caffeine", "카페인")))),
        )

        assertThat(key1).isEqualTo(key2)
    }

    @Test
    fun `입력 스냅샷이 달라지면 키가 달라진다(자연 무효화)`() {
        val base = keyFactory.createKey(1L, snapshotFactory.create(context()))
        val changed = keyFactory.createKey(1L, snapshotFactory.create(context(medications = listOf("제산제", "PPI"))))

        assertThat(base).isNotEqualTo(changed)
    }

    @Test
    fun `같은 스냅샷이라도 foodId가 다르면 키가 다르다`() {
        val snapshot = snapshotFactory.create(context())

        assertThat(keyFactory.createKey(1L, snapshot)).isNotEqualTo(keyFactory.createKey(2L, snapshot))
    }
}
