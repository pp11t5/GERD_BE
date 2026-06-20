package com.gerd.domain.symptom.service

import com.gerd.domain.symptom.dto.SymptomPatternAnalysisInputDTO
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * 증상 상세의 맞춤 패턴 분석용 프롬프트·스키마 빌더
 */
@Component
class SymptomPatternAnalysisPromptBuilder(
    private val objectMapper: ObjectMapper,
) {

    fun buildSystemInstruction(): String = SYSTEM_INSTRUCTION

    fun buildUserContent(input: SymptomPatternAnalysisInputDTO): String =
        objectMapper.writeValueAsString(input)

    fun buildResponseSchema(): Map<String, Any> = RESPONSE_SCHEMA

    companion object {
        private val SYSTEM_INSTRUCTION = """
            당신은 위식도역류질환(GERD) 관리 앱의 증상 패턴 분석 도우미입니다.
            입력 JSON의 currentSymptom, linkedMeal, window, features를 근거로
            이 사용자의 최근 식사-증상 연결 기록에서 보이는 패턴을 짧게 설명하세요.

            [분석 목적]
            - 이 분석은 음식 단건 판정이 아니라 사용자의 누적 식사-증상 기록에서 도출한 맞춤 패턴 설명입니다.
            - 모델 학습을 말하지 마세요. 입력으로 주어진 집계값과 후보 패턴만 근거로 사용하세요.
            - 수치 근거는 features.evidenceText, consecutiveCount, window의 count 값을 그대로 사용하세요.
              없는 수치를 만들어내면 안 됩니다.

            [입력 데이터 설명]
            - user: 사용자 닉네임, 등록 트리거 음식, 알레르기, 복용약
            - currentSymptom: 현재 조회 중인 증상 기록
            - linkedMeal: 이 증상 기록에 필수 연결된 식사와 음식 목록
            - window: 최근 N일 rolling 식사-증상 연결 기록 집계
            - features: 서버가 룰 기반으로 계산한 후보 패턴과 신뢰 가능 여부

            [작성 규칙]
            - label은 다음 중 하나만 사용하세요: 유지 권장, 주의 필요, 기록 부족
            - features.hasReliablePattern이 false이면 단정하지 말고 label은 "기록 부족"으로 두세요.
            - pattern은 한 문장으로 작성하세요.
            - advice는 사용자가 바로 적용할 수 있는 부드러운 권유 한 문장으로 작성하세요.
            - 치료·진단·처방·완치 표현은 금지합니다.
            - 금지: "반드시", "확실히", "원인입니다", "치료됩니다"
            - 말투는 요체/해요체를 사용하세요.

            출력은 지정된 JSON 스키마만 따르세요.
        """.trimIndent()

        private val RESPONSE_SCHEMA: Map<String, Any> = mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "label" to mapOf(
                    "type" to "STRING",
                    "enum" to listOf("유지 권장", "주의 필요", "기록 부족"),
                ),
                "pattern" to mapOf("type" to "STRING"),
                "advice" to mapOf("type" to "STRING"),
            ),
            "required" to listOf("label", "pattern", "advice"),
        )
    }
}
