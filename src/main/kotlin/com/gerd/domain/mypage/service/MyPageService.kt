package com.gerd.domain.mypage.service

import com.gerd.domain.auth.entity.AuthAccount
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
import com.gerd.domain.food.repository.AllergenRepository
import com.gerd.domain.mypage.dto.MealCount
import com.gerd.domain.mypage.dto.MedicalInfoResponseDTO
import com.gerd.domain.mypage.dto.MedicalInfoUpdateRequestDTO
import com.gerd.domain.mypage.dto.MyPageSummaryResponseDTO
import com.gerd.domain.mypage.dto.ProfileDetailResponseDTO
import com.gerd.domain.onboarding.entity.UserAllergen
import com.gerd.domain.onboarding.entity.UserMedication
import com.gerd.domain.onboarding.entity.UserProfile
import com.gerd.domain.onboarding.repository.UserAllergenRepository
import com.gerd.domain.onboarding.repository.UserMedicationRepository
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
    private val userMedicationRepository: UserMedicationRepository,
    private val userFoodDictionaryRepository: UserFoodDictionaryRepository,
    private val reportService: ReportService,
) {

    fun getProfileSummary(userId: Long): MyPageSummaryResponseDTO {
        val user: User = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        val userProfile: UserProfile = userProfileRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

        val safeCount = userFoodDictionaryRepository.countByUser_IdAndDictionaryType(userId, DictionaryType.SAFE)
        val cautionCount = userFoodDictionaryRepository.countByUser_IdAndDictionaryType(userId, DictionaryType.CAUTION)

        val weeklySummary = reportService.getWeeklySummary(userId)

        return MyPageSummaryResponseDTO(
            profile = MyPageSummaryResponseDTO.ProfileSummary(
                nickName = user.nickname,
                profileImage = user.profileImage,
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
                mealCount = weeklySummary?.mealCount ?: MealCount(0, 0, 0),
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
        // 알레르기, 복용약 조회해서 있는걸 알레르기->복용약 내림차순으로 조회해서 있는걸 먼저 반환, count도
        val allergens = userAllergenRepository.findAllergensByUserId(userId)
        val medications = userMedicationRepository.findByUserProfileUserId(userId)
        // 알레르기 -> 복용약 순 대표 1건 + 나머지 count
        val allItems: List<String> = allergens.map { it.displayName } + medications.map { it.name }
        val representativeInfo = allItems.firstOrNull()
        val etcCount = maxOf(0, allItems.size - 1)
        return ProfileDetailResponseDTO(
            nickName = user.nickname,
            profileImage = user.profileImage,
            email = user.email,
            provider = authAccount.provider,
            diseaseType = userProfile.diseaseType,
            representativeInfo = representativeInfo,
            etcCount = etcCount,
        )
    }

    fun getHealthInfo(userId: Long): MedicalInfoResponseDTO {
        val allergens = userAllergenRepository.findAllergensByUserId(userId)
        val medications = userMedicationRepository.findByUserProfileUserId(userId)
        return MedicalInfoResponseDTO(
            allergies = allergens.map { it.displayName },
            medications = medications.map { it.name },
        )
    }

    @Transactional
    fun updateHealthInfo(userId: Long, request: MedicalInfoUpdateRequestDTO): MedicalInfoResponseDTO {
        val userProfile = userProfileRepository.getReferenceById(userId)

        // 알레르기 전체 교체
        userAllergenRepository.deleteAllByUserProfileUserId(userId)
        val newAllergens = allergenRepository.findByCodeIn(request.allergens.map { it.code })
        userAllergenRepository.saveAll(newAllergens.map { UserAllergen(userProfile = userProfile, allergen = it) })

        // 복용약 전체 교체
        userMedicationRepository.deleteAllByUserProfileUserId(userId)
        val newMedications = request.medications.map { UserMedication(userProfile = userProfile, name = it) }
        userMedicationRepository.saveAll(newMedications)

        return MedicalInfoResponseDTO(
            allergies = newAllergens.map { it.displayName },
            medications = request.medications,
        )
    }

}
