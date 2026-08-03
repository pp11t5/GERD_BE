package com.gerd.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class RestClientConfig {

    @Bean
    fun restClient(): RestClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build(),
            ).apply { setReadTimeout(READ_TIMEOUT) },
        )
        .build()

    companion object {
        private val CONNECT_TIMEOUT = Duration.ofSeconds(2)
        private val READ_TIMEOUT = Duration.ofSeconds(5)
    }
}
