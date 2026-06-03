package com.gerd.domain.food.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.food.dto.AddRecentRequestDTO
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Food", description = "음식 검색 API")
@RequestMapping("/api/v1/foods")
interface FoodApi {

    @Operation(
        summary = "음식 검색",
        description = """
            음식 이름으로 검색합니다(공백 무시 부분일치). 응답은 음식 외부 식별자(externalId)와 분류 목록을 포함합니다.
            - q: 검색어(필수, 앞뒤 공백 제거 후 1자 이상). 한글·영어 그대로 입력.
            - size: 결과 수(기본 10, 최대 50). 범위를 벗어나면 보정합니다.
            - 노출 범위: 공개 카탈로그 + 본인이 추가한 비공개 음식.
        """,
    )
    @ApiErrorExample(FoodErrorCode::class, "INVALID_SEARCH_QUERY")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "검색 성공(결과 없으면 빈 배열)"))
    @GetMapping("/search")
    fun search(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "검색어", example = "된장찌개") @RequestParam(required = false) q: String?,
        @Parameter(description = "결과 수(기본 10, 최대 50)", example = "10") @RequestParam(required = false) size: Int?,
    ): ResponseEntity<ApiResponse<List<FoodSummaryDTO>>>

    @Operation(
        summary = "최근 본 음식 조회",
        description = "본인이 최근 본 음식을 최신순으로 반환합니다(기본 10, 최대 50). 삭제된 음식은 제외됩니다.",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"))
    @GetMapping("/recent")
    fun getRecent(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "결과 수(기본 10, 최대 50)", example = "10") @RequestParam(required = false) size: Int?,
    ): ResponseEntity<ApiResponse<List<RecentFoodDTO>>>

    @Operation(
        summary = "최근 본 음식 추가",
        description = """
            음식 상세 진입 시 호출합니다. (user, food) 단위로 upsert되어 같은 음식 재진입 시 시각만 갱신됩니다.
            보관 상한(10) 초과 시 오래된 항목부터 삭제됩니다.
        """,
    )
    @ApiErrorExample(FoodErrorCode::class, "FOOD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "추가/갱신 성공"))
    @PostMapping("/recent")
    fun addRecent(
        @CurrentUser userDetails: CustomUserDetails,
        @RequestBody request: AddRecentRequestDTO,
    ): ResponseEntity<ApiResponse<RecentFoodDTO>>

    @Operation(summary = "최근 본 음식 단건 삭제", description = "본인의 최근 검색 항목 1건을 삭제합니다.")
    @ApiErrorExample(FoodErrorCode::class, "RECENT_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "삭제 성공"))
    @DeleteMapping("/recent/{recentId}")
    fun deleteRecent(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "최근 검색 항목 ID", example = "1024") @PathVariable recentId: Long,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(summary = "최근 본 음식 전체 삭제", description = "본인의 최근 검색 기록을 모두 삭제합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "전체 삭제 성공"))
    @DeleteMapping("/recent")
    fun deleteAllRecent(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<Unit>>
}
