package com.gerd.domain.food.repository.dto

// (foodId, 태그 code) 평면 projection — 여러 음식의 트리거/알레르겐 코드를 한 쿼리로 묶어 읽을 때 사용
data class FoodTagCodeDTO(
    val foodId: Long,
    val code: String,
)
