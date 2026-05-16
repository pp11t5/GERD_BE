package com.gerd.global.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@TestConfiguration
class TestSecurityConfig {

    // @WebMvcTest에서 @Import(TestSecurityConfig::class)로 사용
    // JWT 필터 없이 모든 요청 허용 → 컨트롤러 로직에만 집중
    @Bean
    fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()
}
