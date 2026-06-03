package com.gerd.domain.onboarding.service

import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.enums.ConsentType
import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.UserConsentRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 약관동의 처리 서비스
 *
 * - 필수 3종 동의 검증
 * - type별 현재 상태 upsert (재호출 시 동의 상태·시점 갱신)
 */
@Service
@Transactional(readOnly = true)
class ConsentService(
    private val userConsentRepository: UserConsentRepository,
) {

    @Transactional
    fun submitConsent(userId: Long, request: ConsentRequestDTO) {
        // 필수 3종 미동의 시 온보딩 진입 거부
        if (!request.tos || !request.privacy || !request.healthSensitive) {
            throw GeneralException(OnboardingErrorCode.REQUIRED_CONSENT_NOT_AGREED)
        }

        val now = LocalDateTime.now()
        val agreedByType = mapOf(
            ConsentType.TOS to request.tos,
            ConsentType.PRIVACY to request.privacy,
            ConsentType.HEALTH_SENSITIVE to request.healthSensitive,
            ConsentType.MARKETING to request.marketing,
        )
        val existingByCode = userConsentRepository.findByIdUserId(userId)
            .associateBy { it.id.consentType }

        val consents = agreedByType.map { (type, agreed) ->
            existingByCode[type.code]?.apply { updateAgreement(agreed, now) }
                ?: UserConsent(UserConsentId(userId, type.code), agreed, now)
        }
        userConsentRepository.saveAll(consents)
    }
}
