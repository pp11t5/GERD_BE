package com.gerd.global.exception

import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.apiPayload.code.CommonErrorCode
import jakarta.persistence.OptimisticLockException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tools.jackson.databind.exc.InvalidFormatException
import java.time.LocalDate

private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

/**
 * 전역 예외 응답 변환기
 *
 * - 도메인 예외의 공통 `ApiResponse` 변환
 * - 입력 검증 예외의 상태코드 일관성 유지
 * - 미처리 예외의 내부 정보 비노출
 */
@RestControllerAdvice(annotations = [RestController::class])
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    // URI 파라미터 validation 실패 처리
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(e: ConstraintViolationException, request: WebRequest): ResponseEntity<Any> {
        val message = e.constraintViolations.firstOrNull()?.message ?: "잘못된 요청입니다."
        val body = ApiResponse.onFailure(CommonErrorCode.INVALID_REQUEST, message)
        return handleExceptionInternal(e, body, HttpHeaders(), CommonErrorCode.INVALID_REQUEST.httpStatus, request)
            ?: ResponseEntity(body, CommonErrorCode.INVALID_REQUEST.httpStatus)
    }

    // DB 제약조건 위반의 상태코드 분기
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(e: DataIntegrityViolationException, request: WebRequest): ResponseEntity<Any> {
        val cause = e.mostSpecificCause.message ?: ""
        log.warn("DataIntegrityViolationException: {}", cause)
        // vendor별 예외 타입 차이 대응용 메시지 기반 분기
        val errorCode = if (cause.contains("unique", ignoreCase = true) ||
            cause.contains("duplicate", ignoreCase = true)
        ) CommonErrorCode.DUPLICATE_RESOURCE else CommonErrorCode.INVALID_REQUEST
        val body = ApiResponse.onFailure<Nothing>(errorCode)
        return handleExceptionInternal(e, body, HttpHeaders(), errorCode.httpStatus, request)
            ?: ResponseEntity(body, errorCode.httpStatus)
    }

    // 요청 바디 파싱 실패의 타입별 분기
    override fun handleHttpMessageNotReadable(
        e: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val cause = e.cause
        if (cause is InvalidFormatException && cause.targetType == LocalDate::class.java) {
            val body = ApiResponse.onFailure<Nothing>(CommonErrorCode.INVALID_DATE)
            return handleExceptionInternal(e, body, headers, CommonErrorCode.INVALID_DATE.httpStatus, request)
        }
        log.debug("HttpMessageNotReadableException: {}", e.message)
        val body = ApiResponse.onFailure<Nothing>(CommonErrorCode.INVALID_FORMAT)
        return handleExceptionInternal(e, body, headers, CommonErrorCode.INVALID_FORMAT.httpStatus, request)
    }

    // 단일 파라미터 타입 불일치 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(e: MethodArgumentTypeMismatchException, request: WebRequest): ResponseEntity<Any> {
        log.debug("MethodArgumentTypeMismatchException: param={}", e.name)
        val body = ApiResponse.onFailure<Nothing>(CommonErrorCode.INVALID_REQUEST)
        return handleExceptionInternal(e, body, HttpHeaders(), CommonErrorCode.INVALID_REQUEST.httpStatus, request)
            ?: ResponseEntity(body, CommonErrorCode.INVALID_REQUEST.httpStatus)
    }

    // 필수 request parameter 누락 처리
    override fun handleMissingServletRequestParameter(
        e: MissingServletRequestParameterException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val message = "필수 파라미터 '${e.parameterName}'가 누락되었습니다."
        val body = ApiResponse.onFailure(CommonErrorCode.INVALID_REQUEST, message)
        return handleExceptionInternal(e, body, headers, CommonErrorCode.INVALID_REQUEST.httpStatus, request)
    }

    // request body validation 실패 처리
    override fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "") }
        val body = ApiResponse.onFailure(CommonErrorCode.INVALID_REQUEST, errors)
        return handleExceptionInternal(e, body, headers, CommonErrorCode.INVALID_REQUEST.httpStatus, request)
    }

    // model binding 전용 검증 실패 처리
    @ExceptionHandler(BindException::class)
    fun handleBindException(e: BindException, request: WebRequest): ResponseEntity<Any> {
        val errors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "") }
        val body = ApiResponse.onFailure(CommonErrorCode.INVALID_REQUEST, errors)
        return handleExceptionInternal(e, body, HttpHeaders(), CommonErrorCode.INVALID_REQUEST.httpStatus, request)
            ?: ResponseEntity(body, CommonErrorCode.INVALID_REQUEST.httpStatus)
    }

    // 도메인 예외의 공통 응답 변환
    @ExceptionHandler(GeneralException::class)
    fun handleGeneralException(e: GeneralException, request: HttpServletRequest): ResponseEntity<Any> {
        log.warn("GeneralException: code={}, message={}", e.errorCode.code, e.message)
        val body = ApiResponse.onFailure<Nothing>(e.errorCode)
        return handleExceptionInternal(e, body, HttpHeaders(), e.errorCode.httpStatus, ServletWebRequest(request))
            ?: ResponseEntity(body, e.errorCode.httpStatus)
    }

    // 낙관적 락 충돌의 공통 매핑
    @ExceptionHandler(OptimisticLockException::class, ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLock(e: Exception, request: HttpServletRequest): ResponseEntity<Any> {
        log.warn("OptimisticLockException: {}", e.message)
        val body = ApiResponse.onFailure<Nothing>(CommonErrorCode.CONCURRENT_MODIFICATION)
        return handleExceptionInternal(e, body, HttpHeaders(), CommonErrorCode.CONCURRENT_MODIFICATION.httpStatus, ServletWebRequest(request))
            ?: ResponseEntity(body, CommonErrorCode.CONCURRENT_MODIFICATION.httpStatus)
    }

    // 미처리 예외의 내부 정보 비노출 처리
    @ExceptionHandler(Exception::class)
    fun handleUnknown(e: Exception, request: WebRequest): ResponseEntity<Any> {
        log.error("Unhandled exception", e)
        val body = ApiResponse.onFailure<Nothing>(CommonErrorCode.INTERNAL_SERVER_ERROR)
        return handleExceptionInternal(e, body, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request)
            ?: ResponseEntity(body, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
