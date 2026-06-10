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
        LlmJudgmentItemDTO("카페인이 들어 있어요", "{nickname}님이 등록한 커피류에 해당해요."),
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
            assertThat(cached.items).hasSize(2)
            assertThat(cached.items[0].emphasis).isEqualTo("카페인이 들어 있어요")
            assertThat(cached.substitutes).hasSize(1)
        }

        @Test
        fun `LLM이 UNKNOWN이면 items를 버리고 고정 폴백 카피를 쓴다`() {
            val cached = assembler.assembleCacheable(
                context = context,
                llmJudgment = LlmJudgmentDTO(JudgmentGrade.UNKNOWN, items = llmItems),
                override = overrideOf(JudgmentGrade.UNKNOWN),
                substitutes = emptyList(),
            )

            assertThat(cached.items[0].emphasis).isEqualTo("정보가 부족해요")
            assertThat(cached.items[1].emphasis).isEqualTo("알레르기 확인이 어려워요")
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
        fun `닉네임 토큰을 치환하고 등급별 제목을 내려준다`() {
            val cached = assembler.assembleCacheable(
                context = context,
                llmJudgment = LlmJudgmentDTO(JudgmentGrade.CAUTION, items = llmItems),
                override = overrideOf(JudgmentGrade.CAUTION),
                substitutes = emptyList(),
            )

            val response = assembler.toResponse(cached, nickname = "유진", cachedFlag = true)

            assertThat(response.personalTitle).isEqualTo("속이 편안할 수 있도록 천천히 드세요!")
            assertThat(response.items[0].body).isEqualTo("유진님이 등록한 커피류에 해당해요.")
            assertThat(response.cached).isTrue()
            assertThat(response.disclaimer).isEqualTo("본 앱은 진단·치료 서비스가 아닙니다.")
            assertThat(response.stateRecords).isEmpty()
        }

        @Test
        fun `닉네임이 없으면 기본 호칭으로 치환한다`() {
            val cached = assembler.assembleCacheable(
                context = context,
                llmJudgment = LlmJudgmentDTO(JudgmentGrade.RECOMMEND, items = llmItems),
                override = overrideOf(JudgmentGrade.RECOMMEND),
                substitutes = emptyList(),
            )

            val response = assembler.toResponse(cached, nickname = null, cachedFlag = false)

            assertThat(response.personalTitle).isEqualTo("회원님, 좋은 선택이에요!")
            assertThat(response.items[0].body).isEqualTo("회원님이 등록한 커피류에 해당해요.")
        }
    }

    @Nested
    inner class `assembleUnknownFallback` {

        @Test
        fun `UNKNOWN 폴백 응답을 조립한다(대체식단 없음, 캐시 아님)`() {
            val response = assembler.assembleUnknownFallback(context)

            assertThat(response.grade).isEqualTo(JudgmentGrade.UNKNOWN)
            assertThat(response.personalTitle).isEqualTo("이 음식은 정보가 충분하지 않아요")
            assertThat(response.items).hasSize(2)
            assertThat(response.substitutes).isEmpty()
            assertThat(response.cached).isFalse()
        }
    }
}
