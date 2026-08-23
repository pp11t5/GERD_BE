-- 복용약 수집 전면 폐지 (L11) — 기존 데이터는 파기 대상이라 soft delete 없이 테이블째 제거
DROP TABLE IF EXISTS user_medications;
