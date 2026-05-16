# 배포/운영 전략 정리

## 1. 문서 목적
이 문서는 배포 플랫폼 선택, 전환 전략, CI/CD 운영 방향을 정리한다.

## 2. 배포 방식

### 2.1 운영 배포는 Dockerfile 단일 방식

Railway는 `Dockerfile`을 감지하면 자동으로 이미지를 빌드해 배포한다.  
`docker-compose.yml`은 Railway 배포에는 사용되지 않고, 로컬 검증용으로만 사용한다.

| 구분 | 방식 | 이유 |
|---|---|---|
| Railway 배포 | `Dockerfile` 단일 | Railway 공식 지원, 설정 단순 |
| 로컬 개발 | Spring Boot 직접 실행 + H2 in-memory | 기본 설정만으로 바로 실행 가능 |
| 로컬 DB 검증 | `docker-compose.yml` | PostgreSQL/H2를 컨테이너로 띄워 추가 검증 가능 |
| 향후 AWS 전환 | `Dockerfile` 재사용 | ECR 이미지 빌드에 그대로 사용 가능 |

**현재 Dockerfile 구성**
- 멀티스테이지 빌드: `gradle:9.4.1-jdk21-jammy` (빌드) → `eclipse-temurin:21-jre-jammy` (런타임)
- 런타임 이미지만 배포 → 이미지 크기 최소화
- `GRADLE_OPTS="-Xmx512m -Xms256m"` — Railway 메모리 제한 대응

### 2.2 로컬 실행 방식

기본 로컬 실행은 애플리케이션 내장 H2를 사용한다.

profile 분리
- `application.yml`: H2 in-memory (개발/테스트 공통), 기본 local로 설정, docker-compose.yml과 동기화
- `application-test.yml`: H2 in-memory (테스트 전용)
- `application-prod.yml`: 운영용

```bash
./gradlew bootRun
```

- 기본 datasource: `jdbc:h2:mem:gerd;MODE=PostgreSQL`
- H2 콘솔: `http://localhost:8080/h2-console`
- 별도 DB 컨테이너 없이 바로 실행 가능

### 2.3 docker-compose 로컬 DB 실행 방식

로컬에서 컨테이너 DB로 추가 검증이 필요하면 `docker-compose.yml`을 사용한다.

```bash
docker compose up -d
```

기동 대상:
- `postgres`: `localhost:5432`
- `h2`: TCP `localhost:1521`, 웹 콘솔 `http://localhost:8082`

권장 접속값:
- PostgreSQL: `jdbc:postgresql://localhost:5432/gerd`
- PostgreSQL 계정: `gerd` / `gerd`
- H2 컨테이너: `jdbc:h2:tcp://localhost:1521/gerd;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`

정리:
- 현재 `application.yml` 기본값은 내장 H2를 사용하므로 `docker compose up`만으로 애플리케이션 대상 DB가 자동 전환되지는 않는다
- 즉, `application.yml`의 `jdbc:h2:mem:gerd...` 와 `docker-compose`의 H2는 DB 이름은 `gerd`로 맞췄지만, 실행 방식은 `in-memory` 와 `tcp server`로 다르다
- 현재 `application-test.yml`도 H2 in-memory를 사용하므로 테스트 실행이 `docker-compose` DB와 충돌하지 않는다
- 즉, 지금 상태에서 문서상 혼선은 있었지만 설정 충돌이나 포트 충돌은 없다
- PostgreSQL 컨테이너를 실제 애플리케이션 검증에 쓰려면 별도 프로필 또는 `SPRING_DATASOURCE_*` 환경변수 override가 필요하다

### 2.4 Railway 배포 흐름

```
main push
  → Railway가 Dockerfile 감지
  → 이미지 빌드
  → 컨테이너 기동 (SPRING_PROFILES_ACTIVE=prod)
  → /actuator/health 헬스체크
  → 이전 컨테이너 교체
```

### 3. CI 파이프라인 (GitHub Actions)

목표: PR 단계에서 빌드/테스트 실패를 조기 발견. 실제 배포는 Railway가 담당.

```
PR (main, develop 대상)
  → checkout
  → JDK 21 세팅 + Gradle 캐시
  → ./gradlew build -x test   # 빌드 검증
  → ./gradlew test            # 전체 테스트
  → 실패 시 병합 차단
```

Railway 자동 배포와 역할 분리:
- **GitHub Actions**: 빌드 + 테스트 - PR 단계에서 안정성 검증
- **Railway**: 이미지 빌드 + 배포 

필수 관리 항목:
- 모든 시크릿은 Railway Variables에서만 관리 (Git 저장 금지)
- 로컬 환경변수는 `.env` 파일 (`.gitignore` 포함)

## 5. 운영 전환 시 전략 

1. `prod` 프로필 추가 후 PostgreSQL 연결 정보는 환경변수로 주입
2. `org.postgresql:postgresql` 의존성 활성화
3. 운영의 `ddl-auto`는 `validate` 또는 마이그레이션 도구(Flyway/Liquibase) 기반으로 전환
4. Railway 배포 파이프라인에서 환경변수/시크릿 정리
5. AWS 전환 시 RDS + ECS(또는 EC2/Elastic Beanstalk) 기준으로 단계적 마이그레이션 계획 수립
