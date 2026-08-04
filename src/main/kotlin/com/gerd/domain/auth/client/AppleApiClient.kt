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
        try {
            return restClient.post()
                .uri(appleProperties.tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body<AppleTokenResponseDTO>()
                ?: throw GeneralException(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED)
        } catch (exception: RestClientResponseException) {
            val appleError = runCatching {
                exception.getResponseBodyAs(AppleErrorResponseDTO::class.java)
            }.getOrNull()
            log.warn {
                "애플 로그인 실패: status=${exception.statusCode}, error=${appleError?.error}"
            }

            throw GeneralException(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED, exception)
        } catch (exception: ResourceAccessException) {
            log.warn { "애플 로그인 타임아웃" }

            throw GeneralException(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED, exception)
        } catch (exception: RestClientException) {
            log.warn { "애플 로그인 응답 처리 실패" }

            throw GeneralException(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED, exception)
        }
    }

    fun revoke(refreshToken: String) {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("client_id", appleProperties.clientId)
            add("client_secret", appleLoginUtil.generateClientSecret())
            add("token", refreshToken)
            add("token_type_hint", "refresh_token")
        }

        try {
            restClient.post()
                .uri(appleProperties.revokeUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity()
        } catch (exception: RestClientException) {
            log.warn { "애플 연결 해제 실패" }
            throw GeneralException(AuthErrorCode.APPLE_REVOKE_FAILED, exception)
        }
    }
}
