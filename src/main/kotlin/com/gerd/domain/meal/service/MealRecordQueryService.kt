package com.gerd.domain.meal.service

import com.gerd.domain.meal.dto.MealGroupDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 식사 기록 단건 조회 / 타임라인 날짜별 조회
 *
 * - 단건: 본인 소유분만, 삭제된 기록은 404
 * - 목록: 날짜(Asia/Seoul 경계) 1일치를 끼니 단위로 그룹핑
 */
@Service
@Transactional(readOnly = true)
class MealRecordQueryService(
    private val mealRecordRepository: MealRecordRepository,
    private val mealRecordAssembler: MealRecordAssembler,
) {

    fun getDetail(mealId: String, userId: Long): MealRecordDetailDTO {
        val externalId = mealRecordAssembler.parseUuid(mealId)
            ?: throw GeneralException(MealErrorCode.MEAL_NOT_FOUND)
        val record = mealRecordRepository.findByExternalIdAndUser_Id(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_NOT_FOUND)
        return mealRecordAssembler.toDetail(record)
    }

    fun getDaily(date: String?, userId: Long): List<MealGroupDTO> {
        val day = mealRecordAssembler.parseDate(date)
        val (from, to) = mealRecordAssembler.toDayRange(day)
        val records = mealRecordRepository.findDailyRecords(userId, from, to)
        val summaries = mealRecordAssembler.toSummaries(records)
        return mealRecordAssembler.toGroups(summaries)
    }
}
