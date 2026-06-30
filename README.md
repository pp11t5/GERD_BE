# GERD Server

**GERD** 프로젝트의 백엔드 레포지토리입니다.  
Spring Boot 기반의 서버로 구성되어 있으며, 위식도역류질환(GERD) 관리를 위한 식사 기록, 증상 기록, 음식 판단 기능을 제공합니다.

## 프로젝트 진행 기간

**Prography 11기**

2026.05 ~ ing

## 🛠 Tech Stack

<p>
  <strong>Language</strong><br />
  <img src="https://img.shields.io/badge/Kotlin_2.2.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
</p>

<p>
  <strong>Framework / Security / Test</strong><br />
  <img src="https://img.shields.io/badge/Spring_Boot_4.0.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/QueryDSL-005571?style=for-the-badge&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" />
  <img src="https://img.shields.io/badge/Actuator-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/JUnit_5-25A162?style=for-the-badge&logo=junit5&logoColor=white" />
</p>

<p>
  <strong>Deploy</strong><br />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/Railway-0B0D0E?style=for-the-badge&logo=railway&logoColor=white" />
</p>

<p>
  <strong>Database</strong><br />
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Testcontainers-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
</p>

<p>
  <strong>ETC</strong><br />
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" />
  <img src="https://img.shields.io/badge/Sentry-362D59?style=for-the-badge&logo=sentry&logoColor=white" />
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" />
  <img src="https://img.shields.io/badge/Gemini-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white" />
</p>

---

## 📚 목차

1. [프로젝트 소개](#-프로젝트-소개)
2. [기술 스택](#-기술-스택)
3. [서버 아키텍처](#-서버-아키텍처)
4. [프로젝트 구조](#-프로젝트-구조)
5. [브랜치 전략](#-브랜치-전략)
6. [Github 관리 규칙](#-github-관리-규칙)

---

## 📖 프로젝트 소개

**GERD**는 위식도역류질환(GERD)을 관리하는 사용자가 자신의 식사와 증상 데이터를 기록하고, 음식 섭취 전 GERD 관점의 위험도를 확인할 수 있도록 돕는 서비스입니다.

백엔드 서버는 인증, 온보딩, 음식 검색, 식사 기록, 증상 기록, AI 기반 음식 판단, 증상 패턴 분석, 알림 설정을 담당합니다.

**주요 기능**

- 온보딩, Kakao OIDC 로그인, JWT 기반 인증
- 음식 검색 및 최근 조회 음식 관리
- GERD 관점의 음식 섭취 판단
- 대체 음식 추천
- 식사 기록 생성, 조회, 삭제
- 식사와 연결된 증상 기록 관리
- 증상 패턴 분석
- FCM 기반 알림 토큰 및 알림 설정 관리
- Actuator 기반 서버 상태 확인

---

## 🛠 기술 스택

| 카테고리 | 기술 |
|------|------|
| **Language** | Kotlin 2.2.21, Java 21 |
| **Framework** | Spring Boot 4.0.5, Spring MVC, Spring Data JPA, QueryDSL |
| **Security** | Spring Security, JWT, Kakao OIDC |
| **Database** | PostgreSQL |
| **Test** | JUnit 5, Mockito, MockK, Testcontainers PostgreSQL |
| **Monitoring** | Spring Actuator, Sentry, Logback JSON |
| **Deployment / Infra** | Docker, Railway, GitHub Actions |
| **Documentation** | Swagger(OpenAPI) |
| **External API** | Gemini API, Firebase Admin SDK |

---

## 🖥 서버 아키텍처

```text
Client
  └── GERD Server (Spring Boot)
        ├── PostgreSQL
        ├── Kakao OIDC
        ├── Gemini API
        ├── Firebase Cloud Messaging
        └── Sentry
```

Railway는 `Dockerfile`을 기반으로 서버 이미지를 빌드하고 배포합니다.  
서버 상태 확인은 Actuator health check를 기준으로 합니다.

---

## 📂 프로젝트 구조

```plaintext
src
 └── main
     ├── kotlin
     │   └── com
     │       └── gerd
     │           ├── GerdApplication.kt
     │           ├── domain
     │           │   ├── auth            # 인증, JWT, OIDC
     │           │   ├── fcm             # FCM 토큰 및 푸시 발송
     │           │   ├── food            # 음식 검색, 최근 음식
     │           │   ├── health          # 서버 상태 확인
     │           │   ├── judgment        # 음식 섭취 판단
     │           │   ├── meal            # 식사 기록
     │           │   ├── notification    # 알림 설정 및 스케줄
     │           │   ├── onboarding      # 온보딩, 동의, 사용자 프로필
     │           │   └── symptom         # 증상 기록 및 패턴 분석
     │           └── global              # 공통 설정, 응답, 예외, 검증, 로깅
     └── resources
         ├── application.yml
         ├── application-local.yml
         ├── application-prod.yml
         └── application-staging.yml
```

도메인 내부는 대체로 아래 구조를 따릅니다.

```plaintext
{domain}
 ├── controller
 ├── service
 ├── repository
 ├── entity
 ├── dto
 └── exception
```

현재 API는 `/api/v1/**` prefix를 사용합니다.

---

## 🌿 브랜치 전략

### Workflow

- 배포 기준 브랜치: `main`
- 통합 개발 브랜치: `develop`
- 기능 개발 브랜치: `feature/*`
- 버그 수정 브랜치: `fix/*`
- 초기 설정 브랜치: `init/*`

> 기능 개발 시 통합 브랜치에서 파생된 기능 브랜치에서 작업합니다.  
> 완료되면 Pull Request를 통해 리뷰 후 병합합니다.

---

## 📍 Github 관리 규칙

- `main` 브랜치는 직접 push하지 않고 Pull Request를 통해 반영
- PR은 리뷰어 1명 이상 승인 후 병합
- CI 통과 후 병합
- CI는 빌드와 테스트를 분리해 검증
- API 응답은 공통 `ApiResponse<T>` 포맷 사용
- 기본 API 문서는 Swagger(`/swagger-ui/index.html`)로 관리
- 컨트롤러는 얇게 유지하고 비즈니스 로직은 서비스 계층에 배치
- 커밋 메시지는 `타입: #이슈번호 작업 내용` 형식을 기본으로 사용
