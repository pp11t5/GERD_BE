package com.gerd.domain.mypage.service

import com.gerd.domain.auth.entity.AuthAccount
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.service.NicknameService
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
import com.gerd.domain.food.entity.Allergen
import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.food.repository.AllergenRepository
import com.gerd.domain.mypage.dto.MealCount
import com.gerd.domain.mypage.dto.MedicalInfoResponseDTO
import com.gerd.domain.mypage.dto.MedicalInfoUpdateRequestDTO
import com.gerd.domain.mypage.dto.MyPageSummaryResponseDTO
import com.gerd.domain.mypage.dto.NicknameUpdateRequestDTO
import com.gerd.domain.mypage.dto.ProfileDetailResponseDTO
import com.gerd.domain.onboarding.entity.UserAllergen
import com.gerd.domain.onboarding.entity.UserProfile
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.UserAllergenRepository
import com.gerd.domain.onboarding.repository.UserProfileRepository
import com.gerd.domain.report.service.ReportService
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MyPageService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val authAccountRepository: AuthAccountRepository,
    private val allergenRepository: AllergenRepository,
    private val userAllergenRepository: UserAllergenRepository,
    private val userFoodDictionaryRepository: UserFoodDictionaryRepository,
    private val reportService: ReportService,
    private val nicknameService: NicknameService,
) {

    fun getProfileSummary(userId: Long): MyPageSummaryResponseDTO {
        val user: User = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        val userProfile: UserProfile = userProfileRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        val safeCount = userFoodDictionaryRepository.countByUser_IdAndDictionaryType(userId, DictionaryType.SAFE)
        val cautionCount = userFoodDictionaryRepository.countByUser_IdAndDictionaryTypeIn(
            userId, listOf(DictionaryType.CAUTION, DictionaryType.RISK),
        )

        val weeklySummary = reportService.getWeeklySummary(userId)

        return MyPageSummaryResponseDTO(
            profile = MyPageSummaryResponseDTO.ProfileSummary(
                nickName = user.nickname,
                disease = userProfile.diseaseType,
            ),
            foodHistory = MyPageSummaryResponseDTO.FoodHistory(
                safeCount = safeCount.toInt(),
                cautionCount = cautionCount.toInt(),
            ),
            weeklySummary = MyPageSummaryResponseDTO.WeeklySummary(
                mealRecordCount = weeklySummary?.mealRecordCount ?: 0,
                recentSymptomCount = weeklySummary?.recentSymptomCount ?: 0,
                streakCount = weeklySummary?.streakCount ?: 0,
                mealCount = weeklySummary?.mealCount ?: MealCount(0, 0, 0, 0),
            ),
        )
    }

    fun getProfile(userId: Long): ProfileDetailResponseDTO {
        val user: User = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        val userProfile: UserProfile = userProfileRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        val authAccount: AuthAccount = authAccountRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        // 알레르기 대표 1건 + 나머지 count
        val allergens = userAllergenRepository.findAllergensByUserId(userId)
        val allergenNames = allergens.map { it.displayName }
        val representativeInfo = allergenNames.firstOrNull()
        val etcCount = maxOf(0, allergenNames.size - 1)
        return ProfileDetailResponseDTO(
            nickName = user.nickname,
            provider = authAccount.provider,
            diseaseType = userProfile.diseaseType,
            representativeInfo = representativeInfo,
            etcCount = etcCount,
        )
    }

    @Transactional
    fun updateNickname(userId: Long, request: NicknameUpdateRequestDTO) {
        nicknameService.changeNickname(userId, request.nickname)
    }

    fun getHealthInfo(userId: Long): MedicalInfoResponseDTO {
        val allergens = userAllergenRepository.findAllergensByUserId(userId)
        return MedicalInfoResponseDTO(
            allergies = allergens.map { it.displayName },
        )
    }

    @Transactional
    fun updateHealthInfo(userId: Long, request: MedicalInfoUpdateRequestDTO): MedicalInfoResponseDTO {
        if (!userProfileRepository.existsById(userId)) {
            throw GeneralException(AuthErrorCode.USER_NOT_FOUND)
        }

        val newAllergens = resolveAllergens(request.allergens)
        val userProfile = userProfileRepository.getReferenceById(userId)

        // 알레르기 전체 교체
        userAllergenRepository.deleteAllByUserProfileUserId(userId)
        userAllergenRepository.saveAll(newAllergens.map { UserAllergen(userProfile = userProfile, allergen = it) })

        return MedicalInfoResponseDTO(
            allergies = newAllergens.map { it.displayName },
        )
    }

    private fun resolveAllergens(codes: List<AllergenCode>): List<Allergen> {
        if (codes.isEmpty()) return emptyList()
        val distinctCodes = codes.map { it.code }.distinct()
        val allergens = allergenRepository.findByCodeIn(distinctCodes)
        if (allergens.size < distinctCodes.size) {
            throw GeneralException(OnboardingErrorCode.INVALID_ALLERGEN)
        }
        return allergens
    }

}
