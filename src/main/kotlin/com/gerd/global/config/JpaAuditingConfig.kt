package com.gerd.global.config

import com.gerd.domain.auth.security.CustomUserDetails
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "koreaDateTimeProvider")
class JpaAuditingConfig {

    // JVM timezone 설정과 무관하게 항상 KST로 감사 시각 고정
    @Bean
    fun koreaDateTimeProvider() = DateTimeProvider {
        Optional.of(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
    }

    @Bean
    fun auditorAware(): AuditorAware<Long> = AuditorAware {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.principal == "anonymousUser") {
            Optional.empty()
        } else {
            Optional.ofNullable((auth.principal as CustomUserDetails).userId)
        }
    }
}
