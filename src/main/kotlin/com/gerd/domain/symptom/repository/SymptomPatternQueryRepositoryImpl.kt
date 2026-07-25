package com.gerd.domain.symptom.repository

import com.gerd.domain.food.entity.QFood.food
import com.gerd.domain.food.entity.QFoodCategory.foodCategory
import com.gerd.domain.food.entity.QFoodCategoryMap.foodCategoryMap
import com.gerd.domain.meal.entity.QMealFood.mealFood
import com.gerd.domain.symptom.entity.QSymptom.symptom
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDateTime

/**
 * 증상 패턴 분석용 쿼리 구현체
 * - 증상 기록과 연관된 음식·카테고리 정보를 한 번에 조회
 * - 증상 패턴 분석은 최근 30일 기록만 참고하므로, 30일 이전 기록은 조회하지 않도록 쿼리에서 필터
 */
class SymptomPatternQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SymptomPatternQueryRepository {

    override fun findLinkedRows(userId: Long, since: LocalDateTime): List<SymptomMealPatternRow> =
        queryFactory
            .select(
                Projections.constructor(
                    SymptomMealPatternRow::class.java,
                    symptom.id,
                    symptom.symptomState,
                    symptom.occurredAt,
                    symptom.mealRecordId,
                    food.name,
                    foodCategory.code,
                    mealFood.judgedGrade,
                ),
            )
            .from(symptom)
            .join(mealFood).on(mealFood.mealRecord.id.eq(symptom.mealRecordId))
            .join(food).on(food.id.eq(mealFood.foodId))
            .leftJoin(foodCategoryMap).on(foodCategoryMap.food.id.eq(food.id))
            .leftJoin(foodCategoryMap.foodCategory, foodCategory)
            .where(
                symptom.user.id.eq(userId),
                symptom.occurredAt.goe(since),
            )
            .orderBy(symptom.occurredAt.desc(), symptom.id.desc())
            .fetch()
}
