-- FoodRepository.findSimilarByJamo 오타 보정 검색이 쓰는 pg_trgm 확장과 표현식 인덱스
-- normalize(name, NFD)로 한글을 자모 단위로 분해한 뒤 trigram 유사도(%)/거리(<->) 연산에 사용
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- CONCURRENTLY로 생성해 인덱스 빌드 중 foods 테이블 쓰기 잠금 방지 (spring.flyway.mixed 필요)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_foods_name_jamo_trgm
    ON foods USING gin (normalize(name, NFD) gin_trgm_ops);
