package com.gerd.domain.judgment.service

import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.judgment.client.GeminiClient
import com.gerd.domain.judgment.dto.JudgmentContext
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.domain.judgment.dto.LlmJudgmentDTO
import com.gerd.domain.judgment.dto.LlmJudgmentDTO.LlmJudgmentItemDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.global.fixture.FoodFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.databind.json.JsonMapper

@ExtendWith(MockitoExtension::class)
class FoodJudgmentQueryServiceTest {

    @Mock private lateinit var judgmentContextReader: JudgmentContextReader
    @Mock private lateinit var geminiClient: GeminiClient

    private lateinit var service: FoodJudgmentQueryService

    private val userId = 1L
    private val foodExternalId = FoodFixture.EXTERNAL_ID.toString()

    private val milk = TagDTO("milk", "우유")
    private val caffeine = TagDTO("caffeine", "카페인")

    private val llmJudgment = LlmJudgmentDTO(
        grade = JudgmentGrade.CAUTION,
        reasons = listOf("카페인"),
        items = listOf(
            LlmJudgmentItemDTO("카페인이 들어 있어요", "{nickname}님이 등록한 커피류에 해당해요."),
            LlmJudgmentItemDTO("알레르기 해당 없어요", "알레르기 성분이 포함되지 않았어요."),
        ),
    )

    @BeforeEach
    fun setUp() {
        // 순수 컴포넌트는 실물, 외부 경계(DB Reader·LLM)만 mock — 캐시는 테스트마다 새로 만든다
        service = FoodJudgmentQueryService(
            judgmentContextReader = judgmentContextReader,
            judgmentSnapshotFactory = JudgmentSnapshotFactory(),
            judgmentCacheKeyFactory = JudgmentCacheKeyFactory(),
            judgmentCache = JudgmentCache(),
            judgmentPromptBuilder = JudgmentPromptBuilder(JsonMapper.builder().findAndAddModules().build()),
            geminiClient = geminiClient,
            safetyOverrideRule = SafetyOverrideRule(),
            judgmentResponseAssembler = JudgmentResponseAssembler(),
        )
    }

    private fun seedContext(
        foodAllergens: List<TagDTO> = emptyList(),
        userAllergens: List<TagDTO> = emptyList(),
    ) = JudgmentContext(
        food = FoodFixture.food(name = "아메리카노"),
        category = "beverage",
        foodTriggers = listOf(caffeine),
        foodAllergens = foodAllergens,
        userTriggers = listOf(caffeine),
        userAllergens = userAllergens,
        medications = emptyList(),
        symptomCodes = listOf("heartburn_reflux"),
    )

    private fun userFoodContext() = JudgmentContext(
        food = FoodFixture.food(name = "집밥", source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = userId),
        category = null,
        foodTriggers = emptyList(),
        foodAllergens = emptyList(),
        userTriggers = emptyList(),
        userAllergens = emptyList(),
        medications = emptyList(),
        symptomCodes = emptyList(),
    )

