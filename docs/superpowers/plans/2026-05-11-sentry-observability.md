# Sentry Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 운영 환경에서 5xx 예외, 에러와 연관된 로그 breadcrumbs, 검색 가능한 Sentry structured logs를 수집한다.

**Architecture:** Spring Boot 4용 Sentry starter와 Logback appender를 사용한다. 공통 예외 핸들러는 `ApiResponse`로 예외를 감싸기 전에 5xx 예외만 Sentry에 명시적으로 전달한다. 운영 배포는 SSM `/saynow/prod` 파라미터를 EC2 `.env`로 동기화한다.

**Tech Stack:** Java 21, Spring Boot 4.0.6, Gradle Kotlin DSL, Logback, Sentry Java SDK.

---

### Task 1: 5xx 예외 캡처 동작

**Files:**
- Create: `src/test/java/com/saynow/common/exception/GlobalExceptionHandlerTest.java`
- Create: `src/main/java/com/saynow/common/observability/SentryEventReporter.java`
- Create: `src/main/java/com/saynow/common/observability/DefaultSentryEventReporter.java`
- Modify: `src/main/java/com/saynow/common/exception/GlobalExceptionHandler.java`

- [x] 테스트를 먼저 작성해 `Exception`과 5xx `ApiException`만 캡처되고 4xx `ApiException`은 캡처되지 않는지 검증한다.
- [x] 테스트 실패를 확인한다.
- [x] Sentry 전송을 감싸는 작은 reporter를 추가하고 핸들러에 주입한다.
- [x] 테스트 통과를 확인한다.

### Task 2: Sentry SDK와 Logback 설정

**Files:**
- Modify: `build.gradle.kts`
- Create: `src/main/resources/logback-spring.xml`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-prod.yml`
- Modify: `src/main/resources/application-local.yml`

- [x] Spring Boot 4용 Sentry starter와 Logback appender 의존성을 추가한다.
- [x] 로컬 기본값은 비활성화하고 운영은 `SENTRY_DSN`, `SENTRY_ENVIRONMENT`, `SENTRY_RELEASE`, `SENTRY_LOGS_ENABLED`로 제어한다.
- [x] Logback appender는 prod 프로파일에서만 Sentry로 전송하고, `ERROR` 이상은 이벤트로, `INFO` 이상은 breadcrumb/log로 수집한다.

### Task 3: 운영 배포와 문서

**Files:**
- Modify: `.github/workflows/deploy-prod-ec2.yml`
- Modify: `.env.example`
- Modify: `README.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

- [x] SSM에서 `SENTRY_DSN`, `SENTRY_ENVIRONMENT`, `SENTRY_RELEASE`, `SENTRY_LOGS_ENABLED`를 선택적으로 읽어 `.env`에 기록한다.
- [x] README에 Sentry 운영 파라미터와 수집 범위를 문서화한다.
- [x] `./gradlew test`와 `git diff --check`로 검증한다.
