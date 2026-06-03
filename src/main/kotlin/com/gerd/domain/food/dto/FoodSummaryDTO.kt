package com.gerd.domain.food.dto

import io.swagger.v3.oas.annotations.media.Schema

// 검색 결과 음식 1건 — 내부 id 대신 externalId만 노출(D4)
data class FoodSummaryDTO(
    @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val externalId: String,

    @field:Schema(description = "음식 이름", example = "된장찌개")
    val name: String,

    // 데이터상 다중 분류가 가능하지만 화면 노출은 대표 분류 1개 — code만 내리고 표시명은 클라이언트가 매핑(D6). 분류 없으면 null
    @field:Schema(description = "대표 음식 분류 code", example = "soup_stew", nullable = true)
    val category: String?,
)
