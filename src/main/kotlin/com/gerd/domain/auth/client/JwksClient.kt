package com.gerd.domain.auth.client

import com.gerd.domain.auth.dto.JwksResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient

/**
 * JWKS (JSON Web Key Set) 클라이언트
 * 인증 서버에서 공개 키를 가져옴
 */
@Component
class JwksClient {

    private val restClient = RestClient.create()

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun getPublicKeys(jwksUrl: String): JwksResponseDTO =
        restClient.get()
            .uri(jwksUrl)
            .retrieve()
            .body(JwksResponseDTO::class.java)
            ?: throw GeneralException(AuthErrorCode.INVALID_TOKEN)
}
