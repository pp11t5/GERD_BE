package com.gerd.domain.fcm.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.gerd.global.config.properties.FirebaseProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.ByteArrayInputStream

private val log = KotlinLogging.logger {}

/**
 * FirebaseApp, FirebaseMessaging 빈 초기화 (prod 전용)
 */
@Configuration
@Profile("prod")
class FirebaseConfig(
    private val firebaseProperties: FirebaseProperties,
) {

    @Bean
    fun firebaseApp(): FirebaseApp {
        // 이미 초기화된 앱이 있으면 재사용 — 중복 초기화 방지
        if (FirebaseApp.getApps().isNotEmpty()) return FirebaseApp.getInstance()

        log.info { "Initializing Firebase app" }

        val serviceAccount = ByteArrayInputStream(
            firebaseProperties.serviceAccountJson.toByteArray(Charsets.UTF_8)
        )
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging =
        FirebaseMessaging.getInstance(firebaseApp)
}
