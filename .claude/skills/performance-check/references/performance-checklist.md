# Performance Checklist

## Query
- N+1 위험
- 불필요한 전체 조회
- projection 가능성
- fetch join / 2단계 조회 판단
- count 쿼리 비용
- 쿼리 실행 횟수
- 응답 DTO 크기

## Index
- 필터 컬럼 인덱스
- 정렬 키 인덱스
- 커서 + tie-breaker 컬럼 조합
- unique constraint 필요 여부
- covering index 가능성
- 실행계획 기준 실제 사용 여부

## Runtime
- 트랜잭션 안의 외부 호출
- 캐시 fallback
- 반복 계산 / 중복 조회
- 동기 호출로 인한 대기 시간
- p95 / TPS / latency 기준 병목
- 피크 트래픽 기준 병목 확대 가능성

## Cache
- 어떤 데이터가 캐시 대상인지
- key 설계
- TTL 전략
- 무효화 방식
- Redis 자료구조 선택 근거

## Load Test
- 사용자 행동 기반 시나리오
- VU / duration 적정성
- 주요 API 선정 이유
- error rate 기준
- 개선 후 재측정 계획
