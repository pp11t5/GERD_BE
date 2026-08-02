package com.gerd.domain.notification.dto

import com.gerd.domain.notification.entity.enums.NotificationType
import jakarta.validation.constraints.NotNull

data class AdminNotificationTestRequestDTO(
    @NotNull(message = "유저 ID는 필수입니다")
    val userId: Long,
    @NotNull(message = "알림 타입은 필수입니다")
    val type: NotificationType,
    val targetId: String? = null,
)
