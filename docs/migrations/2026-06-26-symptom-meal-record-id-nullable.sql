-- symptom_records.meal_record_id NOT NULL → NULL 허용
-- 미연결 증상(식사 연결 없이 단독 증상 기록) 지원을 위한 스키마 변경
-- 적용 대상: staging, prod (ddl-auto: validate 환경)
ALTER TABLE symptom_records
    ALTER COLUMN meal_record_id DROP NOT NULL;
