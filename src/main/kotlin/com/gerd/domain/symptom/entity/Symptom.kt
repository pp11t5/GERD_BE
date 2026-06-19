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
@SQLDelete(sql = "UPDATE symptom_records SET deleted_at = CURRENT_TIMESTAMP, modified_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class Symptom(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "symptom_state", nullable = false)
    val symptomState: SymptomState,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "symptom_types", joinColumns = [JoinColumn(name = "symptom_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "symptom_type", nullable = false)
    val symptomTypes: Set<SymptomType>,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: LocalDateTime,

    @Column(name = "meal_record_id")
    val mealRecordId: Long? = null,

    @Column(length = 200)
    var memo: String? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "symptom_id")
    val id: Long? = null,
) : BaseEntity() {

    fun updateMemo(memo: String?) {
        this.memo = memo?.trim()?.takeUnless { it.isEmpty() }
    }
}
