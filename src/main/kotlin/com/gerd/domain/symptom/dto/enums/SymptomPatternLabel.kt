package com.gerd.domain.symptom.dto.enums

/**
 * 증상 패턴 분석 라벨 — 규칙 기반 계산 결과에 붙는 고정 멘트
 * pattern/advice는 {key} 형태 placeholder를 포함할 수 있고, 실제 렌더링 시 값으로 치환한다
 */
enum class SymptomPatternLabel(
    val labelText: String,
    val pattern: String,
    val advice: String,
) {
    MAINTAIN(
        labelText = "유지 권장",
        pattern = "최근 {window_days}일간 비슷한 음식을 드신 뒤 {comfort_n}번 편안했어요.",
        advice = "지금처럼 편안했던 음식 위주로 이어가면 좋아요.",
    ),
    CAUTION(
        labelText = "주의 필요",
        pattern = "{food_group}을(를) 드신 뒤 비슷한 증상이 {repeat_n}번 반복됐어요.",
        advice = "다음엔 양을 줄이거나 천천히 드시면 속이 더 편할 수 있어요.",
    ),
    OBSERVING(
        labelText = "관찰 중",
        pattern = "아직 패턴을 말하기엔 기록이 조금 적어요.",
        advice = "며칠만 더 기록하면 {닉네임}님만의 패턴을 찾아드릴 수 있어요.",
    ),
}
