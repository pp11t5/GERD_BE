package com.gerd.domain.onboarding.entity

import com.gerd.domain.auth.entity.User
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDateTime

/**
 * 온보딩 프로필 (계정당 1 row, users와 1:1 공유 PK)
 *
 * - row 존재 자체가 온보딩 완료 신호 (ADR-0007, 별도 플래그 없음)
 * - 온보딩 일괄제출 시 INSERT, 자식(증상/트리거/알레르기/복용약)의 부모
 * - user_id를 @MapsId로 PK이자 users FK로 매핑 — User 하드삭제 시 DB 캐스케이드로 프로필·자식까지 함께 정리
 */
@Entity
@Table(name = "user_profiles")
class UserProfile(
    // users(user_id)와 공유하는 PK 겸 FK — User가 소유, 하드삭제 시 함께 정리
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    // 07 트리거 자유입력 원문 — 파싱 없이 그대로 저장
    @Column(name = "custom_trigger_text")
    var customTriggerText: String? = null,

    // 제출 시점 = 온보딩 완료 시점
    @Column(name = "onboarded_at", nullable = false)
    val onboardedAt: LocalDateTime,
) : BaseEntity() {

    // @MapsId가 user.id로 채우는 공유 PK — 직접 할당하지 않는다
    @Id
    @Column(name = "user_id")
    val userId: Long = 0L
}
