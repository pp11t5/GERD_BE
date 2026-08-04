package com.gerd.domain.auth.client

import com.gerd.domain.auth.dto.JwksResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.apiPayload.GeneralException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

private val log = KotlinLogging.logger {}

/**
 * JWKS (JSON Web Key Set) 클라이언트
 * 인증 서버에서 공개 키를 가져옴
 */
@Component
class JwksClient(
    private val restClient: RestClient,
) {

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun getPublicKeys(jwksUrl: String): JwksResponseDTO {
        log.info { "JWKS 공개키 조회 시작: url=$jwksUrl" }
        try {
            val response = restClient.get()
                .uri(jwksUrl)
                .retrieve()
                .body(JwksResponseDTO::class.java)
                ?: throw GeneralException(AuthErrorCode.INVALID_TOKEN)

            log.info { "JWKS 공개키 조회 성공: url=$jwksUrl, keyCount=${response.keys.size}" }
            return response
        } catch (exception: RestClientException) {
            log.warn(exception) { "JWKS 공개키 조회 실패: url=$jwksUrl" }
            throw exception
        }
    }
}
