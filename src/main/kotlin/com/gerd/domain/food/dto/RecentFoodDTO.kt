package com.gerd.domain.food.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// 최근 검색어 1건
data class RecentFoodDTO(
    @field:Schema(description = "최근 검색어 id — 단건 삭제 시 사용", example = "1")
    val id: Long,

    @field:Schema(description = "검색어", example = "된장찌개")
    val query: String,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @field:Schema(description = "마지막으로 검색한 시각", example = "2026-06-03 08:12:00")
    val searchedAt: LocalDateTime,
)
