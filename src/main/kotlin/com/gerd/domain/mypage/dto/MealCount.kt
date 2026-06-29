package com.gerd.domain.mypage.dto

import io.swagger.v3.oas.annotations.media.Schema

data class MealCount(
    @field:Schema(description = "지난주 신호등 권장(RECOMMEND) 끼니 수", example = "10")
    val recommendCount: Int,
    @field:Schema(description = "지난주 신호등 주의(CAUTION) 끼니 수", example = "3")
    val cautionCount: Int,
    @field:Schema(description = "지난주 신호등 위험(RISK) 끼니 수", example = "1")
    val riskCount: Int,
)
