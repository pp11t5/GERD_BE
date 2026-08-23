package com.gerd.domain.judgment.service

import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.service.FoodCategoryAssigner
import com.gerd.domain.food.service.FoodCategoryReader
import com.gerd.domain.judgment.dto.JudgmentContext
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.domain.judgment.dto.SubstituteCandidateDTO
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.databind.json.JsonMapper

@ExtendWith(MockitoExtension::class)
class FoodJudgmentQueryServiceTest {

    @Mock private lateinit var judgmentContextReader: JudgmentContextReader
    @Mock private lateinit var judgmentGeminiAdapter: JudgmentGeminiAdapter
    @Mock private lateinit var foodCategoryReader: FoodCategoryReader
    @Mock private lateinit var foodCategoryAssigner: FoodCategoryAssigner

    private lateinit var service: FoodJudgmentQueryService

    private val userId = 1L
    private val foodExternalId = FoodFixture.EXTERNAL_ID.toString()

    private val milk = TagDTO("milk", "우유")
    private val caffeine = TagDTO("caffeine", "카페인")

    private val llmJudgment = LlmJudgmentDTO(
        grade = JudgmentGrade.CAUTION,
        personalTitle = "카페인이 있으니 천천히 즐겨보세요",
        items = listOf(
            LlmJudgmentItemDTO("카페인이 들어 있어요", "등록하신 커피류 트리거에 해당해요."),
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
            judgmentPromptBuilder = JudgmentPromptBuilder(JsonMapper.builder().findAndAddModules().build(), foodCategoryReader),
            judgmentGeminiAdapter = judgmentGeminiAdapter,
            safetyOverrideRule = SafetyOverrideRule(),
            foodCategoryAssigner = foodCategoryAssigner,
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
        symptomCodes = listOf("heartburn_reflux"),
    )

    private fun userFoodContext() = JudgmentContext(
        food = FoodFixture.food(name = "집밥", source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = userId),
        category = null,
        foodTriggers = emptyList(),
        foodAllergens = emptyList(),
        userTriggers = emptyList(),
        userAllergens = emptyList(),
        symptomCodes = emptyList(),
    )

    @Nested
    inner class `⓪ 출처 게이트 - 유저 음식은 텍스트 파이프라인으로 위임` {

        @Test
        fun `유저 입력 음식도 이름 기반 LLM 판정과 캐싱을 거친다`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(userFoodContext())
            whenever(judgmentContextReader.loadUserContext(userId)).thenReturn(
                com.gerd.domain.judgment.dto.UserContext(
                    userTriggers = emptyList(),
                    userAllergens = emptyList(),
                    symptomCodes = emptyList(),
                ),
            )
            whenever(judgmentContextReader.loadHistoryForText(any(), any()))
                .thenReturn(com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.HistorySnapshotDTO())
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(llmJudgment)

            val (first, firstCached) = service.getJudgment(foodExternalId, userId)
            val (second, secondCached) = service.getJudgment(foodExternalId, userId)

            assertThat(first.grade).isEqualTo(JudgmentGrade.CAUTION)
            assertThat(first.foodExternalId).isEqualTo(foodExternalId)
            assertThat(firstCached).isFalse()
            assertThat(secondCached).isTrue()
            verify(judgmentGeminiAdapter, times(1)).generateJudgment(any(), any(), any())
            verify(judgmentContextReader, never()).loadSubstituteCandidates(any())
        }

        @Test
        fun `LLM 실패 시 UNKNOWN 폴백을 반환한다`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(userFoodContext())
            whenever(judgmentContextReader.loadUserContext(userId)).thenReturn(
                com.gerd.domain.judgment.dto.UserContext(
                    userTriggers = emptyList(),
                    userAllergens = emptyList(),
                    symptomCodes = emptyList(),
                ),
            )
            whenever(judgmentContextReader.loadHistoryForText(any(), any()))
                .thenReturn(com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.HistorySnapshotDTO())
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(null)

            val (response, isCached) = service.getJudgment(foodExternalId, userId)

            assertThat(response.grade).isEqualTo(JudgmentGrade.UNKNOWN)
            assertThat(isCached).isFalse()
        }
    }

    @Nested
    inner class `텍스트 판정` {

        @Test
        fun `음식명으로 판정하고 근거 기록을 LLM 입력에 반영한다`() {
            val userContext = com.gerd.domain.judgment.dto.UserContext(
                userTriggers = emptyList(),
                userAllergens = emptyList(),
                symptomCodes = emptyList(),
            )
            whenever(judgmentContextReader.loadUserContext(userId)).thenReturn(userContext)
            whenever(judgmentContextReader.loadHistoryForText(userId, "아메리카노")).thenReturn(
                com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.HistorySnapshotDTO(discomfortCount = 2),
            )
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(llmJudgment)

            val (response, isCached) = service.getJudgmentByText("아메리카노", userId)

            assertThat(response.foodName).isEqualTo("아메리카노")
            assertThat(response.grade).isEqualTo(JudgmentGrade.CAUTION)
            assertThat(isCached).isFalse()

            val userContentCaptor = argumentCaptor<String>()
            verify(judgmentGeminiAdapter).generateJudgment(any(), userContentCaptor.capture(), any())
            assertThat(userContentCaptor.firstValue).contains("\"discomfortCount\":2")
        }

        @Test
        fun `동일 음식명 재조회는 LLM 없이 캐시로 응답한다`() {
            whenever(judgmentContextReader.loadUserContext(userId)).thenReturn(
                com.gerd.domain.judgment.dto.UserContext(
                    userTriggers = emptyList(),
                    userAllergens = emptyList(),
                    symptomCodes = emptyList(),
                ),
            )
            whenever(judgmentContextReader.loadHistoryForText(any(), any()))
                .thenReturn(com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.HistorySnapshotDTO())
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(llmJudgment)

            val (_, firstCached) = service.getJudgmentByText("아메리카노", userId)
            val (_, secondCached) = service.getJudgmentByText("아메리카노", userId)

            assertThat(firstCached).isFalse()
            assertThat(secondCached).isTrue()
            verify(judgmentGeminiAdapter, times(1)).generateJudgment(any(), any(), any())
        }

        @Test
        fun `LLM 실패 시 UNKNOWN 폴백을 반환한다`() {
            whenever(judgmentContextReader.loadUserContext(userId)).thenReturn(
                com.gerd.domain.judgment.dto.UserContext(
                    userTriggers = emptyList(),
                    userAllergens = emptyList(),
                    symptomCodes = emptyList(),
                ),
            )
            whenever(judgmentContextReader.loadHistoryForText(any(), any()))
                .thenReturn(com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.HistorySnapshotDTO())
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(null)

            val (response, isCached) = service.getJudgmentByText("아메리카노", userId)

            assertThat(response.grade).isEqualTo(JudgmentGrade.UNKNOWN)
            assertThat(isCached).isFalse()
        }

        @Test
        fun `두 호출 사이 근거 기록이 바뀌면 캐시 미스로 LLM을 다시 호출한다`() {
            whenever(judgmentContextReader.loadUserContext(userId)).thenReturn(
                com.gerd.domain.judgment.dto.UserContext(
                    userTriggers = emptyList(),
                    userAllergens = emptyList(),
                    symptomCodes = emptyList(),
                ),
            )
            whenever(judgmentContextReader.loadHistoryForText(userId, "아메리카노")).thenReturn(
                com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.HistorySnapshotDTO(discomfortCount = 1),
                com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.HistorySnapshotDTO(discomfortCount = 2),
            )
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(llmJudgment)

            val (_, firstCached) = service.getJudgmentByText("아메리카노", userId)
            val (_, secondCached) = service.getJudgmentByText("아메리카노", userId)

            assertThat(firstCached).isFalse()
            assertThat(secondCached).isFalse()
            verify(judgmentGeminiAdapter, times(2)).generateJudgment(any(), any(), any())
        }
    }

    @Nested
    inner class `캐시` {

        @Test
        fun `같은 입력의 재호출은 LLM 없이 캐시로 응답한다(X-Cache HIT)`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(seedContext())
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(llmJudgment)
            whenever(judgmentContextReader.loadSubstituteCandidates(any())).thenReturn(emptyList())

            val (first, firstCached) = service.getJudgment(foodExternalId, userId)
            val (second, secondCached) = service.getJudgment(foodExternalId, userId)

            assertThat(firstCached).isFalse()
            assertThat(secondCached).isTrue()
            assertThat(second.grade).isEqualTo(JudgmentGrade.CAUTION)
            verify(judgmentGeminiAdapter, times(1)).generateJudgment(any(), any(), any())
        }

        @Test
        fun `LLM 실패는 UNKNOWN 폴백을 반환하고 캐시에 남기지 않는다`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(seedContext())
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(null)

            val (first, _) = service.getJudgment(foodExternalId, userId)
            val (second, secondCached) = service.getJudgment(foodExternalId, userId)

            assertThat(first.grade).isEqualTo(JudgmentGrade.UNKNOWN)
            assertThat(secondCached).isFalse()
            // 실패가 캐시되지 않았으므로 재호출 시 LLM을 다시 시도한다
            verify(judgmentGeminiAdapter, times(2)).generateJudgment(any(), any(), any())
        }
    }

    @Nested
    inner class `카테고리 자동분류` {

        @Test
        fun `카테고리가 없는 음식이면 LLM 분류 결과를 등록한다`() {
            val context = seedContext().copy(category = null)
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(context)
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any()))
                .thenReturn(llmJudgment.copy(categoryCode = "soup_stew"))
            whenever(judgmentContextReader.loadSubstituteCandidates(any())).thenReturn(emptyList())

            service.getJudgment(foodExternalId, userId)

            verify(foodCategoryAssigner).assignIfPresent(context.food, "soup_stew")
        }

        @Test
        fun `이미 카테고리가 있으면 분류 결과를 등록하지 않는다`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(seedContext())
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any()))
                .thenReturn(llmJudgment.copy(categoryCode = "soup_stew"))
            whenever(judgmentContextReader.loadSubstituteCandidates(any())).thenReturn(emptyList())

            service.getJudgment(foodExternalId, userId)

            verify(foodCategoryAssigner, never()).assignIfPresent(any(), any())
        }

        @Test
        fun `유저 음식 재판정도 LLM 분류 결과를 등록한다`() {
            val context = userFoodContext()
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(context)
            whenever(judgmentContextReader.loadUserContext(userId)).thenReturn(
                com.gerd.domain.judgment.dto.UserContext(
                    userTriggers = emptyList(),
                    userAllergens = emptyList(),
                    symptomCodes = emptyList(),
                ),
            )
            whenever(judgmentContextReader.loadHistoryForText(any(), any()))
                .thenReturn(com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.HistorySnapshotDTO())
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any()))
                .thenReturn(llmJudgment.copy(categoryCode = "soup_stew"))

            service.getJudgment(foodExternalId, userId)

            verify(foodCategoryAssigner).assignIfPresent(context.food, "soup_stew")
        }
    }

    @Nested
    inner class `판정 파이프라인` {

        @Test
        fun `알레르겐 매치 시 RISK로 강등하고 대체식단을 조회한다`() {
            whenever(judgmentContextReader.load(foodExternalId, userId))
                .thenReturn(seedContext(foodAllergens = listOf(milk), userAllergens = listOf(milk)))
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any()))
                .thenReturn(llmJudgment.copy(grade = JudgmentGrade.RECOMMEND))
            whenever(judgmentContextReader.loadSubstituteCandidates(any()))
                .thenReturn(listOf(SubstituteCandidateDTO(foodExternalId, "디카페인 아메리카노", emptySet())))

            val (response, _) = service.getJudgment(foodExternalId, userId)

            assertThat(response.grade).isEqualTo(JudgmentGrade.RISK)
            assertThat(response.items[1].emphasis).isEqualTo("알레르기 성분이 들어 있어요")
            assertThat(response.substitutes).hasSize(1)
        }

        @Test
        fun `사용자 트리거·알레르겐을 가진 대체식 후보는 노출 전에 제외한다`() {
            // 사용자: caffeine 트리거 + milk 알레르기. 음식: 우유 알레르겐 매치로 RISK
            whenever(judgmentContextReader.load(foodExternalId, userId))
                .thenReturn(seedContext(foodAllergens = listOf(milk), userAllergens = listOf(milk)))
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(llmJudgment)
            whenever(judgmentContextReader.loadSubstituteCandidates(any())).thenReturn(
                listOf(
                    SubstituteCandidateDTO(foodExternalId, "달걀찜", setOf("egg")),
                    SubstituteCandidateDTO(foodExternalId, "치즈우유푸딩", setOf("milk")),
                    SubstituteCandidateDTO(foodExternalId, "콜드브루", setOf("caffeine")),
                    SubstituteCandidateDTO(foodExternalId, "율무차", emptySet()),
                ),
            )

            val (response, _) = service.getJudgment(foodExternalId, userId)

            // milk(알레르기)·caffeine(트리거) 보유 후보는 제외, 사용자와 무관한 egg와 클린 후보만 남는다
            assertThat(response.substitutes.map { it.name }).containsExactly("달걀찜", "율무차")
        }

        @Test
        fun `RECOMMEND이고 매치가 없으면 대체식단을 조회하지 않는다`() {
            // 트리거 강등(RECOMMEND→CAUTION)이 일어나지 않게 사용자 트리거 없는 컨텍스트 구성
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(
                seedContext().copy(userTriggers = emptyList()),
            )
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any()))
                .thenReturn(llmJudgment.copy(grade = JudgmentGrade.RECOMMEND))

            val (response, _) = service.getJudgment(foodExternalId, userId)

            assertThat(response.grade).isEqualTo(JudgmentGrade.RECOMMEND)
            assertThat(response.substitutes).isEmpty()
            verify(judgmentContextReader, never()).loadSubstituteCandidates(any())
        }

        @Test
        fun `LLM items 본문과 제목을 가공 없이 그대로 응답에 담는다`() {
            whenever(judgmentContextReader.load(foodExternalId, userId)).thenReturn(seedContext())
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(llmJudgment)
            whenever(judgmentContextReader.loadSubstituteCandidates(any())).thenReturn(emptyList())

            val (response, _) = service.getJudgment(foodExternalId, userId)

            assertThat(response.items[0].body).isEqualTo("등록하신 커피류 트리거에 해당해요.")
            // 등급이 유지됐으므로(CAUTION 그대로) LLM 제목이 그대로 노출된다
            assertThat(response.personalTitle).isEqualTo("카페인이 있으니 천천히 즐겨보세요")
        }
    }
}
