package com.gerd.domain.onboarding.entity

import com.gerd.domain.onboarding.entity.id.UserSymptomId
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

// 06 증상 다중선택 (저장 전용, F2 미사용) — symptom_code는 SymptomCode enum으로 검증된 값
@Entity
@Table(name = "user_symptoms")
class UserSymptom(
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val userProfile: UserProfile,

    @EmbeddedId
    val id: UserSymptomId = UserSymptomId(),
) : BaseEntity()
