package com.gerd.domain.food.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.food.dto.FoodCategoryDTO
import com.gerd.domain.food.dto.FoodSearchResultDTO
import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.dto.RecentFoodDTO
import com.gerd.domain.food.exception.FoodErrorCode
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Food", description = "음식 검색 API")
@RequestMapping("/api/v1/foods")
interface FoodApi {

    @Operation(
        summary = "음식 분류 목록 조회",
        description = "전체 음식 분류를 sortOrder 순으로 반환. 아이콘 표시 시 code를 키로 사용",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/categories")
    fun getCategories(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<List<FoodCategoryDTO>>>

    @Operation(
        summary = "음식 검색",
        description = """
            음식 이름으로 검색합니다(공백 무시 부분일치), 응답은 음식 외부 식별자(externalId)와 분류 목록을 포함합니다.
            - q: 검색어(필수, 앞뒤 공백 제거 후 1자 이상), 한글·영어 그대로 입력
            - size: 결과 수(기본 10, 최대 50). 범위를 벗어나면 보정합니다.
            - 노출 범위: 공개 카탈로그 + 본인이 추가한 비공개 음식.
            - hasExactMatch=false이면 직접 입력 판정(GET /api/v1/foods/judgment?name=...) 유도.
        """,
    )
    @ApiErrorExample(FoodErrorCode::class, "INVALID_SEARCH_QUERY")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "검색 성공(결과 없으면 빈 배열)"))
    @GetMapping("/search")
    fun search(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "검색어", example = "된장찌개") @RequestParam(required = false) q: String?,
        @Parameter(description = "결과 수(기본 10, 최대 50)", example = "10") @RequestParam(required = false) size: Int?,
    ): ResponseEntity<ApiResponse<FoodSearchResultDTO>>

    @Operation(
        summary = "최근 검색어 조회",
        description = "본인이 최근 검색한 검색어를 최신순으로 반환합니다(기본 10, 최대 50).",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"))
    @GetMapping("/recent")
    fun getRecent(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "결과 수(기본 10, 최대 50)", example = "10") @RequestParam(required = false) size: Int?,
    ): ResponseEntity<ApiResponse<List<RecentFoodDTO>>>

    @Operation(
        summary = "최근 검색어 단건 삭제",
        description = "본인의 최근 검색어 1건을 id로 삭제합니다.",
    )
    @ApiErrorExample(FoodErrorCode::class, "RECENT_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "삭제 성공"))
    @DeleteMapping("/recent/{id}")
    fun deleteRecent(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "최근 검색어 id", example = "1")
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(summary = "최근 검색어 전체 삭제", description = "본인의 최근 검색 기록을 모두 삭제합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "전체 삭제 성공"))
    @DeleteMapping("/recent")
    fun deleteAllRecent(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "가장 많이 검색된 음식 TOP 3",
        description = "전체 유저의 검색 기록 기준 상위 3개 음식을 반환합니다. 검색 기록이 없으면 빈 배열.",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/top-searched")
    fun getTopSearched(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<List<FoodSummaryDTO>>>
}
