package com.gerd.domain.symptom.service

import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.fixture.SymptomFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SymptomPatternRefreshServiceTest {

    @Mock
    private lateinit var symptomRepository: SymptomRepository

    @Mock
    private lateinit var symptomPatternAnalysisService: SymptomPatternAnalysisService

    @Mock
    private lateinit var symptomAnalysisResultWriter: SymptomAnalysisResultWriter

    private val service by lazy {
        SymptomPatternRefreshService(symptomRepository, symptomPatternAnalysisService, symptomAnalysisResultWriter)
    }

    private val userId = 1L

    @Nested
    inner class `비동기 분석 갱신` {

        @Test
        fun `dirty 상태면 분석 결과를 저장하고 dirty를 해제한다`() {
            val symptom = SymptomFixture.symptom(isAnalysisDirty = true)
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId)).thenReturn(symptom)
            whenever(symptomPatternAnalysisService.generate(symptom, userId)).thenReturn(
                SymptomPatternAnalysisResult(
                    analysisJson = """{"label":"유지 권장","pattern":"편안한 패턴이에요","advice":"이어가 보세요."}""",
                    shouldUpdate = true,
                ),
            )

            service.refreshAsync(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), userId)

            verify(symptomAnalysisResultWriter).updateIfVersionMatches(
                symptomId = SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(),
                userId = userId,
                expectedVersion = 0L,
                analysisJson = """{"label":"유지 권장","pattern":"편안한 패턴이에요","advice":"이어가 보세요."}""",
            )
        }

        @Test
        fun `Gemini 실패 등으로 저장할 결과가 없으면 dirty를 유지한다`() {
            val symptom = SymptomFixture.symptom(isAnalysisDirty = true)
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId)).thenReturn(symptom)
            whenever(symptomPatternAnalysisService.generate(symptom, userId)).thenReturn(
                SymptomPatternAnalysisResult(
                    analysisJson = """{"label":"기록 부족","pattern":"지금은 분석을 만들기 어려워요.","advice":"잠시 후 다시 확인해 주세요."}""",
                    shouldUpdate = false,
                ),
            )

            service.refreshAsync(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), userId)

            assertThat(symptom.analysisJson).isNull()
            assertThat(symptom.isAnalysisDirty).isTrue()
            verify(symptomAnalysisResultWriter, never()).updateIfVersionMatches(any(), any(), any(), any())
        }

        @Test
        fun `이미 최신 상태면 분석을 호출하지 않는다`() {
            val symptom = SymptomFixture.symptom(isAnalysisDirty = false)
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId)).thenReturn(symptom)

            service.refreshAsync(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(), userId)

            verify(symptomPatternAnalysisService, never()).generate(any(), any())
            verify(symptomAnalysisResultWriter, never()).updateIfVersionMatches(any(), any(), any(), any())
        }
    }
}
