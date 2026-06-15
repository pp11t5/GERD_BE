package com.gerd.domain.notification.listener

import com.gerd.domain.notification.event.PostMealNotificationEvent
import com.gerd.domain.notification.service.NotificationFacade
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 식사 기록 커밋 후 식후 알림 예약
 * - 식사 기록 트랜잭션 커밋 후에만 예약
 */
@Component
class PostMealNotificationListener(
    private val notificationFacade: NotificationFacade,
) {

    @Async("fcmTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onPostMeal(event: PostMealNotificationEvent) {
        notificationFacade.enqueuePostMeal(event.userId, event.mealRecordId)
    }
}
