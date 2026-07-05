package com.gerd.domain.auth.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.auth.service.WithdrawService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 개발용 완전 탈퇴 — local/test/staging 전용
 * 유예기간을 우회해 본인 계정을 즉시 물리 삭제(unlink 포함)한다.
 * prod는 스케줄러 기반 유예 탈퇴만 사용하므로 이 엔드포인트를 노출하지 않는다.
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@Profile("local", "test", "staging")
class DevWithdrawController(
    private val withdrawService: WithdrawService,
) {

    @Operation(
        summary = "개발용 완전 탈퇴",
        description = """
            인증된 본인 계정을 유예 없이 즉시 물리 삭제합니다.
            - 유예 테스트 시에는 일반 탈퇴 API, 완전 탈퇴 후 재가입 로직 테스트를 위해서는 해당 API를 사용합니다.
            - 개발용 서버 환경에서만 활성화됩니다.
        """,
    )
    @DeleteMapping("/withdraw/hard")
    fun withdrawHard(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = userDetails.userId
        withdrawService.withdraw(userId)           // 토큰 정리 + soft delete(status=DELETED, deleted_at)
        withdrawService.withdrawHardDelete(userId) // kakao unlink + 물리 삭제(auth_account 캐스케이드)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }
}
