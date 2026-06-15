package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.UserFcmToken
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * local/test 프로필용 NoOp 구현체 — Firebase 없이 컨텍스트 로드 가능
 */
@Service
@Profile("!prod")
class FcmNoOpClient : FcmPushSender {

    override fun sendToUser(userId: Long, payload: FcmPayload) {
        log.debug { "[NoOp] sendToUser: userId=$userId, type=${payload.type}" }
    }

    override fun send(fcmToken: UserFcmToken, payload: FcmPayload) {
        log.debug { "[NoOp] send: token=${fcmToken.token}" }
    }

    override fun sendRaw(token: String, payload: FcmPayload) {
        log.debug { "[NoOp] sendRaw: token=$token" }
    }

    override fun sendMulticast(tokens: List<String>, payload: FcmPayload) {
        log.debug { "[NoOp] sendMulticast: ${tokens.size}건, type=${payload.type}" }
    }
}
