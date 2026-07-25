package com.gerd.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.util.concurrent.Executor

@EnableAsync
@Configuration
class AsyncConfig {

    @Bean(name = ["analysisTaskExecutor"])
    fun analysisTaskExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 2
        maxPoolSize = 4
        queueCapacity = 50
        setThreadNamePrefix("analysis-async-")
        initialize()
    }

    @Bean(name = ["withdrawTaskScheduler"])
    fun withdrawTaskScheduler(): TaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 1
        setThreadNamePrefix("withdraw-scheduler-")
        initialize()
    }
}
