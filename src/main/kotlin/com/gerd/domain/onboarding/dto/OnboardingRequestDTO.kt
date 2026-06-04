package com.gerd.domain.onboarding.dto

import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.food.entity.enums.TriggerCode
import com.gerd.domain.onboarding.entity.enums.SymptomCode
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/**
 * 온보딩 4단계(05~08) 일괄 제출 요청
 *
 * 트리거/알레르기/증상은 한글 라벨이 아니라 영어 code(enum)로 전달한다 — 허용값은 각 필드 enum 스키마 참고
 */
data class OnboardingRequestDTO(
    @field:Schema(
        description = """06 증상 체크리스트(다중, 0개 이상). 영어 code 배열로 전달.
            heartburn_reflux(속쓰림·신물), post_meal_cough(식후 기침), throat_globus(목 이물감),
            sour_mouth_odor(입 신맛·악취), supine_chest_tight(누우면 가슴 답답), none_but_manage(불편 없지만 관리)""",
        example = "[\"heartburn_reflux\", \"post_meal_cough\"]",
    )
    @field:Size(max = 6, message = "증상은 최대 6개까지 선택할 수 있습니다.")
    val symptoms: List<SymptomCode> = emptyList(),

    @field:Schema(
        description = """07 트리거 음식 칩(다중). 영어 code 배열로 전달.
            caffeine(커피·카페인), carbonated(탄산음료), alcohol(술), spicy(매운 음식), fried_fatty(튀김·기름진 음식),
            chocolate(초콜릿), citrus(감귤류), tomato(토마토), mint(민트), onion_garlic_raw(양파·마늘 생),
            cheese_dairy(치즈·유제품), refined_flour(빵·정제 밀가루)""",
        example = "[\"caffeine\", \"carbonated\", \"spicy\"]",
    )
    @field:Size(max = 12, message = "트리거 음식은 최대 12개까지 선택할 수 있습니다.")
    val triggers: List<TriggerCode> = emptyList(),

    @field:Schema(
        description = """08 알레르기 칩(다중). 영어 code 배열로 전달.
            milk(우유·유제품), egg(계란), wheat(밀), soy(콩·대두), peanut(땅콩),
            crustacean(갑각류), tree_nut(견과류), fish_shellfish(생선·조개류)""",
        example = "[\"milk\", \"peanut\"]",
    )
    @field:Size(max = 8, message = "알레르기는 최대 8개까지 선택할 수 있습니다.")
    val allergens: List<AllergenCode> = emptyList(),

    @field:Schema(description = "08 복용약 다건(각 1행, 자유 텍스트)", example = "[\"PPI\", \"제산제\"]")
    @field:Size(max = 10, message = "복용약은 최대 10개까지 입력할 수 있습니다.")
    val medications: List<@Size(max = 100, message = "복용약 이름은 최대 100자까지 입력할 수 있습니다.") String> = emptyList(),

    @field:Schema(description = "07 '해당 음식 없나요?' 자유입력 원문(nullable)", example = "오렌지주스, 라면")
    @field:Size(max = 255, message = "트리거 자유입력은 최대 255자까지 입력할 수 있습니다.")
    val customTriggerText: String? = null,
)
