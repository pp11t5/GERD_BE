package com.gerd.domain.notification.listener

import com.gerd.domain.fcm.FcmPushSender
import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.domain.notification.event.PostMealNotificationEvent
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Instant

/**
 * 식사 기록 커밋 후 6시간 뒤 푸시 예약
 * - TaskScheduler 인메모리 방식 — 서버 재시작 시 예약 소멸 허용
 * - TransactionalEventListener로 트랜잭션 커밋 후 이벤트 처리, 실패 시 푸시 예약 안 됨
 * - FcmPushSender로 유저별 푸시 발송, payload에 알림 유형과 타겟 ID 포함 - 사용자의 식사 기록이 될 수 있음
 */
@Component
class PostMealNotificationListener(
    private val fcmPushSender: FcmPushSender,
    private val taskScheduler: TaskScheduler,
) {

    // 커밋 후 인메모리 스케줄러로 식후 6시간 뒤 푸시 예약
    @Async("fcmTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onPostMeal(event: PostMealNotificationEvent) {
        taskScheduler.schedule(
            {
                fcmPushSender.sendToUser(
                    event.userId,
                    FcmPayload(
                        title = "식사 기록이 완료되었습니다!",
                        body = "식사 기록이 성공적으로 저장되었습니다. 6시간 뒤에 식사 후 알림이 발송됩니다.",
                        type = NotificationType.POST_MEAL,
                        targetId = event.userId.toString()
                    )
                )
            },
            Instant.now().plusSeconds(6 * 3600L)  // 지금으로부터 6시간 뒤
        )
    }
}
