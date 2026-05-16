// ─── CurrentUser.kt (커스텀 어노테이션) ─────────────────────────────────────────

import io.swagger.v3.oas.annotations.Parameter

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(hidden = true)
annotation class CurrentUser


// ─── CurrentUserArgumentResolver.kt ─────────────────────────────────────────────

@Component
class CurrentUserArgumentResolver(
    private val userRepository: UserRepository,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUser::class.java) &&
            parameter.parameterType == User::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): User {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = (authentication.principal as CustomUserDetails).userId
        return userRepository.findById(userId)
            ?: throw CustomException(UserErrorCode.USER_NOT_FOUND)
    }
}


// ─── WebMvcConfig.kt  ──────────────────────────────────────────────

@Configuration
class WebMvcConfig(
    private val currentUserArgumentResolver: CurrentUserArgumentResolver,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserArgumentResolver)
    }
}
