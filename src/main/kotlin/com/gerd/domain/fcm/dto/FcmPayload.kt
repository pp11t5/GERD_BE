package com.gerd.domain.fcm.dto

import com.gerd.domain.notification.entity.enums.NotificationType

data class FcmPayload(
    val title: String,
    val body: String,
    val type: NotificationType,
    val targetId: String? = null,
    val mealOccurredAt: String? = null,
    val hoursElapsed: String? = null,
    val foodNames: String? = null,
) {
    fun toDataMap(): Map<String, String> = buildMap {
        put("type", type.code)
        if (type !in POST_MEAL_TYPES) {
            put("title", title)
            put("body", body)
        }
        targetId?.let { put("targetId", it) }
        mealOccurredAt?.let { put("mealOccurredAt", it) }
        hoursElapsed?.let { put("hoursElapsed", it) }
        foodNames?.let { put("foodNames", it) }
    }

    companion object {
        private val POST_MEAL_TYPES = setOf(
            NotificationType.POST_MEAL,
            NotificationType.POST_MEAL_DELAYED_SINGLE,
        )
    }
}
