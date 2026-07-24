package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.QFood
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory

/**
 * QueryDSL 음식 검색 구현
 *
 * - 공백 무시 매칭: 이름의 공백을 제거(replace)한 뒤 LIKE 비교 → DB의 띄어쓰기와 입력 오차 흡수
 * - 노출 범위: 공개 카탈로그(seed/curated·public) ∪ 본인 비공개 음식. soft-deleted는 Food의 @SQLRestriction이 제외
 * - 정렬: 정규화 기준 정확일치 → 접두 → 이름
 *
 */
class FoodRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : FoodRepositoryCustom {

    override fun search(normalizedQuery: String, size: Int, userId: Long): List<Food> {
        val food = QFood.food

        val normalizedName = Expressions.stringTemplate("replace({0}, ' ', '')", food.name)

        val exposure = food.source.`in`(FoodSource.SEED, FoodSource.CURATED)
            .and(food.visibility.eq(FoodVisibility.PUBLIC))
            .or(
                food.visibility.eq(FoodVisibility.PRIVATE)
                    .and(food.ownerUserId.eq(userId)),
            )

        val rank = CaseBuilder()
            .`when`(normalizedName.equalsIgnoreCase(normalizedQuery)).then(0)
            .`when`(normalizedName.startsWithIgnoreCase(normalizedQuery)).then(1)
            .otherwise(2)

        return queryFactory
            .selectFrom(food)
            .where(exposure, normalizedName.containsIgnoreCase(normalizedQuery))
            .orderBy(rank.asc(), food.name.asc())
            .limit(size.toLong())
            .fetch()
    }
}
