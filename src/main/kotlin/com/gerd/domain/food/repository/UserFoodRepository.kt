package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.UserFood
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface UserFoodRepository : JpaRepository<UserFood, Long> {

    @Query(
        value = "SELECT uf FROM UserFood uf JOIN FETCH uf.food ORDER BY uf.id DESC",
        countQuery = "SELECT COUNT(uf) FROM UserFood uf",
    )
    fun findAllWithFood(pageable: Pageable): Page<UserFood>

    @Query(
        value = "SELECT uf FROM UserFood uf JOIN FETCH uf.food WHERE uf.isUnknown = :isUnknown ORDER BY uf.id DESC",
        countQuery = "SELECT COUNT(uf) FROM UserFood uf WHERE uf.isUnknown = :isUnknown",
    )
    fun findAllWithFoodByIsUnknown(isUnknown: Boolean, pageable: Pageable): Page<UserFood>

    @Transactional
    @Modifying
    @Query("DELETE FROM UserFood uf WHERE uf.food.id = :foodId")
    fun deleteAllByFoodId(@Param("foodId") foodId: Long)

    fun existsByUserIdAndFoodId(userId: Long, foodId: Long): Boolean
}
