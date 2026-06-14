package com.gerd.domain.notification.service

import com.gerd.domain.notification.entity.enums.DailyNotificationTime
import org.springframework.stereotype.Service

/**
 * 알림 단일 진입점 — 트리거(크론/리스너)는 이 Facade만 주입받는다.
 * 실제 로직은 유스케이스별 서비스에 위임 (책임 분리 유지).
 */
@Service
class NotificationFacade(
    private val postMeal: PostMealNotificationUseCase,
    private val dailyRecord: DailyRecordNotificationUseCase,
    private val weeklyReport: WeeklyReportNotificationUseCase,
) {

    // 식후 알림 예약 (식사 기록 커밋 후 리스너가 호출)
    fun enqueuePostMeal(userId: Long, mealRecordId: Long) = postMeal.enqueue(userId, mealRecordId)

    // 식후 PENDING 발송 (매분)
    fun processPostMealPending() = postMeal.processPending()

    // 매일 기록 푸시 (시간대별)
    fun sendDailyRecord(time: DailyNotificationTime) = dailyRecord.send(time)

    // 주간 리포트 (일요일)
    fun sendWeeklyReport() = weeklyReport.send()
}
