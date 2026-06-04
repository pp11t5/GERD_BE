package com.gerd.global.annotation

import com.gerd.global.apiPayload.code.BaseErrorCode
import kotlin.reflect.KClass

/**
 * Swagger 도메인 에러 예시 선언
 *
 * 어노테이션 멤버는 인터페이스(BaseErrorCode) 타입을 가질 수 없어, 에러코드 enum 클래스와 상수 이름으로 받는다.
 * codes를 비우면 해당 enum의 전체 상수를 문서화한다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ApiErrorExample(
    val value: KClass<out BaseErrorCode>,
    vararg val codes: String,
)
