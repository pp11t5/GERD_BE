package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.Food

interface FoodRepositoryCustom {

    /**
     * 음식 이름 검색 (공백 무시 ILIKE)
     *
     * @param normalizedQuery 공백을 제거한 검색어 (서비스에서 전처리)
     * @param size 최대 결과 수
     * @param userId 노출 범위 판정용 (본인 비공개 음식 포함)
     */
    fun search(normalizedQuery: String, size: Int, userId: Long): List<Food>
}
