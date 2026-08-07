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
}
