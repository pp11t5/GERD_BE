-- 마케팅 수신 동의와 앱 푸시 알림 분리 — 앱 알림 3종(식후/식단기록/주간리포트)은 기본 활성화
UPDATE user_notification_settings
SET post_meal_notification_enabled = true,
    daily_record_notification_enabled = true,
    weekly_report_enabled = true;

-- 약관 제목 문구 변경 — 버전 신설 없이 같은 code의 모든 행 갱신
UPDATE terms SET title = '개인정보 수집·이용 동의' WHERE code = 'privacy';
UPDATE terms SET title = '마케팅 정보 수신' WHERE code = 'marketing';
