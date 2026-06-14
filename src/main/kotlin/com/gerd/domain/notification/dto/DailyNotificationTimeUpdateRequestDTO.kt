package com.gerd.domain.notification.dto

import com.gerd.domain.notification.entity.enums.DailyNotificationTime
import jakarta.validation.constraints.NotNull

data class DailyNotificationTimeUpdateRequestDTO(
    @NotNull(message = "알림 시간대는 필수입니다")
    val time: DailyNotificationTime,
)
