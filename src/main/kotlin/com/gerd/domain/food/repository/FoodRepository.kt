package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.enums.FoodSource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface FoodRepository : JpaRepository<Food, Long>, FoodRepositoryCustom {

    // 최근 본 음식 추가 시 externalId로 음식 resolve
    fun findByExternalId(externalId: UUID): Food?

    // 텍스트 기록 시 동일 이름의 USER 음식 재사용 여부 확인
    fun findByNameAndOwnerUserIdAndSource(name: String, ownerUserId: Long, source: FoodSource): Food?

    // 승격 시 병합 대상 조회 — 승격 음식을 제외한 동일 이름의 다른 유저 소유 USER 음식
    fun findByNameAndSourceAndIdNot(name: String, source: FoodSource, id: Long): List<Food>

    // 식사 기록 조회용 — 삭제된 음식도 포함 (D5: food soft-delete 후에도 기록의 음식 정보 보존)
    @Query(value = "SELECT * FROM foods WHERE food_id IN (:ids)", nativeQuery = true)
    fun findAllByIdsIncludingDeleted(@Param("ids") ids: Collection<Long>): List<Food>

    // 오타 보정용 자모 유사도 검색, pg_trgm의 %(threshold 필터)/<->(KNN 정렬) 연산자는 Postgres 전용 문법이라 JPQL(QueryDSL 포함)로 표현 불가
    // 네이티브 쿼리로 우회하도록 설정
    // 인덱스: idx_foods_name_jamo_trgm 표현식 인덱스
    @Query(
        value = """
            SELECT * FROM foods
            WHERE deleted_at IS NULL
              AND (
                (source IN ('seed', 'curated') AND visibility = 'public')
                OR (visibility = 'private' AND owner_user_id = :userId)
              )
              AND normalize(name, NFD) % normalize(:query, NFD)
            ORDER BY normalize(name, NFD) <-> normalize(:query, NFD)
            LIMIT :size
        """,
        nativeQuery = true,
    )
    fun findSimilarByJamo(
        @Param("query") query: String,
        @Param("userId") userId: Long,
        @Param("size") size: Int,
    ): List<Food>
}
