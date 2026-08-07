package com.gerd.domain.food.service

import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
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
    private val dictionaryRepository: UserFoodDictionaryRepository,
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
        mergeDuplicateUserFoods(food.name, foodId)

        userFoodRepository.deleteAllByFoodId(foodId)
        food.promote()
        log.info { "음식 승격 완료: foodId=$foodId name=${food.name} → CURATED/PUBLIC" }
    }

    // 동일 이름의 다른 유저 소유 USER 음식을 승격 음식으로 흡수 — 도감 등재 이전 후 정리, 원본은 소프트 삭제
    private fun mergeDuplicateUserFoods(name: String, promotedFoodId: Long) {
        val duplicates = foodRepository.findByNameAndSourceAndIdNot(name, FoodSource.USER, promotedFoodId)
        duplicates.forEach { duplicate ->
            val duplicateId = requireNotNull(duplicate.id)
            dictionaryRepository.migrateFoodId(duplicateId, promotedFoodId)
            dictionaryRepository.deleteAllByFoodId(duplicateId)
            userFoodRepository.deleteAllByFoodId(duplicateId)
            foodRepository.delete(duplicate)
            log.info { "중복 유저 음식 병합: foodId=$duplicateId name=$name → foodId=$promotedFoodId" }
        }
    }

    companion object {
        const val PAGE_SIZE = 100
    }
}
