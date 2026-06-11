package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.FoodTrigger
import com.gerd.domain.food.entity.TriggerLabel
import com.gerd.domain.food.entity.id.FoodTriggerId
import com.gerd.domain.food.repository.dto.FoodTagCodeDTO
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FoodTriggerRepository : JpaRepository<FoodTrigger, FoodTriggerId> {

    // 판정 입력 조립 시 음식의 트리거 마스터를 한 번에 로딩 (N+1 방지)
    @Query("select ft.triggerLabel from FoodTrigger ft where ft.id.foodId = :foodId")
    fun findTriggerLabelsByFoodId(foodId: Long): List<TriggerLabel>

    // 대체식 후보들의 트리거 코드를 한 쿼리로 로딩 — 사용자별 안전 필터용 (N+1 방지)
    @Query(
        "select new com.gerd.domain.food.repository.dto.FoodTagCodeDTO(ft.id.foodId, ft.triggerLabel.code) " +
            "from FoodTrigger ft where ft.id.foodId in :foodIds",
    )
    fun findTagCodesByFoodIdIn(foodIds: Collection<Long>): List<FoodTagCodeDTO>
}
