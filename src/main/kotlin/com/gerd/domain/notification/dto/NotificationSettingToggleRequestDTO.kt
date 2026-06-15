package com.gerd.domain.notification.dto

import com.gerd.domain.notification.entity.enums.NotificationSettingType
import jakarta.validation.constraints.NotNull

data class NotificationSettingToggleRequestDTO(
    @NotNull(message = "알림 타입은 필수입니다")
    val type: NotificationSettingType,
)
