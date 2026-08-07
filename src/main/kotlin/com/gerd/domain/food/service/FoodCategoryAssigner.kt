package com.gerd.domain.food.service

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.FoodCategoryMap
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import com.gerd.domain.food.repository.FoodCategoryRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

// LLM이 판정 중 분류한 카테고리를 미분류 음식에 등록 — 이미 분류된 음식인지 여부는 호출부 책임
@Component
class FoodCategoryAssigner(
    private val foodCategoryRepository: FoodCategoryRepository,
    private val foodCategoryMapRepository: FoodCategoryMapRepository,
) {

    fun assignIfPresent(food: Food, categoryCode: String?) {
        if (categoryCode == null) return
        val category = foodCategoryRepository.findByCode(categoryCode) ?: run {
            log.warn { "LLM이 알 수 없는 카테고리 code를 반환함: foodId=${food.id} categoryCode=$categoryCode" }
            return
        }
        runCatching {
            foodCategoryMapRepository.save(FoodCategoryMap(food = food, foodCategory = category))
        }.onFailure { e ->
            if (e !is DataIntegrityViolationException) throw e
        }
    }
}
