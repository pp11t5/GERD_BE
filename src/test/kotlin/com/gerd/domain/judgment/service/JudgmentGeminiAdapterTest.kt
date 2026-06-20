package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.global.ai.gemini.GeminiClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import tools.jackson.databind.json.JsonMapper

@ExtendWith(MockitoExtension::class)
class JudgmentGeminiAdapterTest {

    @Mock
    private lateinit var geminiClient: GeminiClient

    private lateinit var adapter: JudgmentGeminiAdapter

    @BeforeEach
    fun setUp() {
        adapter = JudgmentGeminiAdapter(
            geminiClient = geminiClient,
            objectMapper = JsonMapper.builder().findAndAddModules().build(),
        )
    }

    private fun call() = adapter.generateJudgment("system", "user", mapOf("type" to "OBJECT"))

    @Nested
    inner class 성공 {

        @Test
        fun `structured output을 LlmJudgmentDTO로 파싱한다`() {
            whenever(geminiClient.generateJson(any())).thenReturn(
                """
                {"grade":"CAUTION","personalTitle":"오늘은 천천히 즐겨보세요","items":[
                  {"emphasis":"카페인이 들어 있어요","body":"천천히 드세요."},
                  {"emphasis":"알레르기 해당 없어요","body":"포함되지 않았어요."}
                ]}
                """.trimIndent(),
            )

            val judgment = call()

            assertThat(judgment).isNotNull
            assertThat(judgment?.grade).isEqualTo(JudgmentGrade.CAUTION)
            assertThat(judgment?.personalTitle).isEqualTo("오늘은 천천히 즐겨보세요")
            assertThat(judgment?.items).hasSize(2)
        }

        @Test
        fun `UNKNOWN은 items 슬롯 수와 무관하게 유효하다`() {
            whenever(geminiClient.generateJson(any()))
                .thenReturn("""{"grade":"UNKNOWN","items":[]}""")

            assertThat(call()?.grade).isEqualTo(JudgmentGrade.UNKNOWN)
        }
    }

    @Nested
    inner class 실패 {

        @Test
        fun `Gemini 호출 실패는 null을 반환한다`() {
            whenever(geminiClient.generateJson(any())).thenReturn(null)

            assertThat(call()).isNull()
        }

        @Test
        fun `응답 텍스트가 JSON이 아니면 null을 반환한다`() {
            whenever(geminiClient.generateJson(any())).thenReturn("죄송하지만 판단할 수 없습니다")

            assertThat(call()).isNull()
        }

        @Test
        fun `UNKNOWN이 아닌데 items가 2개가 아니면 null을 반환한다`() {
            whenever(geminiClient.generateJson(any()))
                .thenReturn("""{"grade":"CAUTION","items":[{"emphasis":"하나","body":"뿐"}]}""")

            assertThat(call()).isNull()
        }
    }
}
