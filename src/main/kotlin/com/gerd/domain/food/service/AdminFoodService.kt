package com.gerd.domain.food.service

import com.gerd.domain.food.dto.AdminUserFoodDTO
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.repository.UserFoodRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.common.response.PageResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class AdminFoodService(
    private val foodRepository: FoodRepository,
    private val userFoodRepository: UserFoodRepository,
) {

    @Transactional(readOnly = true)
    fun getAllUserFoods(page: Int, isUnknown: Boolean?): PageResponse<AdminUserFoodDTO> {
        val pageable = PageRequest.of(page, PAGE_SIZE)
        val result = if (isUnknown != null) {
            userFoodRepository.findAllWithFoodByIsUnknown(isUnknown, pageable)
        } else {
            userFoodRepository.findAllWithFood(pageable)
        }
        return PageResponse.of(result) {
            AdminUserFoodDTO(externalId = it.food.externalId.toString(), name = it.food.name)
        }
    }

    @Transactional
    fun promote(foodExternalId: String) {
        val uuid = runCatching { UUID.fromString(foodExternalId) }.getOrElse {
            throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        }
        val food = foodRepository.findByExternalId(uuid)
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)

        val foodId = requireNotNull(food.id)
        userFoodRepository.deleteAllByFoodId(foodId)
        food.promote()
        log.info { "음식 승격 완료: foodId=$foodId name=${food.name} → CURATED/PUBLIC" }
    }

    companion object {
        const val PAGE_SIZE = 100
    }
}
