package com.gerd.global.config

import com.gerd.global.security.CustomUserDetails
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@Configuration
@EnableJpaAuditing
class JpaAuditingConfig {

    @Bean
    fun auditorAware(): AuditorAware<Long> = AuditorAware {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.principal == "anonymousUser") {
            Optional.empty()
        } else {
            Optional.ofNullable((auth.principal as CustomUserDetails).user.id)
        }
    }
}
