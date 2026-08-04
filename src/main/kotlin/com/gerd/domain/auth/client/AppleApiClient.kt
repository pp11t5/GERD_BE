package com.gerd.domain.auth.client

import com.gerd.domain.auth.dto.AppleErrorResponseDTO
import com.gerd.domain.auth.dto.AppleTokenResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.util.AppleLoginUtil
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.config.properties.AppleProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

private val log = KotlinLogging.logger {}

@Component
class AppleApiClient(
    private val appleProperties: AppleProperties,
    private val restClient: RestClient,
    private val appleLoginUtil: AppleLoginUtil,
) {
    fun issueToken(
        code: String,
        clientSecret: String,
    ): AppleTokenResponseDTO {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("client_id", appleProperties.clientId)
            add("client_secret", clientSecret)
            add("code", code)
            add("grant_type", "authorization_code")
        }
        log.info { "애플 토큰 발급 요청: code=${mask(code)}" }
        try {
            val token = restClient.post()
                .uri(appleProperties.tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body<AppleTokenResponseDTO>()
                ?: throw GeneralException(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED)

            log.info { "애플 토큰 발급 성공: accessToken=${mask(token.accessToken)}" }
            return token
        } catch (exception: RestClientResponseException) {
            val appleError = runCatching {
                exception.getResponseBodyAs(AppleErrorResponseDTO::class.java)
            }.getOrNull()
            log.warn(exception) {
                "애플 로그인 실패: status=${exception.statusCode}, error=${appleError?.error}"
            }

            throw GeneralException(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED)
        } catch (exception: ResourceAccessException) {
            log.warn(exception) { "애플 로그인 타임아웃" }

            throw GeneralException(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED)
        } catch (exception: RestClientException) {
            log.warn(exception) { "애플 로그인 응답 처리 실패" }

            throw GeneralException(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED)
        }
    }

    fun revoke(refreshToken: String) {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("client_id", appleProperties.clientId)
            add("client_secret", appleLoginUtil.generateClientSecret())
            add("token", refreshToken)
            add("token_type_hint", "refresh_token")
        }

        log.info { "애플 연결 해제 요청: token=${mask(refreshToken)}" }
        try {
            restClient.post()
                .uri(appleProperties.revokeUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity()

            log.info { "애플 연결 해제 성공" }
        } catch (exception: RestClientException) {
            log.warn(exception) { "애플 연결 해제 실패" }
            throw GeneralException(AuthErrorCode.APPLE_REVOKE_FAILED)
        }
    }

    // 로그에 원문 노출되지 않도록 앞 4자만 남기고 마스킹
    private fun mask(value: String): String =
        if (value.length <= 4) "****" else "${value.take(4)}****"
}
