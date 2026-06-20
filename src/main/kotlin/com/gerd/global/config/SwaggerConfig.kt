package com.gerd.global.config

import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.annotation.CurrentUser
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.fcm.exception.FcmErrorCode
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.notification.exception.NotificationErrorCode
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.global.apiPayload.code.BaseErrorCode
import com.gerd.global.apiPayload.code.CommonErrorCode
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.MethodParameter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.ReflectionUtils
import org.springframework.web.method.HandlerMethod
import java.lang.reflect.ParameterizedType

@Configuration
class SwaggerConfig {

    companion object {
        private const val JWT_SCHEME = "JWT TOKEN"
    }

    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .addServersItem(Server().url("/"))
            .info(
                Info()
                    .title("Backend API")
                    .description("Backend API 명세서" + buildErrorCodeReference())
                    .version("1.0.0"),
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        JWT_SCHEME,
                        SecurityScheme()
                            .name(JWT_SCHEME)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"),
                    )
                    // 공통 에러 응답 스키마 등록
                    .addResponses("400", errorResponse("잘못된 요청입니다."))
                    .addResponses("401", errorResponse("인증이 필요합니다."))
                    .addResponses("403", errorResponse("접근 권한이 없습니다."))
                    .addResponses("404", errorResponse("리소스를 찾을 수 없습니다."))
                    .addResponses("409", errorResponse("리소스 충돌이 발생했습니다."))
                    .addResponses("500", errorResponse("서버 내부 오류가 발생했습니다.")),
            )

    private fun buildErrorCodeReference(): String {
        val errorEnums: List<Class<out BaseErrorCode>> = listOf(
            CommonErrorCode::class.java,
            AuthErrorCode::class.java,
            FoodErrorCode::class.java,
            FcmErrorCode::class.java,
            OnboardingErrorCode::class.java,
            NotificationErrorCode::class.java,
        )

        return buildString {
            appendLine()
            appendLine("---")
            appendLine("### 도메인별 에러코드")
            appendLine()

            errorEnums.forEach { errorEnum ->
                appendLine("<details>")
                appendLine("<summary><b>${errorEnum.simpleName}</b></summary>")
                appendLine()
                appendLine("| Code | HTTP | Message |")
                appendLine("|------|:----:|---------|")

                errorEnum.enumConstants.forEach { errorCode ->
                    appendLine(
                        "| `${errorCode.code}` | ${errorCode.httpStatus.value()} | ${errorCode.message.escapeMarkdownTableCell()} |",
                    )
                }

                appendLine()
                appendLine("</details>")
                appendLine()
            }
        }
    }

    private fun String.escapeMarkdownTableCell(): String =
        replace("|", "\\|")

    // 모든 Operation 공통 에러 응답 자동 추가
    @Bean
    fun globalErrorResponseCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, handlerMethod ->
            val responses = operation.responses ?: ApiResponses().also { operation.responses = it }
            val hasCurrentUser = handlerMethod.methodParameters
                .any { it.hasParameterAnnotation(CurrentUser::class.java) }

            responses
                .addApiResponse("400", ApiResponse().`$ref`("#/components/responses/400"))
                .addApiResponse("500", ApiResponse().`$ref`("#/components/responses/500"))

            if (hasCurrentUser) {
                responses.addApiResponse("401", ApiResponse().`$ref`("#/components/responses/401"))
                operation.addSecurityItem(SecurityRequirement().addList(JWT_SCHEME))
            }

            findApiErrorExample(handlerMethod)
                ?.let { resolveErrorCodes(it) }
                ?.groupBy { it.httpStatus.value().toString() }
                ?.forEach { (statusCode, errorCodes) ->
                    responses.addApiResponse(statusCode, errorResponse(errorCodes))
                }

            val hiddenParameterNames = handlerMethod.methodParameters
                .filter { it.hasParameterAnnotation(CurrentUser::class.java) }
                .mapNotNull(MethodParameter::getParameterName)
                .toSet()

            if (hiddenParameterNames.isNotEmpty()) {
                operation.parameters = operation.parameters
                    ?.filterNot { parameter -> parameter.name in hiddenParameterNames }
            }

            if (handlerMethod.returnsEmptyApiResponse()) {
                responses["200"]?.let { successResponse ->
                    successResponse.content = emptySuccessContent()
                }
            }

            operation
        }

    private fun HandlerMethod.returnsEmptyApiResponse(): Boolean {
        val returnType = method.genericReturnType as? ParameterizedType ?: return false
        val responseBodyType = returnType.actualTypeArguments.firstOrNull() as? ParameterizedType ?: return false
        if (responseBodyType.rawType.typeName != "com.gerd.global.apiPayload.ApiResponse") return false
        return responseBodyType.actualTypeArguments.firstOrNull()?.typeName == "kotlin.Unit"
    }

    private fun emptySuccessContent(): Content =
        Content().addMediaType(
            "application/json",
            MediaType()
                .schema(
                    Schema<Any>().apply {
                        type = "object"
                        properties = mapOf(
                            "isSuccess" to Schema<Boolean>().apply { type = "boolean"; example = true },
                            "code" to Schema<String>().apply { type = "string"; example = "COMMON200" },
                            "message" to Schema<String>().apply { type = "string"; example = "성공입니다." },
                            "result" to Schema<Any>().apply {
                                nullable = true
                                example = null
                            },
                        )
                    },
                )
                .example(
                    mapOf(
                        "isSuccess" to true,
                        "code" to "COMMON200",
                        "message" to "성공입니다.",
                        "result" to null,
                    ),
                ),
        )

    // 선언된 enum 클래스의 상수를 이름으로 선별 — codes가 비면 전체 상수를 문서화
    private fun resolveErrorCodes(annotation: ApiErrorExample): List<BaseErrorCode> {
        val constants = annotation.value.java.enumConstants?.toList() ?: return emptyList()
        if (annotation.codes.isEmpty()) return constants
        return annotation.codes.mapNotNull { name ->
            constants.firstOrNull { (it as Enum<*>).name == name }
        }
    }

    private fun findApiErrorExample(handlerMethod: HandlerMethod): ApiErrorExample? =
        AnnotatedElementUtils.findMergedAnnotation(handlerMethod.method, ApiErrorExample::class.java)
            ?: handlerMethod.beanType.interfaces
                .asSequence()
                .mapNotNull { interfaceType ->
                    ReflectionUtils.findMethod(interfaceType, handlerMethod.method.name, *handlerMethod.method.parameterTypes)
                }
                .mapNotNull { interfaceMethod ->
                    AnnotatedElementUtils.findMergedAnnotation(interfaceMethod, ApiErrorExample::class.java)
                }
                .firstOrNull()

    private fun errorResponse(message: String): ApiResponse =
        ApiResponse()
            .description(message)
            .content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(
                        Schema<Any>().apply {
                            type = "object"
                            properties = mapOf(
                                "isSuccess" to Schema<Boolean>().apply { type = "boolean"; example = false },
                                "code" to Schema<String>().apply { type = "string"; example = "COMMON400_1" },
                                "message" to Schema<String>().apply { type = "string"; example = message },
                                "result" to Schema<Any>().apply { nullable = true },
                                "traceId" to Schema<String>().apply { type = "string"; example = "550e8400-e29b-41d4-a716-446655440000" },
                            )
                        },
                    ),
                ),
            )

    private fun errorResponse(errorCodes: List<BaseErrorCode>): ApiResponse {
        val examples = linkedMapOf<String, Example>()
        errorCodes.forEach { errorCode ->
            examples[errorCode.code] = Example().value(
                mapOf(
                    "isSuccess" to false,
                    "code" to errorCode.code,
                    "message" to errorCode.message,
                    "result" to null,
                    "traceId" to "550e8400-e29b-41d4-a716-446655440000",
                ),
            )
        }

        return ApiResponse()
            .description(errorCodes.joinToString(" / ") { it.message })
            .content(
                Content().addMediaType(
                    "application/json",
                    MediaType()
                        .schema(
                            Schema<Any>().apply {
                                type = "object"
                                properties = mapOf(
                                    "isSuccess" to Schema<Boolean>().apply { type = "boolean"; example = false },
                                    "code" to Schema<String>().apply { type = "string"; example = errorCodes.first().code },
                                    "message" to Schema<String>().apply { type = "string"; example = errorCodes.first().message },
                                    "result" to Schema<Any>().apply { nullable = true },
                                    "traceId" to Schema<String>().apply { type = "string"; example = "550e8400-e29b-41d4-a716-446655440000" },
                                )
                            },
                        )
                        .examples(examples),
                ),
            )
    }
}
