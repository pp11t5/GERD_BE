# 배포/운영 전략 정리

## 1. 문서 목적
이 문서는 배포 플랫폼 선택, 전환 전략, CI/CD 운영 방향을 정리한다.

## 1.1 추가 도입사항

| 항목 | 적용 내용 | 기대 효과 |
|---|---|---|
| 빌드 컨텍스트 최소화 | `.dockerignore`에서 `build/`, `.gradle/`, 문서, Git 메타데이터, 테스트 소스를 제외 | Railway 업로드 크기 감소, 빌드 시작 시간 단축 |
| 레이어 캐싱 최적화 | `Dockerfile`에서 Gradle 스크립트를 먼저 복사하고 의존성 해석 레이어를 분리 | 코드 변경 시 의존성 레이어 재사용 |
| Layered JAR 런타임 | Spring Boot layertools로 `dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application` 레이어 분리 | 재배포 속도 개선, 이미지 레이어 효율 증가 |
| 런타임 보안 기본값 | 실행 이미지를 non-root 유저로 구동 | 컨테이너 권한 최소화 |
| Railway 파이프라인 명시 | PR 검증과 Railway 배포 역할을 문서로 분리 | 배포 책임 범위 명확화 |

## 2. 배포 방식

### 2.1 운영 배포는 Dockerfile 단일 방식

Railway는 `Dockerfile`을 감지하면 자동으로 이미지를 빌드해 배포한다.  
`docker-compose.yml`은 Railway 배포에는 사용되지 않고, 로컬 검증용으로만 사용한다.

| 구분 | 방식 | 이유 |
|---|---|---|
| Railway 배포 | `Dockerfile` 단일 | Railway 공식 지원, 설정 단순 |
| 로컬 개발 | Spring Boot 직접 실행 + PostgreSQL | 운영 DB와 같은 계열로 검증 |
| 로컬 DB 검증 | `docker-compose.yml` | PostgreSQL 컨테이너 실행 |
| 향후 AWS 전환 | `Dockerfile` 재사용 | ECR 이미지 빌드에 그대로 사용 가능 |

**현재 Dockerfile 구성**
- 멀티스테이지 빌드: `gradle:9.4.1-jdk21-jammy` (빌드) → `eclipse-temurin:21-jre-jammy` (레이어 추출/런타임)
- Gradle 스크립트 선복사 후 의존성 레이어 캐싱
- Spring Boot Layered JAR 추출 후 런타임 레이어 분리 복사
- `GRADLE_OPTS="-Xmx512m -Xms256m"` 적용
- 런타임 컨테이너는 non-root 유저로 실행

### 2.2 로컬 실행 방식

기본 로컬 실행은 PostgreSQL을 사용한다.

profile 분리
- `application-local.yml`: 로컬 PostgreSQL, 기본 local로 설정
- `application-test.yml`: Testcontainers PostgreSQL (테스트 전용)
- `application-prod.yml`: 운영용

```bash
docker compose up -d postgres
./gradlew bootRun
```

- 기본 datasource: `jdbc:postgresql://localhost:5432/gerd`
- 기본 계정: `gerd` / `gerd`

### 2.3 docker-compose 로컬 DB 실행 방식

로컬 PostgreSQL 실행에는 `docker-compose.yml`을 사용한다.

```bash
docker compose up -d
```

기동 대상:
- `postgres`: `localhost:5432`

권장 접속값:
- PostgreSQL: `jdbc:postgresql://localhost:5432/gerd`
- PostgreSQL 계정: `gerd` / `gerd`

정리:
- `local` 프로필은 기본적으로 docker-compose의 PostgreSQL을 바라본다
- 테스트는 Testcontainers PostgreSQL을 사용하므로 docker-compose DB와 충돌하지 않는다

### 2.4 Railway 배포 흐름

```
main push
  → Railway가 Dockerfile 감지
  → .dockerignore 기준으로 최소 컨텍스트 업로드
  → Docker build cache 확인
  → Gradle 의존성 레이어 재사용
  → 이미지 빌드
  → Layered JAR 추출 및 런타임 이미지 생성
  → 컨테이너 기동 (SPRING_PROFILES_ACTIVE=prod)
  → /actuator/health 헬스체크
  → 이전 컨테이너 교체
```

### 2.5 Railway 운영 파이프라인

| 단계 | 주체 | 핵심 작업 | 비고 |
|---|---|---|---|
| PR 검증 | GitHub Actions | `./gradlew build -x test`, `./gradlew test` | 병합 전 품질 게이트 |
| main 반영 | GitHub | `main` 브랜치 merge/push | Railway 배포 트리거 |
| 이미지 빌드 | Railway | `Dockerfile` 기반 멀티스테이지 빌드 | `.dockerignore` 반영 |
| 애플리케이션 기동 | Railway | `prod` 프로필로 컨테이너 실행 | `PORT`는 Railway 주입값 사용 |
| 상태 확인 | Railway | `/actuator/health` 헬스체크 | 실패 시 롤백 판단 기준 |

### 2.6 Docker 최적화 세부 기준

