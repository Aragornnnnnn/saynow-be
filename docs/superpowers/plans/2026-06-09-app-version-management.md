# App Version Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** DB에 저장된 앱 버전 정책으로 클라이언트 업데이트가 강제, 소프트, 없음 중 무엇인지 판단하는 공개 API를 만든다.

**Architecture:** `appversion` 기능 패키지를 새로 두고 Controller, Service, Entity, Repository, DTO를 분리한다. 서버는 문자열 버전 비교를 하지 않고 `platform`별 최신 active row의 `buildNumber`와 `minimumSupportedBuildNumber`를 기준으로 `FORCE`, `SOFT`, `NONE`을 계산한다.

**Tech Stack:** Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, Flyway, PostgreSQL, H2 PostgreSQL mode integration tests.

---

### Task 1: 작업 기록 준비

**Files:**
- Modify: `checklist.md`
- Modify: `context-notes.md`
- Create: `docs/superpowers/plans/2026-06-09-app-version-management.md`

- [ ] 앱 버전 관리 체크리스트를 추가한다.
- [ ] 컨텍스트 노트에 한 테이블 정책, build number 비교 기준, 공개 API 결정을 기록한다.

### Task 2: RED 테스트 작성

**Files:**
- Create: `src/test/java/com/saynow/appversion/AppVersionApiIntegrationTest.java`
- Create: `src/test/java/com/saynow/appversion/AppVersionSchemaIntegrationTest.java`
- Modify: `src/test/java/com/saynow/OpenApiIntegrationTest.java`

- [ ] 강제 업데이트 테스트를 작성한다.

```java
mockMvc.perform(get("/api/v1/app-versions/check")
                .param("platform", "IOS")
                .param("buildNumber", "14")
                .param("versionName", "1.3.0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("FORCE"))
        .andExpect(jsonPath("$.data.reason").value("이전 버전에서 로그인 안정성 문제가 있어 업데이트가 필요합니다."));
```

- [ ] 소프트 업데이트 테스트를 작성한다.

```java
mockMvc.perform(get("/api/v1/app-versions/check")
                .param("platform", "IOS")
                .param("buildNumber", "16"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("SOFT"))
        .andExpect(jsonPath("$.data.reason").value("새로운 대화 품질 개선이 포함되어 있습니다."));
```

- [ ] 최신 버전 테스트를 작성한다.

```java
mockMvc.perform(get("/api/v1/app-versions/check")
                .param("platform", "IOS")
                .param("buildNumber", "18"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("NONE"))
        .andExpect(jsonPath("$.data.reason").value(nullValue()));
```

- [ ] 스키마 테스트는 `app_versions` 테이블과 핵심 컬럼을 검증한다.
- [ ] OpenAPI 테스트는 `/api/v1/app-versions/check` 경로와 성공 예시를 검증한다.
- [ ] `./gradlew test --tests com.saynow.appversion.AppVersionApiIntegrationTest --tests com.saynow.appversion.AppVersionSchemaIntegrationTest --tests com.saynow.OpenApiIntegrationTest`가 새 타입 부재로 실패하는지 확인한다.

### Task 3: GREEN 구현

**Files:**
- Create: `src/main/resources/db/migration/V14__create_app_versions.sql`
- Create: `src/main/java/com/saynow/appversion/domain/AppPlatform.java`
- Create: `src/main/java/com/saynow/appversion/domain/AppUpdateType.java`
- Create: `src/main/java/com/saynow/appversion/domain/AppVersion.java`
- Create: `src/main/java/com/saynow/appversion/infrastructure/AppVersionRepository.java`
- Create: `src/main/java/com/saynow/appversion/application/AppVersionService.java`
- Create: `src/main/java/com/saynow/appversion/api/AppVersionController.java`
- Create: `src/main/java/com/saynow/appversion/api/dto/AppVersionCheckResponse.java`
- Modify: `src/main/java/com/saynow/common/openapi/OpenApiResponseCustomizer.java`

- [ ] Flyway migration으로 `app_versions` 테이블, platform/build unique 제약, active latest 조회용 index를 만든다.
- [ ] `AppPlatform.from(String)`에서 `IOS`, `ANDROID`만 허용하고 그 외 값은 `VALIDATION_FAILED`로 처리한다.
- [ ] `AppVersionService.check(platform, buildNumber)`에서 최신 active row를 찾고 `FORCE`, `SOFT`, `NONE`을 계산한다.
- [ ] active row가 없으면 현재 요청값 기준 `NONE`을 반환해 앱 실행을 막지 않는다.
- [ ] Controller는 `GET /api/v1/app-versions/check`를 공개 API로 제공한다.
- [ ] OpenAPI customizer에 성공 예시와 `VALIDATION_FAILED`, `INTERNAL_SERVER_ERROR` 예시를 추가한다.

### Task 4: 검증과 커밋

**Files:**
- All changed files.

- [ ] focused 테스트를 통과시킨다.
- [ ] `./gradlew test` 전체 테스트를 통과시킨다.
- [ ] `git diff --check`를 통과시킨다.
- [ ] `git status --short`로 변경 범위를 확인한다.
- [ ] 브랜치가 `develop`이므로 커밋 메시지는 `develop`에 맞춰 한글 설명으로 작성한다.
