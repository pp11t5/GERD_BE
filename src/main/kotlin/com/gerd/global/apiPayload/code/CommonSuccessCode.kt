package com.gerd.global.apiPayload.code

import org.springframework.http.HttpStatus

/**
 * 공통 성공 응답 코드
 *
 * 관리 규칙:
 * - code 형식: COMMON{HTTP상태코드}_{순번} (순번은 같은 상태코드 내에서 중복 방지용)
 * - 도메인 고유 성공 응답이 필요하면 각 도메인 패키지에 *SuccessCode enum으로 분리 (BaseSuccessCode 구현)
 * - 단순 200/201로 충분한 경우 OK/CREATED 사용
 */
enum class CommonSuccessCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseSuccessCode {
    // 200
    OK(HttpStatus.OK, "COMMON200", "요청에 성공하였습니다."),

    // 201
    CREATED(HttpStatus.CREATED, "COMMON201", "생성에 성공하였습니다."),

    // 202
    ACCEPTED(HttpStatus.ACCEPTED, "COMMON202", "요청이 접수되었습니다."),

    // 203
    NON_AUTHORITATIVE_INFORMATION(HttpStatus.NON_AUTHORITATIVE_INFORMATION, "COMMON203", "신뢰할 수 없는 정보입니다."),

    // 204
    NO_CONTENT(HttpStatus.NO_CONTENT, "COMMON204", "처리에 성공하였습니다."),
}