| 구분 | 적용 기준 | 이유 |
|---|---|---|
| `.dockerignore` | 로컬 빌드 산출물, IDE 설정, 문서, Git 메타데이터 제외 | 업로드 크기 최소화 |
| 의존성 캐싱 | `gradlew`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/` 선복사 | 소스 변경과 의존성 변경을 분리 |
| BuildKit 캐시 | `--mount=type=cache,target=/home/gradle/.gradle` | Gradle 다운로드 재사용 가능성 확보 |
| Layered JAR | `java -Djarmode=layertools -jar app.jar extract` | 외부 라이브러리와 애플리케이션 코드를 분리 |
| 런타임 엔트리포인트 | `org.springframework.boot.loader.launch.JarLauncher` | Spring Boot 4.0.5 기준 로더 경로 |

### 2.7 현재 Railway 설정에서 다운타임 최소화 체크리스트

| 항목 | 권장 값 | 이유 |
|---|---|---|
| Healthcheck Path | `/actuator/health` | 새 배포가 정상 응답 전까지 활성 전환 지연 |
| Healthcheck Timeout | 기본 300초에서 시작 | Spring 초기화가 느릴 때 오탐 배포 실패 방지 |
| `PORT` 바인딩 | `${PORT:8080}` 유지 | Railway 라우팅 포트와 애플리케이션 포트 일치 |
| Teardown Overlap | 10~30초 | 신버전 활성 직후 구버전 즉시 종료 방지 |
| Teardown Draining | 10~30초 | 기존 연결을 짧게 정리할 시간 확보 |
| Restart Policy | `On Failure` | 비정상 종료 시 자동 복구 |
| Replicas | 현재 1 유지, 가능하면 추후 2 이상 | 단일 인스턴스보다 실제 무중단에 가까워짐 |
| Serverless | Off | sleep 후 첫 요청 지연과 cold start 방지 |
| Volume 의존성 | 가능하면 없음 | attached volume 서비스는 재배포 시 짧은 중단 가능 |

### 2.8 Railway 대시보드 권장값

| 화면 항목 | 권장 값 | 메모 |
|---|---|---|
| Wait for CI | CI 도입 후 활성화 | 실패한 빌드/테스트 배포 방지 |
| Healthcheck Path | `/actuator/health` | 필수 |
| Healthcheck Timeout | 기본 300초, 필요 시 증가 | 느린 기동이면 timeout 먼저 점검 |
| Deployment Overlap | 10~30초 | 신버전과 구버전 겹침 시간 |
| Deployment Draining | 10~30초 | 종료 전 요청 정리 시간 |
| Restart Policy | `On Failure` | 현재 설정 유지 권장 |
| Replicas | 1에서 시작 | 비용 절약, 완전 무중단은 아님 |
| Serverless | Off | 일반 API 서버 기본값 권장 |

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
- PostgreSQL 연결 변수는 현재 Railway 기본 변수명 `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` 기준으로 운영
- 운영 모니터링 변수는 `SENTRY_DSN`, `SENTRY_ENVIRONMENT`, `SENTRY_RELEASE`, `SENTRY_TRACES_SAMPLE_RATE` 기준으로 관리

## 4. 추후 설정하면 좋은 항목

| 항목 | 권장 시점 | 기대 효과 |
|---|---|---|
| GitHub Actions CI + Wait for CI | `main` 자동배포를 신뢰해야 할 때 | 실패 커밋 배포 차단 |
| Custom Domain | 외부 공개 직전 | 신뢰도, CORS, OAuth redirect 관리 용이 |
| Cloudflare Proxy/WAF | 커스텀 도메인 연결 후 | TLS, 기본 보안, DDoS 완화 |
| Rate Limit | 인증 API 공개 시점 | 로그인/토큰 남용 방어 |
| Replicas 2 이상 | 실제 사용자 유입 후 | 다운타임 감소, 가용성 향상 |
| External Uptime Monitor | 운영 모니터링 필요 시 | Railway 외부에서 연속 상태 확인 |
| DB Migration 도구 | 스키마 변경이 잦아질 때 | 운영 배포 안정성 향상 |
| Rollback 절차 문서화 | 첫 장애 대응 전에 | 복구 시간 단축 |

## 5. 운영 전환 시 전략 

1. 운영의 `ddl-auto`는 `validate` 또는 마이그레이션 도구(Flyway/Liquibase) 기반으로 전환
2. Railway 배포 파이프라인에서 환경변수/시크릿 정리
3. AWS 전환 시 RDS + ECS(또는 EC2/Elastic Beanstalk) 기준으로 단계적 마이그레이션 계획 수립

## 부록. Startup Time / Serverless 운영 메모

### Startup Time

- Railway는 Healthcheck Path가 설정되어 있으면 해당 경로가 `200`을 반환할 때까지 새 배포를 활성화하지 않는다.
- Healthcheck timeout 기본값은 300초이며, 부족하면 서비스 설정 또는 `RAILWAY_HEALTHCHECK_TIMEOUT_SEC`로 조정한다.
- Spring Boot startup time이 길어지는 대표 원인은 DB 연결 지연, Hibernate 초기화, 큰 이미지 pull, 캐시 워밍, 외부 API 초기화다.
- 현재 프로젝트는 Docker 이미지 최적화와 Layered JAR로 image pull 및 재배포 시간을 줄이는 방향을 적용했다.
- 시작 시간이 길면 먼저 Railway deployment logs로 느린 구간을 확인하고, 그다음 timeout을 늘린다.

### Serverless

- Railway의 `Serverless`는 기존 `App Sleeping` 기능 이름이 변경된 것이다.
- 서비스가 10분 이상 outbound traffic을 보내지 않으면 sleep 대상으로 간주될 수 있다.
- 첫 요청이 서비스를 깨우며 cold boot가 발생할 수 있고, 공식 문서상 첫 요청에서 `502`가 발생할 수도 있다.
- DB connection pool, telemetry, 다른 내부 서비스 호출이 있으면 sleep 상태로 잘 진입하지 않을 수 있다.
- 인증/일반 API 서버는 응답 지연과 예측 불가능성을 줄이기 위해 기본적으로 `Off`를 권장한다.
- 배치성 API, 내부 도구, 저트래픽 데모 환경에서만 비용 절감 목적으로 검토한다.
