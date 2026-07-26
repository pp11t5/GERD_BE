package com.gerd.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@EnableAsync
@Configuration
class AsyncConfig {

    @Bean(name = ["withdrawTaskScheduler"])
    fun withdrawTaskScheduler(): TaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 1
        setThreadNamePrefix("withdraw-scheduler-")
        initialize()
    }
}
