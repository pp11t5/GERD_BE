package com.gerd.domain.meal.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.meal.dto.CreateMealRecordRequestDTO
import com.gerd.domain.meal.dto.MealGroupDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordSummaryDTO
import com.gerd.domain.meal.dto.UpdateMealMemoRequestDTO
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Meal", description = "식사 기록 API")
@RequestMapping("/api/v1/meals")
interface MealApi {

    @Operation(
        summary = "식사 기록 생성",
        description = """
            식사 가이드 "내 식단에 추가" / 타임라인 "같이 먹은 음식이 있나요?"에서 호출합니다.
            - foodExternalId: 추가할 음식(공개 카탈로그 + 본인 비공개 음식만, 아니면 FOOD404_1).
            - eatenAt: ISO-8601(offset 포함). 미전달 시 서버 현재 시각(Asia/Seoul). 형식 오류는 MEAL400_2.
            - mealGroupId: 미전달 = 새 끼니(서버가 uuid 발급), 전달 = 기존 끼니에 추가(본인 소유 검증, 아니면 MEAL404_2).
            - judgedGrade: FE가 신호등 판정 화면에서 들고 온 등급 스냅샷. 미전달/null 가능.
        """,
    )
    @ApiErrorExample(MealErrorCode::class, "INVALID_DATE_TIME", "MEAL_GROUP_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "생성 성공"))
    @PostMapping
    fun create(
        @CurrentUser userDetails: CustomUserDetails,
        @RequestBody request: CreateMealRecordRequestDTO,
    ): ResponseEntity<ApiResponse<MealRecordSummaryDTO>>

    @Operation(
        summary = "식사 기록 단건 조회",
        description = """
            식사 상세 정보 화면. 기록 도메인 데이터(음식 요약·설명·eatenAt·memo·상태 기록)만 반환합니다.
            개인화 분석(헤드라인·항목)은 FE가 `GET /api/v1/foods/{foodId}/judgment`를 병행 호출합니다.
            음식이 삭제(soft-delete)됐어도 기록의 음식 정보는 그대로 반환됩니다.
        """,
    )
    @ApiErrorExample(MealErrorCode::class, "MEAL_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/{mealId}")
    fun getDetail(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "식사 기록 외부 식별자(UUID)", example = "7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable mealId: String,
    ): ResponseEntity<ApiResponse<MealRecordDetailDTO>>

    @Operation(
        summary = "식사 기록 메모 수정",
        description = """
            수정 화면 "추가 메모 기록" 저장. 편집 가능한 필드는 메모뿐입니다(0~200자, 초과 시 MEAL400_1).
            null/빈 문자열은 메모 삭제입니다.
        """,
    )
    @ApiErrorExample(MealErrorCode::class, "MEMO_TOO_LONG", "MEAL_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "수정 성공"))
    @PatchMapping("/{mealId}")
    fun updateMemo(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "식사 기록 외부 식별자(UUID)", example = "7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable mealId: String,
        @RequestBody request: UpdateMealMemoRequestDTO,
    ): ResponseEntity<ApiResponse<MealRecordDetailDTO>>

    @Operation(
        summary = "식사 기록 삭제",
        description = "기록 1건을 soft delete 합니다. 멱등이 아니며 이미 삭제된 기록은 MEAL404_1. 끼니 단위 삭제 개념은 없습니다.",
    )
    @ApiErrorExample(MealErrorCode::class, "MEAL_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "삭제 성공"))
    @DeleteMapping("/{mealId}")
    fun delete(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "식사 기록 외부 식별자(UUID)", example = "7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable mealId: String,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "타임라인 날짜별 식사 기록 조회",
        description = """
            해당 날짜(Asia/Seoul 00:00~24:00) 1일치를 끼니(mealGroupId) 단위로 그룹핑해 반환합니다 — 카드 1장 = 끼니 1개.
            끼니 정렬은 대표 시각(소속 기록 최솟값) 오름차순, 끼니 안 records도 오름차순. date 미전달 시 오늘(Asia/Seoul).
            끼니가 자정에 걸치면 각 기록이 자기 날짜 응답에 나뉘어 실립니다.
        """,
    )
    @ApiErrorExample(MealErrorCode::class, "INVALID_DATE_TIME")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"))
    @GetMapping
    fun getDaily(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "조회 날짜(YYYY-MM-DD). 미전달 시 오늘(Asia/Seoul)", example = "2026-06-11")
        @RequestParam(required = false) date: String?,
    ): ResponseEntity<ApiResponse<List<MealGroupDTO>>>
}
