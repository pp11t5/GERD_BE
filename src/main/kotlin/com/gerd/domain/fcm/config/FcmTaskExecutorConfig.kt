package com.gerd.domain.fcm.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.util.concurrent.Executor

/**
 * FCM 비동기 발송 및 지연 스케줄러 설정
 * - fcmTaskExecutor: 이벤트 리스너 @Async 전용 스레드 풀
 * - taskScheduler: 식후 6시간 지연 푸시 예약용
 */
@Configuration
@EnableAsync
@EnableScheduling
class FcmTaskExecutorConfig {

    // FCM 비동기 발송용 스레드 풀
    @Bean(name = ["fcmTaskExecutor"])
    fun fcmTaskExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 2
        maxPoolSize = 5
        queueCapacity = 100
        setThreadNamePrefix("fcm-async-")
        initialize()
    }

    // 푸시 예약용 스케줄러 — 식후 6시간 지연 푸시 예약에 사용
    @Bean
    fun taskScheduler(): TaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 2
        setThreadNamePrefix("fcm-scheduler-")
        initialize()
    }
}
