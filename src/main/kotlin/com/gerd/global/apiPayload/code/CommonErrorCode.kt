package com.gerd.global.apiPayload.code

import com.gerd.global.apiPayload.code.BaseErrorCode
import org.springframework.http.HttpStatus

/**
 * 공통 에러 코드 — 도메인과 무관한 시스템/입력 레벨 에러
 *
 * 관리 규칙:
 * - code 형식: COMMON{HTTP상태코드}_{순번}
 * - 도메인 고유 에러는 각 도메인 패키지에 *ErrorCode enum으로 분리 (BaseErrorCode 구현)
 * - GlobalExceptionHandler에서 공통 에러 처리 시 이 enum을 사용
 * - 새 공통 에러 추가 시 같은 상태코드 내 순번을 이어서 부여
 */
enum class CommonErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {

    // 400
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400_1", "잘못된 요청입니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "COMMON400_2", "요청 형식이 올바르지 않습니다."),
    INVALID_DATE(HttpStatus.BAD_REQUEST, "COMMON400_3", "날짜 형식이 올바르지 않습니다."),

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401_1", "인증이 필요합니다."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403_1", "접근 권한이 없습니다."),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404_1", "요청한 리소스를 찾을 수 없습니다."),

    // 409
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "COMMON409_1", "이미 존재하는 리소스입니다."),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "COMMON409_2", "동시 수정 충돌이 발생했습니다."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500_1", "서버 오류가 발생했습니다."),
}
