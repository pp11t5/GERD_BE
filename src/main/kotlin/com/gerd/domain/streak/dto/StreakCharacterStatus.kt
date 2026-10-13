package com.gerd.domain.streak.dto

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = """홈 캐릭터 상태
- greeting: 증상 기록·식사 기록이 한 번도 없는 첫 방문
- terrible: 스트릭 0이면서 가장 최근 증상 기록이 불편 또는 심함
- satisfied: 기본 상태 (편안 스트릭 0~6일)
- grateful: 편안 스트릭 7일 이상""",
    allowableValues = ["greeting", "terrible", "satisfied", "grateful"],
)
enum class StreakCharacterStatus(
    @get:JsonValue val code: String,
) {
    GREETING("greeting"),
    TERRIBLE("terrible"),
    SATISFIED("satisfied"),
    GRATEFUL("grateful"),
}
