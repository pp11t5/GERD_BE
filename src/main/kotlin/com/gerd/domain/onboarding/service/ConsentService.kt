package com.gerd.domain.onboarding.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.entity.Term
import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.TermRepository
import com.gerd.domain.onboarding.repository.UserConsentRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ConsentService(
    private val userConsentRepository: UserConsentRepository,
    private val termRepository: TermRepository,
    private val userNotificationSettingRepository: UserNotificationSettingRepository,
    private val userRepository: UserRepository,
) {

    fun getTerms(): List<Term> = termRepository.findLatestAll()

    fun isMarketingAgreed(userId: Long): Boolean =
        userConsentRepository.findLatestByUserIdAndTermCode(userId, "marketing")?.agreed ?: false

    @Transactional
    fun toggleMarketing(userId: Long): Boolean {
        val consent = userConsentRepository.findLatestByUserIdAndTermCode(userId, "marketing")
            ?: throw GeneralException(OnboardingErrorCode.MARKETING_CONSENT_NOT_FOUND)
        consent.updateAgreement(!consent.agreed, LocalDateTime.now())
        return consent.agreed
    }

    @Transactional
    fun submitConsent(userId: Long, request: ConsentRequestDTO) {
        val termIds = request.consents.map { it.termId }
        val terms = termRepository.findAllById(termIds)
        val agreedByTermId = request.consents.associateBy { it.termId }

        val allRequiredAgreed = terms
            .filter { it.required }
            .all { agreedByTermId[it.id]?.agreed == true }
        if (!allRequiredAgreed) {
            throw GeneralException(OnboardingErrorCode.REQUIRED_CONSENT_NOT_AGREED)
        }

        // 알림 설정은 마케팅 동의와 무관하게 기본값으로 생성 — 마스킹은 조회 시 처리
        if (userNotificationSettingRepository.findById(userId).isEmpty) {
            userNotificationSettingRepository.save(
                UserNotificationSetting(user = userRepository.getReferenceById(userId)),
            )
        }

        val now = LocalDateTime.now()
        val existingByTermId = userConsentRepository.findByIdUserId(userId)
            .associateBy { it.id.termId }

        val consents = terms.map { term ->
            val agreed = agreedByTermId[term.id]?.agreed ?: false
            existingByTermId[term.id]?.apply { updateAgreement(agreed, now) }
                ?: UserConsent(UserConsentId(userId, term.id), term, agreed, now)
        }
        userConsentRepository.saveAll(consents)
    }
}
