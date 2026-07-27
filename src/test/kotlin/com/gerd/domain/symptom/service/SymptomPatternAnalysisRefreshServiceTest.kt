package com.gerd.domain.symptom.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.food.dto.FoodCategoryDTO
import com.gerd.domain.food.service.FoodCategoryReader
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.symptom.dto.SymptomPatternAnalysisDTO
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomMealPatternRow
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.fixture.SymptomFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SymptomPatternAnalysisRefreshServiceTest {

    @Mock
    private lateinit var symptomRepository: SymptomRepository

    @Mock
    private lateinit var foodCategoryReader: FoodCategoryReader

    @Mock
    private lateinit var userRepository: UserRepository

    private val objectMapper = ObjectMapper()
    private val symptomPatternStatsCalculator = SymptomPatternStatsCalculator()

    private lateinit var service: SymptomPatternAnalysisRefreshService

    private val userId = 1L

    @BeforeEach
    fun setUp() {
        service = SymptomPatternAnalysisRefreshService(
            symptomRepository = symptomRepository,
            symptomPatternStatsCalculator = symptomPatternStatsCalculator,
            foodCategoryReader = foodCategoryReader,
            userRepository = userRepository,
            objectMapper = objectMapper,
        )
    }

    @Test
    fun `dirty가 아니면 재계산하지 않는다`() {
        val symptom = SymptomFixture.symptom(isAnalysisDirty = false)

        service.refresh(symptom, userId)

        verify(symptomRepository, never()).findLinkedRows(any(), any())
    }

    @Test
    fun `기록이 충분하고 불편이 우세하면 CAUTION 분석을 저장하고 카테고리 표시명을 채운다`() {
        val symptom = SymptomFixture.symptom(isAnalysisDirty = true, analysisVersion = 0L)
        whenever(symptomRepository.findLinkedRows(any(), any())).thenReturn(
            listOf(
                row(1L, "noodle", SymptomState.UNCOMFORTABLE),
                row(2L, "noodle", SymptomState.UNCOMFORTABLE),
                row(3L, "noodle", SymptomState.COMFORTABLE),
            ),
        )
        whenever(foodCategoryReader.getAll()).thenReturn(listOf(FoodCategoryDTO(code = "noodle", displayName = "면류")))

        service.refresh(symptom, userId)

        val dto = objectMapper.readValue(symptom.analysisJson, SymptomPatternAnalysisDTO::class.java)
        assertThat(dto.label).isEqualTo("주의 필요")
        assertThat(dto.pattern).contains("면류").contains("2번")
        assertThat(symptom.isAnalysisDirty).isFalse()
        verify(userRepository, never()).findById(any())
    }

    @Test
    fun `기록이 부족하면 OBSERVING 분석을 저장하고 닉네임을 채운다`() {
        val symptom = SymptomFixture.symptom(isAnalysisDirty = true, analysisVersion = 0L)
        whenever(symptomRepository.findLinkedRows(any(), any())).thenReturn(
            listOf(row(1L, "noodle", SymptomState.COMFORTABLE)),
        )
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(SymptomFixture.user(nickname = "유진")))

        service.refresh(symptom, userId)

        val dto = objectMapper.readValue(symptom.analysisJson, SymptomPatternAnalysisDTO::class.java)
        assertThat(dto.label).isEqualTo("관찰 중")
        assertThat(dto.advice).contains("유진")
    }

    private fun row(id: Long, category: String?, state: SymptomState) = SymptomMealPatternRow(
        symptomInternalId = id,
        symptomState = state,
        occurredAt = LocalDateTime.now().minusDays(id),
        mealRecordId = id,
        foodName = "음식$id",
        category = category,
        judgmentGrade = JudgmentGrade.CAUTION,
    )
}
