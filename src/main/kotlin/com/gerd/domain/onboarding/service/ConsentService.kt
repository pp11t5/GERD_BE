package com.gerd.domain.onboarding.service

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
) {

    fun getTerms(): List<Term> = termRepository.findLatestAll()

    fun isMarketingAgreed(userId: Long): Boolean =
        userConsentRepository.findLatestByUserIdAndTermCode(userId, "marketing")?.agreed ?: false

    @Transactional
    fun toggleMarketing(userId: Long): Boolean {
        val consent = userConsentRepository.findLatestByUserIdAndTermCode(userId, "marketing")
            ?: throw GeneralException(OnboardingErrorCode.MARKETING_CONSENT_NOT_FOUND)
        consent.updateAgreement(!consent.agreed, LocalDateTime.now())
        applyMarketingToNotificationSetting(userId, consent.agreed)
        return consent.agreed
    }

    @Transactional
    fun submitConsent(userId: Long, request: ConsentRequestDTO) {
        val latestTerms = termRepository.findLatestAll()
        val agreedByTermId = request.consents.associateBy { it.termId }

        val allRequiredAgreed = latestTerms
            .filter { it.required }
            .all { agreedByTermId[it.id]?.agreed == true }
        if (!allRequiredAgreed) {
            throw GeneralException(OnboardingErrorCode.REQUIRED_CONSENT_NOT_AGREED)
        }

        val marketingTerm = latestTerms.firstOrNull { it.code == "marketing" }
        val marketingAgreed = marketingTerm?.let { agreedByTermId[it.id]?.agreed } ?: false
        applyMarketingToNotificationSetting(userId, marketingAgreed)

        val now = LocalDateTime.now()
        val existingByTermId = userConsentRepository.findByIdUserId(userId)
            .associateBy { it.id.termId }

        val consents = latestTerms.map { term ->
            val agreed = agreedByTermId[term.id]?.agreed ?: false
            existingByTermId[term.id]?.apply { updateAgreement(agreed, now) }
                ?: UserConsent(UserConsentId(userId, term.id), term, agreed, now)
        }
        userConsentRepository.saveAll(consents)
    }

    private fun applyMarketingToNotificationSetting(userId: Long, enabled: Boolean) {
        userNotificationSettingRepository.findById(userId).ifPresent { setting ->
            setting.update(
                postMealNotificationEnabled = enabled,
                dailyRecordNotificationEnabled = enabled,
                dailyNotificationTime = setting.dailyNotificationTime,
                weeklyReportEnabled = enabled,
            )
        }
    }
}
