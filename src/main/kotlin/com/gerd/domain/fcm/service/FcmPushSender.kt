package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.UserFcmToken

interface FcmPushSender {
    fun sendToUser(userId: Long, payload: FcmPayload)
    fun send(fcmToken: UserFcmToken, payload: FcmPayload)
    fun sendRaw(token: String, payload: FcmPayload)
    fun sendMulticast(tokens: List<String>, payload: FcmPayload)
}