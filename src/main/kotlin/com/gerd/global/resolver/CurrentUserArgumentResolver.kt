package com.gerd.global.resolver

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.CustomUserDetails
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUser::class.java) &&
            parameter.parameterType == User::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication == null || authentication.principal !is CustomUserDetails) {
            throw GeneralException(AuthErrorCode.UNAUTHORIZED)
        }

        return (authentication.principal as CustomUserDetails).user
    }
}
