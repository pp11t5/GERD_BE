package com.gerd.domain.fcm

interface FcmSubscriber {
    fun subscribeToTopic(token: String, topic: String)
    fun unsubscribeFromTopic(token: String, topic: String)
}
