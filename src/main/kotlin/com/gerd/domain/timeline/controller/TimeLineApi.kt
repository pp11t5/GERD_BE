package com.gerd.domain.timeline.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.timeline.dto.TimeLineResponseDTO
import com.gerd.domain.timeline.dto.WeeklyJudgementResponseDTO
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Tag(name = "TimeLine", description = "타임라인 API")
@RequestMapping("/api/v1/timeline")
interface TimeLineApi {

    @Operation(summary = "일별 타임라인 조회", description = "특정 날짜의 식사 기록과 증상 기록을 시간 순으로 조회합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping
    fun getTimeLine(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "조회 날짜 (yyyy-MM-dd)", example = "2026-06-21")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): ResponseEntity<ApiResponse<TimeLineResponseDTO>>

    @Operation(summary = "주간 판정 등급 조회", description = "해당 날짜가 속한 주(일~토)의 날짜별 판정 등급 목록을 조회합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/weekly")
    fun getWeeklyJudgements(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "조회 기준 날짜 (yyyy-MM-dd)", example = "2026-06-21")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): ResponseEntity<ApiResponse<List<WeeklyJudgementResponseDTO>>>
}
