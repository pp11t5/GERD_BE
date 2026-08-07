package com.gerd.domain.dictionary.repository

import com.gerd.domain.dictionary.entity.UserFoodDictionary
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserFoodDictionaryRepository : JpaRepository<UserFoodDictionary, Long> {

    fun countByUser_IdAndDictionaryType(userId: Long, type: DictionaryType): Long

    fun countByUser_IdAndDictionaryTypeIn(userId: Long, types: List<DictionaryType>): Long

    // 커서 페이징(id 내림차순) — food는 단일 연관이라 fetch join + Pageable
    // cursor가 null이면 첫 페이지. hasNext 판정을 위해 호출부에서 size+1로 조회한다.
    @Query(
        """
        SELECT d FROM UserFoodDictionary d JOIN FETCH d.food
        WHERE d.user.id = :userId AND d.dictionaryType = :type
          AND (:cursor IS NULL OR d.id < :cursor)
        ORDER BY d.id DESC
        """,
    )
    fun findWithFoodByCursorAndType(
        userId: Long,
        type: DictionaryType,
        cursor: Long?,
        pageable: Pageable,
    ): List<UserFoodDictionary>

    // 커서 페이징(id 내림차순) — food는 단일 연관이라 fetch join, Pageable
    @Query(
        """
        SELECT d FROM UserFoodDictionary d JOIN FETCH d.food
        WHERE d.user.id = :userId AND d.dictionaryType IN :types
          AND (:cursor IS NULL OR d.id < :cursor)
        ORDER BY d.id DESC
        """,
    )
    fun findWithFoodByCursorAndTypeIn(
        userId: Long,
        types: List<DictionaryType>,
        cursor: Long?,
        pageable: Pageable,
    ): List<UserFoodDictionary>

    fun findByUser_IdAndFood_IdAndDictionaryType(userId: Long, foodId: Long, type: DictionaryType): UserFoodDictionary?

    // 사용자별 음식은 한 행만 유지하고, 다시 판정되면 타입과 수정 시각을 원자적으로 갱신한다.
    @Modifying
    @Query(
        value = """
            INSERT INTO user_food_dictionaries (user_id, food_id, dictionary_type, created_at, modified_at)
            VALUES (:userId, :foodId, :type, now(), now())
            ON CONFLICT (user_id, food_id)
            DO UPDATE SET dictionary_type = EXCLUDED.dictionary_type, modified_at = now()
        """,
        nativeQuery = true,
    )
    fun upsertType(userId: Long, foodId: Long, type: String): Int

    @Modifying
    @Query("DELETE FROM UserFoodDictionary d WHERE d.user.id = :userId AND d.food.id IN :foodIds AND d.dictionaryType = :type")
    fun deleteByUserIdAndFoodIdsAndType(userId: Long, foodIds: List<Long>, type: DictionaryType)

    // RECOMMEND 재판정 시 기존 주의/위험 등재 제거
    @Modifying
    @Query("DELETE FROM UserFoodDictionary d WHERE d.user.id = :userId AND d.food.id = :foodId AND d.dictionaryType IN :types")
    fun deleteByUserIdAndFoodIdAndTypeIn(userId: Long, foodId: Long, types: List<DictionaryType>)
}
