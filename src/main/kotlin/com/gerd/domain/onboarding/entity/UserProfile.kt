package com.gerd.domain.onboarding.entity

import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 온보딩 프로필 (계정당 1 row, users와 1:1 공유 PK)
 *
 * - row 존재 자체가 온보딩 완료 신호 (ADR-0007, 별도 플래그 없음)
 * - 온보딩 일괄제출 시 INSERT, 자식(증상/트리거/알레르기/복용약)의 부모
 */
@Entity
@Table(name = "user_profiles")
class UserProfile(
    // users(user_id)와 공유하는 PK — 인증이 소유, 생성 전략 없이 할당
    @Id
    @Column(name = "user_id")
    val userId: Long,

    // 07 트리거 자유입력 원문 — 파싱 없이 그대로 저장
    @Column(name = "custom_trigger_text")
    var customTriggerText: String? = null,

    // 제출 시점 = 온보딩 완료 시점
    @Column(name = "onboarded_at", nullable = false)
    val onboardedAt: LocalDateTime,
) : BaseEntity()
