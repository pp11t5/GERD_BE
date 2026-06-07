package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.FcmPushSender
import com.gerd.domain.fcm.FcmSubscriber
import com.gerd.domain.fcm.dto.FcmPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * local/test 프로필용 NoOp 구현체 — Firebase 없이 컨텍스트 로드 가능
 */
@Service
@Profile("!prod")
class FcmNoOpClient : FcmPushSender, FcmSubscriber {

    override fun sendToUser(userId: Long, payload: FcmPayload) {
        log.debug { "[NoOp] sendToUser: userId=$userId, type=${payload.type}" }
    }

    override fun sendRaw(token: String, payload: FcmPayload) {
        log.debug { "[NoOp] sendRaw: token=$token" }
    }

    override fun sendToTopic(topic: String, payload: FcmPayload) {
        log.debug { "[NoOp] sendToTopic: topic=$topic" }
    }

    override fun subscribeToTopic(token: String, topic: String) {
        log.debug { "[NoOp] subscribeToTopic: topic=$topic" }
    }

    override fun unsubscribeFromTopic(token: String, topic: String) {
        log.debug { "[NoOp] unsubscribeFromTopic: topic=$topic" }
    }
}
