package com.gerd.domain.symptom.service

import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.fixture.SymptomFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SymptomAnalysisResultWriterTest {

    @Mock
    private lateinit var symptomRepository: SymptomRepository

    private val writer by lazy {
        SymptomAnalysisResultWriter(symptomRepository)
    }

    private val userId = 1L

    @Nested
    inner class `분석 결과 저장` {

        @Test
        fun `작업 시작 버전과 현재 버전이 같으면 분석 결과를 저장하고 dirty를 해제한다`() {
            val symptom = SymptomFixture.symptom(isAnalysisDirty = true, analysisVersion = 2L)
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId))
                .thenReturn(symptom)

            val updated = writer.updateIfVersionMatches(
                symptomId = SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(),
                userId = userId,
                expectedVersion = 2L,
                analysisJson = """{"label":"유지 권장"}""",
            )

            assertThat(updated).isTrue()
            assertThat(symptom.analysisJson).contains("유지 권장")
            assertThat(symptom.isAnalysisDirty).isFalse()
        }

        @Test
        fun `작업 시작 이후 증상이 바뀌어 버전이 다르면 오래된 분석 결과를 버린다`() {
            val symptom = SymptomFixture.symptom(isAnalysisDirty = true, analysisVersion = 3L)
            whenever(symptomRepository.findByExternalIdAndUser_Id(SymptomFixture.SYMPTOM_EXTERNAL_ID, userId))
                .thenReturn(symptom)

            val updated = writer.updateIfVersionMatches(
                symptomId = SymptomFixture.SYMPTOM_EXTERNAL_ID.toString(),
                userId = userId,
                expectedVersion = 2L,
                analysisJson = """{"label":"오래된 결과"}""",
            )

            assertThat(updated).isFalse()
            assertThat(symptom.analysisJson).isNull()
            assertThat(symptom.isAnalysisDirty).isTrue()
            assertThat(symptom.analysisVersion).isEqualTo(3L)
        }
    }
}
