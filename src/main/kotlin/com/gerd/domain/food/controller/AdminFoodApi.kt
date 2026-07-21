package com.gerd.domain.food.controller

import com.gerd.domain.food.dto.AdminUserFoodDTO
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.common.response.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "관리자 음식 관리", description = "관리자 음식 관리 API")
@RequestMapping("/api/v1/admin/foods")
interface AdminFoodApi {

    @Operation(
        summary = "유저 음식 전체 조회",
        description = """
            사용자들이 텍스트로 등록한 음식 전체를 100개씩 페이지 단위로 조회합니다.
            isUnknown 값은 사용자가 신호등 판정 시에 unknown이라고 판정된 여부를 나타냅니다. 
        """,
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping
    fun getAllUserFoods(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "true=unknown만, false=known만, 생략=전체")
        @RequestParam(required = false) isUnknown: Boolean?,
    ): ResponseEntity<ApiResponse<PageResponse<AdminUserFoodDTO>>>

    @Operation(
        summary = "유저 음식 승격",
        description = "특정 음식을 개인 검색 결과에만 노출되는 것이 아니라 모든 사용자에게 노출되도록 승격시킵니다.",
    )
    @ApiErrorExample(FoodErrorCode::class, "FOOD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "승격 성공"))
    @PostMapping("/{foodExternalId}/promote")
    fun promote(
        @Parameter(description = "음식 외부 식별자(UUID)") @PathVariable foodExternalId: String,
    ): ResponseEntity<ApiResponse<Unit>>
}
