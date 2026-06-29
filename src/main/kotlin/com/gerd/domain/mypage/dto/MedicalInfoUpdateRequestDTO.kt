package com.gerd.domain.mypage.dto

import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.mypage.validation.ValidMedicationNames
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class MedicalInfoUpdateRequestDTO(
    @field:Schema(
        description = """알레르기 코드 목록(다중). 영어 code 배열로 전달.
            milk(우유·유제품), egg(계란), wheat(밀), soy(콩·대두), peanut(땅콩),
            crustacean(갑각류), tree_nut(견과류), fish_shellfish(생선·조개류)""",
        example = "[\"milk\", \"peanut\"]",
    )
    @field:Size(max = 8, message = "알레르기는 최대 8개까지 선택할 수 있습니다.")
    val allergens: List<AllergenCode> = emptyList(),

    @field:Schema(description = "복용 중인 약 목록(자유 텍스트)", example = "[\"PPI\", \"제산제\"]")
    @field:Size(max = 10, message = "복용약은 최대 10개까지 입력할 수 있습니다.")
    @field:ValidMedicationNames
    val medications: List<String> = emptyList(),
)
