package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.JudgmentContext
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.domain.judgment.dto.LlmJudgmentDTO
import com.gerd.domain.judgment.dto.LlmJudgmentDTO.LlmJudgmentItemDTO
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.judgment.service.SafetyOverrideRule.OverrideResult
import com.gerd.global.fixture.FoodFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JudgmentResponseAssemblerTest {

    private val assembler = JudgmentResponseAssembler()

    private val context = JudgmentContext(
        food = FoodFixture.food(name = "아메리카노"),
        category = "beverage",
        foodTriggers = listOf(TagDTO("caffeine", "카페인")),
        foodAllergens = emptyList(),
        userTriggers = listOf(TagDTO("caffeine", "카페인")),
        userAllergens = emptyList(),
        medications = emptyList(),
        symptomCodes = emptyList(),
    )

    private val llmItems = listOf(
        LlmJudgmentItemDTO("카페인이 들어 있어요", "등록하신 커피류 트리거에 해당해요."),
        LlmJudgmentItemDTO("알레르기 해당 없어요", "알레르기 성분이 포함되지 않았어요."),
    )

    private fun overrideOf(
        grade: JudgmentGrade,
        allergenMatches: List<TagDTO> = emptyList(),
        triggerMatches: List<TagDTO> = emptyList(),
    ) = OverrideResult(grade, allergenMatches, triggerMatches)

    @Nested
    inner class `assembleCacheable` {

        @Test
        fun `LLM items와 최종 등급으로 캐시 value를 조립한다`() {
            val cached = assembler.assembleCacheable(
                context = context,
                llmJudgment = LlmJudgmentDTO(JudgmentGrade.CAUTION, items = llmItems),
                override = overrideOf(JudgmentGrade.CAUTION),
                substitutes = listOf(SubstituteDTO(FoodFixture.EXTERNAL_ID.toString(), "디카페인 아메리카노")),
            )

            assertThat(cached.grade).isEqualTo(JudgmentGrade.CAUTION)
            assertThat(cached.foodName).isEqualTo("아메리카노")
            assertThat(cached.category).isEqualTo("beverage")
            assertThat(cached.items).hasSize(2)
            assertThat(cached.items[0].emphasis).isEqualTo("카페인이 들어 있어요")
            assertThat(cached.substitutes).hasSize(1)
        }

        @Test
        fun `등급이 유지되면 LLM 제목을 그대로 쓴다`() {
            val cached = assembler.assembleCacheable(
                context = context,
                llmJudgment = LlmJudgmentDTO(JudgmentGrade.CAUTION, personalTitle = "카페인이 있으니 오늘은 천천히 즐겨보세요", items = llmItems),
                override = overrideOf(JudgmentGrade.CAUTION),
                substitutes = emptyList(),
            )

            assertThat(cached.personalTitle).isEqualTo("카페인이 있으니 오늘은 천천히 즐겨보세요")
        }

        @Test
        fun `오버라이드로 등급이 강등되면 LLM 제목 대신 고정 제목을 쓴다(톤 불일치 방지)`() {
            val cached = assembler.assembleCacheable(
                context = context,
                llmJudgment = LlmJudgmentDTO(JudgmentGrade.RECOMMEND, personalTitle = "좋은 선택이에요!", items = llmItems),
                override = overrideOf(JudgmentGrade.RISK, allergenMatches = listOf(TagDTO("milk", "우유"))),
                substitutes = emptyList(),
            )

            assertThat(cached.personalTitle).isEqualTo("오늘은 다른 메뉴가 더 편할 거예요")
        }

        @Test
        fun `LLM 제목이 누락되거나 공백이면 등급별 고정 제목으로 폴백한다`() {
            val cached = assembler.assembleCacheable(
                context = context,
                llmJudgment = LlmJudgmentDTO(JudgmentGrade.CAUTION, personalTitle = " ", items = llmItems),
                override = overrideOf(JudgmentGrade.CAUTION),
                substitutes = emptyList(),
            )

            assertThat(cached.personalTitle).isEqualTo("속이 편안할 수 있도록 천천히 드세요!")
        }

        @Test
        fun `알레르겐 매치 시 items 슬롯 1을 결정적 카피로 교체한다`() {
            val milk = TagDTO("milk", "우유")
            val cached = assembler.assembleCacheable(
                context = context,
                llmJudgment = LlmJudgmentDTO(JudgmentGrade.RECOMMEND, items = llmItems),
                override = overrideOf(JudgmentGrade.RISK, allergenMatches = listOf(milk)),
                substitutes = emptyList(),
            )

            assertThat(cached.grade).isEqualTo(JudgmentGrade.RISK)
            assertThat(cached.items[0].emphasis).isEqualTo("카페인이 들어 있어요")
            assertThat(cached.items[1].emphasis).isEqualTo("알레르기 성분이 들어 있어요")
            assertThat(cached.items[1].body).contains("우유")
        }
    }

    @Nested
    inner class `toResponse` {

        @Test
        fun `등급별 제목과 stateRecords 더미를 내려준다`() {
            val cached = assembler.assembleCacheable(
                context = context,
                llmJudgment = LlmJudgmentDTO(JudgmentGrade.CAUTION, items = llmItems),
                override = overrideOf(JudgmentGrade.CAUTION),
                substitutes = emptyList(),
            )

            val response = assembler.toResponse(cached)

            assertThat(response.personalTitle).isEqualTo("속이 편안할 수 있도록 천천히 드세요!")
            assertThat(response.category).isEqualTo("beverage")
            assertThat(response.items[0].body).isEqualTo("등록하신 커피류 트리거에 해당해요.")
            assertThat(response.stateRecords.total).isEqualTo(0)
            assertThat(response.stateRecords.records).isEmpty()
        }

        @Test
        fun `RECOMMEND는 등급 제목을 내려준다`() {
            val cached = assembler.assembleCacheable(
                context = context,
                llmJudgment = LlmJudgmentDTO(JudgmentGrade.RECOMMEND, items = llmItems),
                override = overrideOf(JudgmentGrade.RECOMMEND),
                substitutes = emptyList(),
            )

            val response = assembler.toResponse(cached)

            assertThat(response.personalTitle).isEqualTo("좋은 선택이에요!")
        }
    }

    @Nested
    inner class `assembleFallback` {

        @Test
        fun `분석 근거 없는 폴백 응답을 조립한다(CAUTION, 대체식단 없음)`() {
            val response = assembler.assembleFallback(context)

            assertThat(response.grade).isEqualTo(JudgmentGrade.CAUTION)
            assertThat(response.category).isEqualTo("beverage")
            assertThat(response.personalTitle).isEqualTo("이 음식은 정보가 충분하지 않아요")
            assertThat(response.items).hasSize(2)
            assertThat(response.substitutes).isEmpty()
            assertThat(response.stateRecords.total).isEqualTo(0)
        }
    }
}
