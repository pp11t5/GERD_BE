// ─── DomainApi.kt (Swagger 설명 전담 인터페이스) ───────────────────────────────

@Tag(name = "Domain", description = "도메인 API")
interface DomainApi {

    @Operation(
        summary = "도메인 목록 조회",
        description = """
            사용자의 도메인 데이터를 조회합니다.
            - `resourceId` : 조회 대상 리소스 ID입니다
            - `cursor` : null이면 최신 데이터부터 조회합니다
            - `size` : 조회 개수입니다
            - 다른 사용자의 리소스에 접근하면 권한 에러를 반환합니다
        """,
    )
    @ApiErrorExample(
        DomainErrorCode.RESOURCE_NOT_FOUND,
        DomainErrorCode.FORBIDDEN,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "도메인 목록 조회 성공"),
        ],
    )
    @GetMapping("/resources/{resourceId}/domains")
    fun getDomains(
        @CurrentUser user: User,
        @PathVariable resourceId: Long,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<DomainResponseDto.DomainSliceDto>

    @Operation(
        summary = "도메인 생성",
        description = """
            도메인 데이터를 생성합니다.
            - 필수 입력값이 누락되면 잘못된 요청 응답을 반환합니다
        """,
    )
    @ApiErrorExample(
        DomainErrorCode.RESOURCE_NOT_FOUND,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "도메인 생성 성공"),
        ],
    )
    @PostMapping("/resources/{resourceId}/domains")
    fun createDomain(
        @CurrentUser user: User,
        @PathVariable resourceId: Long,
        @RequestBody request: DomainRequestDto.Create,
    ): ApiResponse<DomainResponseDto.DomainDetail>
}


// ─── DomainController.kt (구현체 — Swagger 어노테이션 없음) ─────────────────────

@RestController
@RequestMapping("/api/v1")
class DomainController(
    private val domainQueryService: DomainQueryService,
    private val domainCommandService: DomainCommandService,
) : DomainApi {

    override fun getDomains(
        @CurrentUser user: User,
        @PathVariable resourceId: Long,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<DomainResponseDto.DomainSliceDto> =
        ApiResponse.onSuccess(
            domainQueryService.getDomains(user.id, resourceId, cursor, size),
        )

    override fun createDomain(
        @CurrentUser user: User,
        @PathVariable resourceId: Long,
        @RequestBody @Valid request: DomainRequestDto.Create,
    ): ApiResponse<DomainResponseDto.DomainDetail> =
        ApiResponse.onSuccess(
            domainCommandService.createDomain(user, resourceId, request),
            SuccessCode.CREATED,
        )
}

// 참고:
// - 400, 500은 SwaggerConfig가 자동 추가
// - @CurrentUser가 있으므로 401과 JWT security requirement도 자동 추가
// - 도메인별 403, 404 등은 @ApiErrorExample(...)에 선언한 값으로 example이 생성됨
