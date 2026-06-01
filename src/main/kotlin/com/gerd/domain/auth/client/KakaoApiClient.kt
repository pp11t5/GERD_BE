package com.gerd.domain.auth.client

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.config.properties.KakaoProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

/**
 * 카카오 Admin REST API 클라이언트
 * - Admin Key 인증이 필요한 서버 사이드 API 호출 담당
 */
@Component
class KakaoApiClient(
    private val kakaoProperties: KakaoProperties,
) {

    private val restClient = RestClient.create()


    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun unlink(providerAccountId: String) {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("target_id_type", "user_id")
            add("target_id", providerAccountId)
        }

        runCatching {
            restClient.post()
                .uri("https://kapi.kakao.com/v1/user/unlink")
                .header("Authorization", "KakaoAK ${kakaoProperties.adminKey}")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity()
        }.onFailure {
            throw GeneralException(AuthErrorCode.KAKAO_UNLINK_FAILED)
        }
    }
}
