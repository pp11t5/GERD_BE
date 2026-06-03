package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.TriggerLabel
import org.springframework.data.jpa.repository.JpaRepository

interface TriggerLabelRepository : JpaRepository<TriggerLabel, Long> {
    // 온보딩 입력 code 집합을 마스터로 resolve — 반환 수 < 요청 수면 시드 누락 code 존재
    fun findByCodeIn(codes: Collection<String>): List<TriggerLabel>
}
