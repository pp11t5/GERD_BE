package com.gerd.domain.onboarding.entity

import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

// 08 복용약 다건 추가 (1건 1행) — name은 자유 텍스트, 정규화·dedup 안 함
@Entity
@Table(name = "user_medications")
class UserMedication(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_medication_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val userProfile: UserProfile,

    name: String,
) : BaseEntity() {

    @Column(nullable = false)
    var name: String = name
        protected set

    fun update(name: String) {
        this.name = name
    }
}
