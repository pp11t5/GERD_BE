package com.gerd.domain.auth.entity.enums

enum class UserStatus {
    // 정상 로그인 가능
    ACTIVE,

    // TODO: 추후 구현 — 본인 비활성화 또는 장기 미사용 자동 전환
    //  로그인 시도 시 ACCOUNT_INACTIVE 에러 반환, 복구 플로우로 분기
    INACTIVE,

    // 탈퇴 유예 14일 중 — 로그인 시 ACCOUNT_RECOVERABLE 에러 반환
    // 14일 후 스케줄러가 물리 삭제 처리
    DELETED,
}
