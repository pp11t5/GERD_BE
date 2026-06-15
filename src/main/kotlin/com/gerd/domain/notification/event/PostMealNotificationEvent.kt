package com.gerd.domain.notification.event

// 식사 기록 저장 커밋 후 발행 — 식후 알림 예약 트리거
data class PostMealNotificationEvent(
    val userId: Long,
    val mealRecordId: Long,
)
