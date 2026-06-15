package com.gerd.domain.notification.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.UserFcmToken
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.domain.fcm.service.FcmPushSender
import com.gerd.domain.notification.entity.NotificationPending
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.PENDING
import com.gerd.domain.notification.entity.enums.NotificationSettingType
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.domain.notification.repository.NotificationPendingRepository
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * 식후 PENDING을 유저 단위로 발송 — 별도 스레드·트랜잭션
 * - 배치 전체가 단일 트랜잭션으로 FCM I/O 시간만큼 커넥션을 점유하는 문제를 분리
 * - REQUIRES_NEW로 유저별 독립 커밋, @Async로 발송 병렬화
 * - 실패 시 트랜잭션 롤백 → PENDING 유지 → 다음 크론에서 자연 재시도
 */
@Service
class PostMealPendingSender(
    private val notificationPendingRepository: NotificationPendingRepository,
    private val userNotificationSettingRepository: UserNotificationSettingRepository,
    private val userFcmTokenRepository: UserFcmTokenRepository,
    private val fcmPushSender: FcmPushSender,
) {

    // 호출 즉시 반환, 별도 스레드/트랜잭션에서 실행
    @Async("fcmTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun sendForUser(userId: Long, pendingIds: List<Long>) {
        // PENDING만 재조회 — managed 엔티티 확보 + 이미 처리된 건 스킵(멱등)
        val pendings = notificationPendingRepository.findByIdInAndStatus(pendingIds, PENDING)
        if (pendings.isEmpty()) return

        if (!checkNotificationEnabled(userId, NotificationSettingType.POST_MEAL)) {
            log.info { "알림 비활성 유저 스킵: userId=$userId" }
            pendings.forEach { it.cancel() }
            return
        }
        // 토큰 1회 조회 → 존재 확인 + 발송에 재사용
        val fcmToken = userFcmTokenRepository.findById(userId).orElse(null)
        if (fcmToken == null) {
            log.warn { "[FCM] FCM 토큰이 없는 유저의 PENDING 발견. userId=$userId" }
            pendings.forEach { it.cancel() }
            return
        }
        sendByPendingGroup(fcmToken, pendings)
    }

    // 알림 설정 활성 여부 확인 — 설정 row 없으면(푸시 미동의) 미발송
    private fun checkNotificationEnabled(userId: Long, type: NotificationSettingType): Boolean {
        val setting = userNotificationSettingRepository.findById(userId).orElse(null)
            ?: return false
        return setting.isEnabled(type)
    }

    // PENDING 건수 · delayed 여부로 발송 타입 결정 후 발송 (managed 엔티티 → 더티체킹)
    // - 1건 delayed=false → POST_MEAL
    // - 1건 delayed=true  → POST_MEAL_DELAYED_SINGLE
    // - N건              → POST_MEAL_DELAYED_BULK
    private fun sendByPendingGroup(fcmToken: UserFcmToken, pendings: List<NotificationPending>) {
        val payload = when {
            // 이연 묶음 — 일반 푸시, 미기록 식사 목록 진입
            pendings.size > 1 -> FcmPayload(
                title = "미기록 식사가 있어요",
                body = "어젯밤 식사 ${pendings.size}건의 증상 기록이 남아 있어요. 잊기 전에 확인해 보세요.",
                type = NotificationType.POST_MEAL_DELAYED_BULK,
            )
            // 이연 단건 — 리치 푸시, 과거형 카피
            pendings.first().delayed -> FcmPayload(
                title = "어젯밤 식사, 기록하셨나요?",
                body = "어젯밤 드신 식사, 속은 좀 어떠셨어요? 잊기 전에 기록해 보세요.",
                type = NotificationType.POST_MEAL_DELAYED_SINGLE,
                targetId = pendings.first().mealRecordId?.toString(),
            )
            // 낮 단건 — 리치 푸시, 바로 증상 기록
            else -> FcmPayload(
                title = "속은 좀 어떠세요?",
                body = "방금 드신 식사, 속은 좀 어떠세요? 지금 증상을 기록해 보세요.",
                type = NotificationType.POST_MEAL,
                targetId = pendings.first().mealRecordId?.toString(),
            )
        }
        fcmPushSender.send(fcmToken, payload)
        pendings.forEach { it.markSent() }
        log.info { "식후 알림 발송: userId=${fcmToken.userId}, type=${payload.type.code}, ${pendings.size}건" }
    }
}
