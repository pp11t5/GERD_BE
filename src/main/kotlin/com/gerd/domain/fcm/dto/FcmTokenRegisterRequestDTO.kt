package com.gerd.domain.fcm.dto

import com.gerd.domain.fcm.entity.enums.DevicePlatform
import jakarta.validation.constraints.NotBlank

data class FcmTokenRegisterRequestDTO(
    val platform: DevicePlatform,
    @field:NotBlank val token: String,
)
