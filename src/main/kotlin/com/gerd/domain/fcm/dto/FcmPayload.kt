package com.gerd.domain.fcm.dto

import com.gerd.domain.notification.entity.enums.NotificationType

data class FcmPayload(
    val title: String,
    val body: String,
    val type: NotificationType,
    val targetId: String? = null,
) {
    fun toDataMap(): Map<String, String> = buildMap {
        put("type", type.code)
        targetId?.let { put("targetId", it) }
    }
}