    @Nested
    inner class `⓪ 출처 게이트` {

        @Test
        fun `유저 입력 음식은 LLM 호출 없이 UNKNOWN 폴백을 반환한다`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(userFoodContext())

            val response = service.getJudgment(foodExternalId, userId, "유진")

            assertThat(response.grade).isEqualTo(JudgmentGrade.UNKNOWN)
            assertThat(response.cached).isFalse()
            verify(geminiClient, never()).generateJudgment(any(), any(), any())
        }
    }

    @Nested
    inner class `캐시` {

        @Test
        fun `같은 입력의 재호출은 LLM 없이 캐시로 응답한다(cached=true)`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(seedContext())
            whenever(geminiClient.generateJudgment(any(), any(), any())).thenReturn(llmJudgment)
            whenever(judgmentContextReader.loadSubstitutes(any())).thenReturn(emptyList())

            val first = service.getJudgment(foodExternalId, userId, "유진")
            val second = service.getJudgment(foodExternalId, userId, "유진")

            assertThat(first.cached).isFalse()
            assertThat(second.cached).isTrue()
            assertThat(second.grade).isEqualTo(JudgmentGrade.CAUTION)
            verify(geminiClient, times(1)).generateJudgment(any(), any(), any())
        }

        @Test
        fun `LLM 실패는 UNKNOWN 폴백을 반환하고 캐시에 남기지 않는다`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(seedContext())
            whenever(geminiClient.generateJudgment(any(), any(), any())).thenReturn(null)

            val first = service.getJudgment(foodExternalId, userId, "유진")
            val second = service.getJudgment(foodExternalId, userId, "유진")

            assertThat(first.grade).isEqualTo(JudgmentGrade.UNKNOWN)
            assertThat(second.cached).isFalse()
            // 실패가 캐시되지 않았으므로 재호출 시 LLM을 다시 시도한다
            verify(geminiClient, times(2)).generateJudgment(any(), any(), any())
        }
    }

    @Nested
    inner class `판정 파이프라인` {

        @Test
        fun `LLM이 UNKNOWN이면 items를 고정 폴백으로 교체하고 대체식단을 조회하지 않는다`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(seedContext())
            whenever(geminiClient.generateJudgment(any(), any(), any()))
                .thenReturn(LlmJudgmentDTO(JudgmentGrade.UNKNOWN))

            val response = service.getJudgment(foodExternalId, userId, "유진")

            assertThat(response.grade).isEqualTo(JudgmentGrade.UNKNOWN)
            assertThat(response.items[0].emphasis).isEqualTo("정보가 부족해요")
            assertThat(response.substitutes).isEmpty()
            verify(judgmentContextReader, never()).loadSubstitutes(any())
        }

        @Test
        fun `알레르겐 매치 시 RISK로 강등하고 대체식단을 조회한다`() {
            whenever(judgmentContextReader.load(foodExternalId, userId))
                .thenReturn(seedContext(foodAllergens = listOf(milk), userAllergens = listOf(milk)))
            whenever(geminiClient.generateJudgment(any(), any(), any()))
                .thenReturn(llmJudgment.copy(grade = JudgmentGrade.RECOMMEND))
            whenever(judgmentContextReader.loadSubstitutes(any()))
                .thenReturn(listOf(SubstituteDTO(foodExternalId, "디카페인 아메리카노")))

            val response = service.getJudgment(foodExternalId, userId, "유진")

            assertThat(response.grade).isEqualTo(JudgmentGrade.RISK)
            assertThat(response.items[1].emphasis).isEqualTo("알레르기 성분이 들어 있어요")
            assertThat(response.substitutes).hasSize(1)
        }

        @Test
        fun `RECOMMEND이고 매치가 없으면 대체식단을 조회하지 않는다`() {
            // 트리거 강등(RECOMMEND→CAUTION)이 일어나지 않게 사용자 트리거 없는 컨텍스트 구성
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(
                seedContext().copy(userTriggers = emptyList()),
            )
            whenever(geminiClient.generateJudgment(any(), any(), any()))
                .thenReturn(llmJudgment.copy(grade = JudgmentGrade.RECOMMEND))

            val response = service.getJudgment(foodExternalId, userId, "유진")

            assertThat(response.grade).isEqualTo(JudgmentGrade.RECOMMEND)
            assertThat(response.substitutes).isEmpty()
            verify(judgmentContextReader, never()).loadSubstitutes(any())
        }

        @Test
        fun `응답 직전 닉네임 토큰을 치환한다(캐시 HIT 경로 포함)`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(seedContext())
            whenever(geminiClient.generateJudgment(any(), any(), any())).thenReturn(llmJudgment)
            whenever(judgmentContextReader.loadSubstitutes(any())).thenReturn(emptyList())

            val first = service.getJudgment(foodExternalId, userId, "유진")
            val second = service.getJudgment(foodExternalId, userId, null)

            assertThat(first.items[0].body).isEqualTo("유진님이 등록한 커피류에 해당해요.")
            // 같은 캐시 엔트리라도 닉네임은 응답 시점 값으로 치환된다
            assertThat(second.items[0].body).isEqualTo("회원님이 등록한 커피류에 해당해요.")
        }
    }
}
