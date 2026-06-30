package com.gerd.infra.monitoring.sentry.controller

import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping

@Tag(name = "Monitoring", description = "모니터링 검증 API")
interface SentryTestApi {

    @Operation(
        summary = "Sentry 에러 수집 검증",
        description = "staging profile에서만 활성화되는 Sentry Issue 생성 검증용 API입니다.",
    )
    @ApiResponses(SwaggerResponse(responseCode = "500", description = "의도적 서버 에러"))
    @GetMapping("/api/v1/monitoring/sentry-test/error")
    fun throwSentryTestError(): ApiResponse<Unit>
}
