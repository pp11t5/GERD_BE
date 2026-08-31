package com.gerd.domain.judgment.service

import com.gerd.domain.food.dto.FoodCategoryDTO
import com.gerd.domain.food.service.FoodCategoryReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import tools.jackson.databind.json.JsonMapper

@ExtendWith(MockitoExtension::class)
class JudgmentPromptBuilderTest {

    @Mock private lateinit var foodCategoryReader: FoodCategoryReader

    private val builder by lazy {
        JudgmentPromptBuilder(JsonMapper.builder().findAndAddModules().build(), foodCategoryReader)
    }

    @Test
    fun `카테고리 목록을 시스템 인스트럭션에 포함한다`() {
        whenever(foodCategoryReader.getAll()).thenReturn(
            listOf(FoodCategoryDTO(code = "soup_stew", displayName = "국·찌개")),
        )

        val instruction = builder.buildSystemInstruction()

        assertThat(instruction).contains("soup_stew: 국·찌개")
    }

    @Test
    fun `응답 스키마에 categoryCode를 nullable enum으로 추가한다`() {
        whenever(foodCategoryReader.getAll()).thenReturn(
            listOf(FoodCategoryDTO(code = "soup_stew", displayName = "국·찌개")),
        )

        val schema = builder.buildResponseSchema()

        @Suppress("UNCHECKED_CAST")
        val properties = schema["properties"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val categoryCodeSchema = properties["categoryCode"] as Map<String, Any>
        assertThat(categoryCodeSchema["nullable"]).isEqualTo(true)
        assertThat(categoryCodeSchema["enum"]).isEqualTo(listOf("soup_stew"))
        assertThat(schema["required"]).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).contains("categoryCode")
    }

    @Test
    fun `카테고리 목록은 프로세스 생애주기 동안 한 번만 조회한다`() {
        whenever(foodCategoryReader.getAll()).thenReturn(emptyList())

        builder.buildSystemInstruction()
        builder.buildResponseSchema()

        org.mockito.kotlin.verify(foodCategoryReader, org.mockito.kotlin.times(1)).getAll()
    }

    @Test
    fun `EVIDENCE RULES에 생리 기전 서술·단정적 인과 표현 금지 규칙을 포함한다`() {
        whenever(foodCategoryReader.getAll()).thenReturn(emptyList())

        val instruction = builder.buildSystemInstruction()

        assertThat(instruction).contains("[EVIDENCE RULES]")
        assertThat(instruction).contains("lower esophageal sphincter pressure")
        assertThat(instruction).contains("위험해요")
        assertThat(instruction).contains("악화돼요")
        assertThat(instruction).contains("유발해요")
    }

    @Test
    fun `EVIDENCE RULES에 허용되는 근거 범주(트리거 라벨·기록·실행 조언)를 포함한다`() {
        whenever(foodCategoryReader.getAll()).thenReturn(emptyList())

        val instruction = builder.buildSystemInstruction()

        assertThat(instruction).contains("predefined trigger labels")
        assertThat(instruction).contains("history/records (history/similarFoodRecords)")
        assertThat(instruction).contains("amount, pace, or")
    }

    @Test
    fun `등록 트리거에 개인 기록이 없으면 구체적인 식사 조절과 기록을 안내한다`() {
        whenever(foodCategoryReader.getAll()).thenReturn(emptyList())

        val instruction = builder.buildSystemInstruction()

        assertThat(instruction).contains("discomfortCount > 0")
        assertThat(instruction).contains("registered trigger")
        assertThat(instruction).contains("amount, pace, or timing")
        assertThat(instruction).contains("recording the response")
        assertThat(instruction).contains("Never use vague standalone caveats")
    }
}
