package com.gerd.domain.dictionary.listener

import com.gerd.domain.dictionary.service.DictionaryCommandService
import com.gerd.domain.meal.event.MealFoodJudgedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = KotlinLogging.logger {}

// 원 트랜잭션 커밋 후 도감을 갱신 — 실패해도 이미 커밋된 응답에 영향 주지 않도록 예외를 여기서 삼킨다
@Component
class DictionaryEventListener(
    private val dictionaryCommandService: DictionaryCommandService,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: MealFoodJudgedEvent) {
        runCatching {
            dictionaryCommandService.upsertCautionRiskEntry(event.userId, event.foodId, event.grade)
        }.onFailure {
            log.error(it) { "[afterCommit] 도감 CAUTION/RISK 갱신 실패: userId=${event.userId}, foodId=${event.foodId}, grade=${event.grade}" }
        }
    }
}
