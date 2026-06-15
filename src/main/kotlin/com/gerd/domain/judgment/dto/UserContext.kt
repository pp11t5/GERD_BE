package com.gerd.domain.judgment.dto

import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO

// 판정 컨텍스트 중 사용자 건강 정보만 분리 — 텍스트 판정 파이프라인에서 DB 음식 엔티티 없이 사용
data class UserContext(
    val userTriggers: List<TagDTO>,
    val userAllergens: List<TagDTO>,
    val medications: List<String>,
    val symptomCodes: List<String>,
)
