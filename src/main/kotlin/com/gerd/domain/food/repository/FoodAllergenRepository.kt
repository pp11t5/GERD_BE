package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.Allergen
import com.gerd.domain.food.entity.FoodAllergen
import com.gerd.domain.food.entity.id.FoodAllergenId
import com.gerd.domain.food.repository.dto.FoodTagCodeDTO
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FoodAllergenRepository : JpaRepository<FoodAllergen, FoodAllergenId> {

    // 판정 입력 조립 시 음식의 알레르겐 마스터를 한 번에 로딩 (N+1 방지)
    @Query("select fa.allergen from FoodAllergen fa where fa.id.foodId = :foodId")
    fun findAllergensByFoodId(foodId: Long): List<Allergen>

    // 대체식 후보들의 알레르겐 코드를 한 쿼리로 로딩 — 사용자별 안전 필터용 (N+1 방지)
    @Query(
        "select new com.gerd.domain.food.repository.dto.FoodTagCodeDTO(fa.id.foodId, fa.allergen.code) " +
            "from FoodAllergen fa where fa.id.foodId in :foodIds",
    )
    fun findTagCodesByFoodIdIn(foodIds: Collection<Long>): List<FoodTagCodeDTO>
}
