package com.gerd.domain.fcm.service

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.fcm.dto.FcmTokenRegisterRequestDTO
import com.gerd.domain.fcm.entity.UserFcmToken
import com.gerd.domain.fcm.exception.FcmErrorCode
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * FCM 토큰 등록·삭제·만료 처리
 * - 등록: user_id 기준 upsert
 * - 삭제: 로그아웃·탈퇴·발송 실패 시 호출
 */
@Service
@Transactional
class FcmTokenService(
    private val userRepository: UserRepository,
    private val userFcmTokenRepository: UserFcmTokenRepository,
) {

    // 기존 토큰이 있으면 갱신, 없으면 신규 등록 (upsert 방식)
    fun register(userId: Long, request: FcmTokenRegisterRequestDTO) {
        // 디바이스 토큰은 다른 계정에 남아 있으면 안 된다. 계정 전환 시 기존 소유 행을 정리한다.
        userFcmTokenRepository.findAllByToken(request.token)
            .filter { it.userId != userId }
            .takeIf { it.isNotEmpty() }
            ?.let { userFcmTokenRepository.deleteAll(it) }

        val existing = userFcmTokenRepository.findById(userId).orElse(null)

        if (existing != null) {
            existing.updateToken(request.token, request.platform)
        } else {
            val user = userRepository.findById(userId)
                .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
            userFcmTokenRepository.save(
                UserFcmToken(user = user, platform = request.platform, token = request.token)
            )
        }
    }

    // 로그아웃·탈퇴 시 호출 — 유저 토큰 제거
    fun delete(userId: Long) {
        val token = userFcmTokenRepository.findById(userId)
            .orElseThrow { GeneralException(FcmErrorCode.FCM_TOKEN_NOT_FOUND) }
        userFcmTokenRepository.delete(token)
    }

    // FCM 발송 실패(UNREGISTERED) 시 만료 토큰 제거
    fun deleteByToken(token: String) {
        userFcmTokenRepository.findAllByToken(token)
            .takeIf { it.isNotEmpty() }
            ?.let { userFcmTokenRepository.deleteAll(it) }
    }
}
