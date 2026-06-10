package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.FoodTrigger
import com.gerd.domain.food.entity.TriggerLabel
import com.gerd.domain.food.entity.id.FoodTriggerId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FoodTriggerRepository : JpaRepository<FoodTrigger, FoodTriggerId> {

    // 판정 입력 조립 시 음식의 트리거 마스터를 한 번에 로딩 (N+1 방지)
    @Query("select ft.triggerLabel from FoodTrigger ft where ft.id.foodId = :foodId")
    fun findTriggerLabelsByFoodId(foodId: Long): List<TriggerLabel>
}
