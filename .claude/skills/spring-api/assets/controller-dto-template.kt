// ─── [Domain]RequestDTO.kt ──────────────────────────────────────────────────
// Request DTO는 도메인 이름으로 파일을 분리하고 sealed/object로 묶는다
// Validation message는 사용자에게 노출되는 문장으로 작성한다

object DomainRequestDTO {

    data class Create(
        @NotBlank(message = "<필드> 값은 필수입니다")
        @Size(max = 100, message = "<필드>는 100자 이하로 입력해 주세요")
        val title: String,

        @Size(max = 1000, message = "<필드>는 1000자 이하로 입력해 주세요")
        val content: String?,

        // 형식 검증이 필요한 경우 @Pattern 추가
        @NotBlank(message = "<필드>는 필수입니다")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "<필드>는 영소문자·숫자·하이픈만 허용됩니다")
        val slug: String,

        @NotNull(message = "<외래키> ID는 필수입니다")
        val categoryId: Long,
    )

    data class Update(
        @NotBlank(message = "<필드> 값은 필수입니다")
        @Size(max = 100, message = "<필드>는 100자 이하로 입력해 주세요")
        val title: String,

        @Size(max = 1000, message = "<필드>는 1000자 이하로 입력해 주세요")
        val content: String?,
    )
}


// ─── [Domain]ResponseDto.kt ─────────────────────────────────────────────────

object DomainResponseDto {

    data class DomainDetail(
        val id: Long,
        val title: String,
        val content: String?,
        val createdAt: LocalDateTime,
    )

    data class DomainSliceDto(
        val items: List<DomainDetail>,
        val nextCursor: Long?,
        val hasNext: Boolean,
    )
}


// ─── [Domain]Api.kt ─────────────────────────────────────────────────────────

@Tag(name = "Domain", description = "도메인 API")
interface DomainApi {

    @Operation(
        summary = "도메인 목록 조회",
        description = """
            도메인 목록을 커서 기반 페이지네이션으로 조회합니다.
            - `resourceId` : 조회 대상 리소스 ID
            - `cursor` : null이면 최신 데이터부터 조회
            - `size` : 조회 개수 (기본값 20)
            - 인증되지 않으면 인증 에러를 반환합니다
            - 다른 사용자의 리소스에 접근하면 권한 에러를 반환합니다
        """,
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "도메인 목록 조회 성공")])
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
            - 인증되지 않으면 인증 에러를 반환합니다
            - 검증 실패 시 각 필드의 message가 에러 응답에 포함됩니다
        """,
    )
    @ApiResponses(value = [ApiResponse(responseCode = "201", description = "도메인 생성 성공")])
    @PostMapping("/resources/{resourceId}/domains")
    fun createDomain(
        @CurrentUser user: User,
        @PathVariable resourceId: Long,
        @RequestBody request: DomainRequestDto.Create,
    ): ApiResponse<DomainResponseDto.DomainDetail>

    @Operation(
        summary = "도메인 수정",
        description = """
            도메인 데이터를 수정합니다.
            - 본인 소유가 아니면 권한 에러를 반환합니다
        """,
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "도메인 수정 성공")])
    @PutMapping("/resources/{resourceId}/domains/{domainId}")
    fun updateDomain(
        @CurrentUser user: User,
        @PathVariable resourceId: Long,
        @PathVariable domainId: Long,
        @RequestBody request: DomainRequestDto.Update,
    ): ApiResponse<DomainResponseDto.DomainDetail>

    @Operation(
        summary = "도메인 삭제",
        description = """
            도메인 데이터를 삭제합니다.
            - 본인 소유가 아니면 권한 에러를 반환합니다
        """,
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "도메인 삭제 성공")])
    @DeleteMapping("/resources/{resourceId}/domains/{domainId}")
    fun deleteDomain(
        @CurrentUser user: User,
        @PathVariable resourceId: Long,
        @PathVariable domainId: Long,
    ): ApiResponse<Void>
}


// ─── [Domain]Controller.kt ──────────────────────────────────────────────────
// @Valid는 Controller override 메서드에 선언한다 (인터페이스에 두지 않는다)

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

    override fun updateDomain(
        @CurrentUser user: User,
        @PathVariable resourceId: Long,
        @PathVariable domainId: Long,
        @RequestBody @Valid request: DomainRequestDto.Update,
    ): ApiResponse<DomainResponseDto.DomainDetail> =
        ApiResponse.onSuccess(
            domainCommandService.updateDomain(user, resourceId, domainId, request),
        )

    override fun deleteDomain(
        @CurrentUser user: User,
        @PathVariable resourceId: Long,
        @PathVariable domainId: Long,
    ): ApiResponse<Void> {
        domainCommandService.deleteDomain(user, resourceId, domainId)
        return ApiResponse.onSuccess()
    }
}
