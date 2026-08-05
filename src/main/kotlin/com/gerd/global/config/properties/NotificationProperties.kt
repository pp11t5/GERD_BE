package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalTime

@Component
@ConfigurationProperties(prefix = "notification.post-meal")
data class NotificationProperties(
    var delay: Duration = Duration.ofHours(2),
    var quietHoursStart: LocalTime = LocalTime.of(22, 0),
    var deferredTime: LocalTime = LocalTime.of(9, 0),
    var cooldown: Duration = Duration.ofMinutes(90),
)
