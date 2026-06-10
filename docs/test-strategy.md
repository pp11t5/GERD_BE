# 테스트 전략

## 1. 문서 목적
이 문서는 백엔드 팀의 테스트 작성 기준과 전략을 정리한다.  
상세 규칙 → [test-writing skill](../.claude/skills/test-writing/SKILL.md)

## 2. 테스트 계층

| 계층 | 범위 | 도구 |
|---|---|---|
| Controller | 입출력/상태코드/응답 구조 | `@WebMvcTest`, MockMvc |
| Service | 비즈니스 로직, 예외 흐름 | JUnit5, Mockito |
| Repository | 쿼리/조회 정확성 | `@DataJpaTest`, H2 |
| 통합 | 전체 흐름 검증 | `@SpringBootTest` |

## 3. 기본 원칙

- 테스트는 CI 파이프라인에서 자동 실행 (`./gradlew test`)
- 하나의 테스트는 하나의 동작만 검증
- 테스트 메서드명은 `given_when_then` 또는 의도를 드러내는 한국어 사용
- DB가 필요한 통합 테스트는 H2 in-memory 사용
- 외부 인프라 의존 빈은 프로필 분리 또는 테스트 대체 빈으로 격리해 `@SpringBootTest` 컨텍스트 로딩이 깨지지 않도록 한다

## 4. Controller 테스트

- `@WebMvcTest`로 웹 계층만 격리
- Service는 Mock으로 대체
- 성공/실패 상태코드, 응답 필드 구조 검증

## 5. Service 테스트

- 외부 의존성(Repository 등)은 Mock 처리
- 비즈니스 예외 발생 조건과 메시지 검증 포함
- 경계값, 중복, 권한 실패 케이스 포함

## 6. Repository 테스트

- `@DataJpaTest`로 JPA 계층만 격리
- QueryDSL custom repository는 실제 쿼리 실행 검증
- 동적 조건 조합 케이스 포함

## 7. 금지 사항

- 프로덕션 DB에 연결하는 테스트 작성 금지
- 테스트 간 상태 공유 금지 (각 테스트는 독립적으로 동작)
- 검증 없는 빈 테스트 커밋 금지
