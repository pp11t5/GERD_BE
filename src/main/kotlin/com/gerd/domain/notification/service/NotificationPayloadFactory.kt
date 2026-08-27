package com.gerd.domain.notification.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.notification.entity.enums.NotificationType

/**
 * NotificationType별 실제 발송 문구를 한 곳에서 관리
 * - 각 UseCase의 실발송과 관리자 테스트 발송(AdminNotificationTestService)이 동일 문구를 공유
 */
object NotificationPayloadFactory {

    // bulkCount: POST_MEAL_DELAYED_BULK 전용 — 실제 미기록 건수(테스트 발송 시엔 예시값)
    // 식사 기록 화면으로 이동하는 단건 알림은 식사 컨텍스트를 함께 전송
    fun of(
        type: NotificationType,
        targetId: String? = null,
        bulkCount: Int = 3,
        mealOccurredAt: String? = null,
        hoursElapsed: String? = null,
        foodNames: String? = null,
    ): FcmPayload = when (type) {
        NotificationType.POST_MEAL -> FcmPayload(
            title = "속은 좀 어떠세요?",
            body = "방금 드신 식사, 속은 좀 어떠세요? 지금 증상을 기록해 보세요.",
            type = type,
            targetId = targetId,
            mealOccurredAt = mealOccurredAt,
            hoursElapsed = hoursElapsed,
            foodNames = foodNames,
        )

        NotificationType.POST_MEAL_DELAYED_SINGLE -> FcmPayload(
            title = "어젯밤 식사, 기록하셨나요?",
            body = "어젯밤 드신 식사, 속은 좀 어떠셨어요? 잊기 전에 기록해 보세요.",
            type = type,
            targetId = targetId,
            mealOccurredAt = mealOccurredAt,
            hoursElapsed = hoursElapsed,
            foodNames = foodNames,
        )

        NotificationType.POST_MEAL_DELAYED_BULK -> FcmPayload(
            title = "미기록 식사가 있어요",
            body = "어젯밤 식사 ${bulkCount}건의 증상 기록이 남아 있어요. 잊기 전에 확인해 보세요.",
            type = type,
        )

        NotificationType.DAILY_RECORD -> FcmPayload(
            title = "오늘 식사 기록을 남겨보세요",
            body = "오늘 하루 드신 식사를 기록하면 증상 패턴을 파악할 수 있어요.",
            type = type,
        )

        NotificationType.WEEKLY_REPORT -> FcmPayload(
            title = "이번 주 리포트가 준비됐어요",
            body = "불편했던 음식과 신호등 분포를 확인해 보세요.",
            type = type,
        )
    }
}
