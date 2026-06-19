package com.gerd.domain.symptom.repository

import com.gerd.domain.symptom.entity.Symptom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SymptomRepository : JpaRepository<Symptom, Long> {
    fun findByMealRecordId(mealRecordId: UUID): List<Symptom>

    @Query("SELECT s.mealRecordId FROM Symptom s WHERE s.user.id = :userId AND s.mealRecordId IS NOT NULL")
    fun findLinkedMealRecordIdsByUserId(@Param("userId") userId: Long): List<UUID>
}
