# Swagger Server URL Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Swagger/OpenAPI 문서에 운영 백엔드 도메인 `https://saynow.p-e.kr`를 서버 URL로 노출한다.

**Architecture:** OpenAPI bean 생성 시 `servers` 목록에 설정 기반 서버 URL을 추가한다. 기본값은 `https://saynow.p-e.kr`이고, 환경별로 `SAYNOW_OPENAPI_SERVER_URL`로 override할 수 있게 한다.

**Tech Stack:** Java 21, Spring Boot 4.0.6, springdoc OpenAPI, MockMvc integration tests.

---

### Task 1: OpenAPI 서버 URL 테스트

**Files:**
- Modify: `src/test/java/com/saynow/OpenApiIntegrationTest.java`

- [ ] **Step 1: Write failing test assertion**

```java
.andExpect(jsonPath("$.servers[0].url").value("https://saynow.p-e.kr"))
```

- [ ] **Step 2: Run RED**

Run: `./gradlew test --tests com.saynow.OpenApiIntegrationTest.exposesOpenApiDocumentForMvpApis`

Expected: FAIL because OpenAPI servers are not configured yet.

### Task 2: Swagger server URL 구현

**Files:**
- Modify: `src/main/java/com/saynow/common/openapi/OpenApiConfig.java`
- Modify: `src/main/resources/application.yml`
- Modify: `.env.example`
- Modify: `README.md`
- Modify: `.github/workflows/deploy-prod-ec2.yml`

- [ ] **Step 1: Inject configured URL**

```java
private final String serverUrl;

public OpenApiConfig(@Value("${saynow.openapi.server-url:https://saynow.p-e.kr}") String serverUrl) {
    this.serverUrl = serverUrl;
}
```

- [ ] **Step 2: Add OpenAPI server**

```java
.servers(List.of(new Server().url(serverUrl)))
```

- [ ] **Step 3: Add configuration defaults and docs**

```yaml
saynow:
  openapi:
    server-url: ${SAYNOW_OPENAPI_SERVER_URL:https://saynow.p-e.kr}
```

### Task 3: 검증과 PR 갱신

**Files:**
- Verify only.

- [ ] **Step 1: Run OpenAPI test**

Run: `./gradlew test --tests com.saynow.OpenApiIntegrationTest`

Expected: PASS.

- [ ] **Step 2: Run full test suite**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 3: Run diff and YAML checks**

Run: `git diff --check`

Run: `ruby -e 'require "yaml"; YAML.load_file(".github/workflows/deploy-prod-ec2.yml"); puts "yaml ok"'`

Expected: PASS.

- [ ] **Step 4: Commit and push**

```bash
git add ...
git commit -m "feat: Swagger 서버 도메인 설정"
git push
```
