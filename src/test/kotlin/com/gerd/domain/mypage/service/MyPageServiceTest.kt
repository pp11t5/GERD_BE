package com.gerd.domain.mypage.service

import com.gerd.domain.auth.entity.AuthAccount
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
import com.gerd.domain.food.entity.Allergen
import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.food.repository.AllergenRepository
import com.gerd.domain.mypage.dto.MealCount
import com.gerd.domain.mypage.dto.MedicalInfoUpdateRequestDTO
import com.gerd.domain.mypage.dto.WeeklySummaryResponseDTO
import com.gerd.domain.onboarding.entity.UserMedication
import com.gerd.domain.onboarding.entity.UserProfile
import com.gerd.domain.onboarding.entity.enums.DiseaseType
import com.gerd.domain.onboarding.repository.UserAllergenRepository
import com.gerd.domain.onboarding.repository.UserMedicationRepository
import com.gerd.domain.onboarding.repository.UserProfileRepository
import com.gerd.domain.report.service.ReportService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MyPageServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userProfileRepository: UserProfileRepository

    @Mock
    private lateinit var authAccountRepository: AuthAccountRepository

    @Mock
    private lateinit var allergenRepository: AllergenRepository

    @Mock
    private lateinit var userAllergenRepository: UserAllergenRepository

    @Mock
    private lateinit var userMedicationRepository: UserMedicationRepository

    @Mock
    private lateinit var userFoodDictionaryRepository: UserFoodDictionaryRepository

    @Mock
    private lateinit var reportService: ReportService

    private val service by lazy {
        MyPageService(
            userRepository,
            userProfileRepository,
            authAccountRepository,
            allergenRepository,
            userAllergenRepository,
            userMedicationRepository,
            userFoodDictionaryRepository,
            reportService,
        )
    }

    private val userId = 1L

    @Nested
    inner class `마이페이지 요약 조회` {

        @Test
        fun `저장된 주간 요약이 있으면 프로필과 음식 히스토리를 함께 반환한다`() {
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user()))
            whenever(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile()))
            whenever(userFoodDictionaryRepository.countByUser_IdAndDictionaryType(userId, DictionaryType.SAFE))
                .thenReturn(5L)
            whenever(userFoodDictionaryRepository.countByUser_IdAndDictionaryType(userId, DictionaryType.CAUTION))
                .thenReturn(2L)
            whenever(reportService.getWeeklySummary(userId)).thenReturn(
                WeeklySummaryResponseDTO(
                    mealRecordCount = 7,
                    recentSymptomCount = 4,
                    streakCount = 3,
                    mealCount = MealCount(4, 2, 1),
                ),
            )

            val result = service.getProfileSummary(userId)

            assertThat(result.profile.nickName).isEqualTo("위장이")
            assertThat(result.profile.disease).isEqualTo(DiseaseType.GERD)
            assertThat(result.foodHistory.safeCount).isEqualTo(5)
            assertThat(result.foodHistory.cautionCount).isEqualTo(2)
            assertThat(result.weeklySummary.mealRecordCount).isEqualTo(7)
            assertThat(result.weeklySummary.mealCount.riskCount).isEqualTo(1)
        }

        @Test
        fun `저장된 주간 요약이 없으면 주간 값은 0으로 채운다`() {
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user()))
            whenever(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile()))
            whenever(userFoodDictionaryRepository.countByUser_IdAndDictionaryType(any(), any())).thenReturn(0L)
            whenever(reportService.getWeeklySummary(userId)).thenReturn(null)

            val result = service.getProfileSummary(userId)

            assertThat(result.weeklySummary.mealRecordCount).isZero()
            assertThat(result.weeklySummary.recentSymptomCount).isZero()
            assertThat(result.weeklySummary.streakCount).isZero()
            assertThat(result.weeklySummary.mealCount).isEqualTo(MealCount(0, 0, 0))
        }
    }

    @Nested
    inner class `프로필 상세 조회` {

        @Test
        fun `알레르기를 복용약보다 우선 대표 건강 정보로 반환한다`() {
            val profile = userProfile()
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user(email = "user@test.com")))
            whenever(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile))
            whenever(authAccountRepository.findById(userId)).thenReturn(Optional.of(authAccount()))
            whenever(userAllergenRepository.findAllergensByUserId(userId)).thenReturn(
                listOf(Allergen(code = "milk", displayName = "우유")),
            )
            whenever(userMedicationRepository.findByUserProfileUserId(userId)).thenReturn(
                listOf(UserMedication(userProfile = profile, name = "PPI")),
            )

            val result = service.getProfile(userId)

            assertThat(result.email).isEqualTo("user@test.com")
            assertThat(result.provider).isEqualTo(AuthProvider.KAKAO)
            assertThat(result.representativeInfo).isEqualTo("우유")
            assertThat(result.etcCount).isEqualTo(1)
        }
    }

    @Nested
    inner class `건강 정보 수정` {

        @Test
        fun `알레르기와 복용약을 전체 교체한다`() {
            val profile = userProfile()
            val milk = Allergen(code = "milk", displayName = "우유")
            whenever(userProfileRepository.getReferenceById(userId)).thenReturn(profile)
            whenever(allergenRepository.findByCodeIn(listOf("milk"))).thenReturn(listOf(milk))
            val request = MedicalInfoUpdateRequestDTO(
                allergens = listOf(AllergenCode.MILK),
                medications = listOf("PPI", "제산제"),
            )

            val result = service.updateHealthInfo(userId, request)

            assertThat(result.allergies).containsExactly("우유")
            assertThat(result.medications).containsExactly("PPI", "제산제")
            verify(userAllergenRepository).deleteAllByUserProfileUserId(userId)
            verify(userMedicationRepository).deleteAllByUserProfileUserId(userId)
            verify(userAllergenRepository).saveAll(any<Iterable<com.gerd.domain.onboarding.entity.UserAllergen>>())
            verify(userMedicationRepository).saveAll(any<Iterable<UserMedication>>())
        }
    }

    @Nested
    inner class `건강 정보 조회` {

        @Test
        fun `알레르기는 표시 이름 목록으로 복용약은 이름 목록으로 반환한다`() {
            val profile = userProfile()
            whenever(userAllergenRepository.findAllergensByUserId(userId)).thenReturn(
                listOf(
                    Allergen(code = "milk", displayName = "우유"),
                    Allergen(code = "peanut", displayName = "땅콩"),
                ),
            )
            whenever(userMedicationRepository.findByUserProfileUserId(userId)).thenReturn(
                listOf(
                    UserMedication(userProfile = profile, name = "PPI"),
                    UserMedication(userProfile = profile, name = "제산제"),
                ),
            )

            val result = service.getHealthInfo(userId)

            assertThat(result.allergies).containsExactly("우유", "땅콩")
            assertThat(result.medications).containsExactly("PPI", "제산제")
        }
    }

    private fun user(
        email: String = "user@test.com",
        nickname: String = "위장이",
    ) = User(
        id = userId,
        email = email,
        nickname = nickname,
    )

    private fun userProfile() = UserProfile(
        user = user(),
        diseaseType = DiseaseType.GERD,
        onboardedAt = LocalDateTime.of(2026, 6, 1, 10, 0),
    )

    private fun authAccount() = AuthAccount(
        user = user(),
        provider = AuthProvider.KAKAO,
        providerAccountId = "kakao-1",
    )
}
