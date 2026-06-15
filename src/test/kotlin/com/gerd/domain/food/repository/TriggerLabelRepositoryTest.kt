package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.TriggerLabel
import com.gerd.global.config.QuerydslTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class)
class TriggerLabelRepositoryTest @Autowired constructor(
    private val triggerLabelRepository: TriggerLabelRepository,
) {

    @Nested
    inner class `findByCodeIn` {

        @Test
        fun `요청한 code에 해당하는 마스터만 조회한다`() {
            triggerLabelRepository.save(TriggerLabel(code = "caffeine", displayName = "커피·카페인"))
            triggerLabelRepository.save(TriggerLabel(code = "spicy", displayName = "매운 음식"))
            triggerLabelRepository.save(TriggerLabel(code = "alcohol", displayName = "술"))

            val result = triggerLabelRepository.findByCodeIn(listOf("caffeine", "spicy"))

            assertThat(result.map { it.code }).containsExactlyInAnyOrder("caffeine", "spicy")
        }

        @Test
        fun `시드되지 않은 code면 빈 리스트를 반환한다`() {
            val result = triggerLabelRepository.findByCodeIn(listOf("not_seeded"))

            assertThat(result).isEmpty()
        }
    }
}
