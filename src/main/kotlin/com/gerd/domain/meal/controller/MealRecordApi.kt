package com.gerd.domain.meal.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.meal.dto.MealCandidatesDTO
import com.gerd.domain.meal.dto.MealFoodRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordByIDRequestDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordTextRequestDTO
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "MealRecord", description = "식사 기록 API")
@RequestMapping("/api/v1/meal-records")
interface MealRecordApi {


    @Operation(
        summary = "신규 끼니 + 음식 추가 (ID)",
        description = "카탈로그 음식 ID로 신규 끼니를 생성하고 첫 음식으로 기록합니다. 신호등 판정은 캐시 우선 조회합니다.",
    )
    @ApiErrorExample(MealErrorCode::class, "INVALID_DATE_TIME", "MEAL_RECORD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "생성 성공"))
    @PostMapping("/foods/{foodId}")
    fun createNew(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable foodId: String,
        @RequestBody(required = false) request: MealRecordByIDRequestDTO?,
    ): ResponseEntity<ApiResponse<MealFoodRecordDetailDTO>>

    @Operation(
        summary = "신규 끼니 + 음식 추가 (text)",
        description = """
            사용자가 직접 입력한 음식 이름으로 신규 끼니를 생성합니다.
            동일 이름 본인 USER 음식을 재사용하고, 없으면 비공개 USER 음식을 신규 생성합니다.
            신호등 등급은 텍스트 LLM 판정(캐시 우선)으로 결정합니다.
        """,
    )
    @ApiErrorExample(MealErrorCode::class, "INVALID_DATE_TIME")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "생성 성공"))
    @PostMapping
    fun createNewByText(
        @CurrentUser userDetails: CustomUserDetails,
        @RequestBody request: MealRecordTextRequestDTO,
    ): ResponseEntity<ApiResponse<MealFoodRecordDetailDTO>>


    @Operation(
        summary = "같이 먹은 음식 추가 (ID)",
        description = "기존 끼니에 카탈로그 음식 ID로 같이 먹은 음식을 추가합니다. 신호등 판정은 캐시 우선 조회합니다.",
    )
    @ApiErrorExample(MealErrorCode::class, "INVALID_DATE_TIME", "MEAL_RECORD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "생성 성공"))
    @PostMapping("/{mealRecordId}/foods/{foodId}")
    fun append(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "끼니 식별자(UUID)", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable mealRecordId: String,
        @Parameter(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable foodId: String,
        @RequestBody(required = false) request: MealRecordByIDRequestDTO?,
    ): ResponseEntity<ApiResponse<MealFoodRecordDetailDTO>>

    @Operation(
        summary = "같이 먹은 음식 추가 (text)",
        description = """
            기존 끼니에 텍스트 입력으로 같이 먹은 음식을 추가합니다.
            동일 이름 본인 USER 음식을 재사용하고, 없으면 비공개 USER 음식을 신규 생성합니다.
            신호등 등급은 텍스트 LLM 판정(캐시 우선)으로 결정합니다.
        """,
    )
    @ApiErrorExample(MealErrorCode::class, "INVALID_DATE_TIME", "MEAL_RECORD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "생성 성공"))
    @PostMapping("/{mealRecordId}/foods")
    fun appendByText(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "끼니 식별자(UUID)", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable mealRecordId: String,
        @RequestBody request: MealRecordTextRequestDTO,
    ): ResponseEntity<ApiResponse<MealFoodRecordDetailDTO>>

    @Operation(
        summary = "식사 음식 단건 조회",
        description = "끼니 조회에서 연결, mealFoodId로 단건 조회",
    )
    @ApiErrorExample(MealErrorCode::class, "MEAL_FOOD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/foods/{mealFoodId}")
    fun getDetail(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "식사 음식 외부 식별자(UUID)", example = "7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable mealFoodId: String,
    ): ResponseEntity<ApiResponse<MealFoodRecordDetailDTO>>

    @Operation(
        summary = "식사 음식 삭제",
        description = "mealFoodId로 단건 삭제, 끼니에 음식이 1개만 남으면 끼니도 함께 삭제됩니다.",
    )
    @ApiErrorExample(MealErrorCode::class, "MEAL_FOOD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "삭제 성공"))
    @DeleteMapping("/foods/{mealFoodId}")
    fun delete(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "식사 음식 외부 식별자(UUID)", example = "7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable mealFoodId: String,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "증상 미연결 끼니 목록 조회",
        description = "최근 24시간 이내 식사 중 증상 기록이 연결되지 않은 끼니를 날짜별로 반환, 증상 기록 시 원인 식사 선택에 사용",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"))
    @GetMapping("/candidates")
    fun getCandidates(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<List<MealCandidatesDTO>>>

    @Operation(
        summary = "끼니 상세 조회",
        description = "끼니에 연결된 음식 기록, 증상 기록을 조회합니다.",
    )
    @ApiErrorExample(MealErrorCode::class, "MEAL_RECORD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/{mealRecordId}")
    fun getGroupDetail(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "끼니 식별자(UUID)", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable mealRecordId: String,
    ): ResponseEntity<ApiResponse<MealRecordDetailDTO>>

    @Operation(summary = "끼니 전체 삭제", description = "소속 음식 포함 전체 삭제")
    @ApiErrorExample(MealErrorCode::class, "MEAL_RECORD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "삭제 성공"))
    @DeleteMapping("/{mealRecordId}")
    fun deleteMealRecord(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "끼니 식별자(UUID)", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable mealRecordId: String,
    ): ResponseEntity<ApiResponse<Unit>>
}
