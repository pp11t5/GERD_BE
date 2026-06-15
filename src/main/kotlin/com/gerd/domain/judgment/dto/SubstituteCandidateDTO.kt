package com.gerd.domain.judgment.dto

/**
 * 대체식 후보 — 정적 큐레이션 조회 결과에 후보의 트리거·알레르겐 코드를 함께 나른다
 *
 * 큐레이션은 원 음식의 태그 회피만 보장하므로(food-schema 결정 #10), "이 사용자"의
 * 트리거·알레르겐 보유 후보는 서비스가 tagCodes로 걸러낸 뒤에만 노출한다
 */
data class SubstituteCandidateDTO(
    val foodExternalId: String,
    val name: String,
    val tagCodes: Set<String>,
)
