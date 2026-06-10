package com.gerd.domain.fcm

import com.gerd.domain.fcm.dto.FcmPayload

interface FcmPushSender {
    fun sendToUser(userId: Long, payload: FcmPayload)
    fun sendRaw(token: String, payload: FcmPayload)
    fun sendToTopic(topic: String, payload: FcmPayload)
}
