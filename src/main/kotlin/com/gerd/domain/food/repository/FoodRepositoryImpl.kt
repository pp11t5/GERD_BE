package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.QFood
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

/**
 * QueryDSL 음식 검색 구현 (Spring Data fragment — FoodRepository에 자동 결합)
 *
 * - 공백 무시 매칭: 이름의 공백을 제거(replace)한 뒤 ILIKE 비교 → DB의 띄어쓰기와 입력 오차 흡수
 * - 노출 범위: 공개 카탈로그(seed/curated·public) ∪ 본인 비공개 음식. soft-deleted는 Food의 @SQLRestriction이 제외
 * - 정렬: 정규화 기준 정확일치 → 접두 → 이름. (pg_trgm은 prod 가속 인덱스일 뿐 결과 동일)
 *
 * EntityManager로 JPAQueryFactory를 직접 구성한다 — fragment가 자급식이라 repository slice 테스트가 별도 설정 없이 동작한다.
 */
@Repository
class FoodRepositoryImpl(
    @PersistenceContext private val entityManager: EntityManager,
) : FoodRepositoryCustom {

    private val queryFactory = JPAQueryFactory(entityManager)

    override fun search(normalizedQuery: String, size: Int, userId: Long): List<Food> {
        val food = QFood.food

        // replace(name, ' ', '') — H2·PostgreSQL 공통 함수. containsIgnoreCase가 lower() like로 감싼다
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
