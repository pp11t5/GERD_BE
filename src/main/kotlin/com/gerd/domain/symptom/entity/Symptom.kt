package com.gerd.domain.symptom.entity

import com.gerd.domain.auth.entity.User
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

@Entity
@Table(
    name = "symptom_records",
    indexes = [
        Index(name = "symptom_records_user_occurred_idx", columnList = "user_id, occurred_at"),
    ],
)
@SQLDelete(sql = "UPDATE symptom_records SET deleted_at = CURRENT_TIMESTAMP, modified_at = CURRENT_TIMESTAMP WHERE symptom_id = ?")
@SQLRestriction("deleted_at IS NULL")
class Symptom(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    symptomState: SymptomState,

    symptomTypes: Set<SymptomType>,

    occurredAt: LocalDateTime,

    mealRecordId: Long,

    memo: String? = null,

    deletedAt: LocalDateTime? = null,

    analysisJson: String? = null,

    isAnalysisDirty: Boolean = true,

    analysisVersion: Long = 0L,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "symptom_id")
    val id: Long? = null,
) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "symptom_state", nullable = false)
    var symptomState: SymptomState = symptomState
        protected set

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "symptom_types", joinColumns = [JoinColumn(name = "symptom_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "symptom_type", nullable = false)
    var symptomTypes: Set<SymptomType> = symptomTypes
        protected set

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: LocalDateTime = occurredAt
        protected set

    @Column(name = "meal_record_id", nullable = false)
    var mealRecordId: Long = mealRecordId
        protected set

    @Column(length = 200)
    var memo: String? = memo
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = deletedAt
        protected set

    @Column(name = "analysis_json", columnDefinition = "TEXT")
    var analysisJson: String? = analysisJson
        protected set

    @Column(name = "is_analysis_dirty", nullable = false, columnDefinition = "boolean default true")
    var isAnalysisDirty: Boolean = isAnalysisDirty
        protected set

    @Column(name = "analysis_version", nullable = false, columnDefinition = "bigint default 0")
    var analysisVersion: Long = analysisVersion
        protected set

    fun update(
        symptomState: SymptomState,
        symptomTypes: Set<SymptomType>,
        occurredAt: LocalDateTime,
        mealRecordId: Long,
        memo: String?,
    ) {
        this.symptomState = symptomState
        this.symptomTypes = symptomTypes
        this.occurredAt = occurredAt
        this.mealRecordId = mealRecordId
        this.memo = normalizeMemo(memo)
        markAnalysisDirty()
    }

    fun updateMemo(memo: String?) {
        this.memo = normalizeMemo(memo)
        markAnalysisDirty()
    }

    fun markAnalysisDirty() {
        isAnalysisDirty = true
        analysisVersion += 1
    }

    fun updateAnalysis(analysisJson: String, expectedVersion: Long): Boolean {
        if (!isAnalysisDirty || analysisVersion != expectedVersion) return false
        this.analysisJson = analysisJson
        isAnalysisDirty = false
        return true
    }

    private fun normalizeMemo(memo: String?): String? {
        return memo?.trim()?.takeUnless { it.isEmpty() }
    }
}
