# CORS Allowed Origins Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** SayNow 프론트 운영 도메인과 로컬 개발 도메인에서 백엔드 API를 호출할 수 있도록 CORS를 명시적으로 허용한다.

**Architecture:** `saynow.web.cors.allowed-origins` 설정을 Spring Boot configuration properties로 바인딩하고, Spring Security의 `cors` 필터를 활성화한다. Bearer token 기반 인증이므로 credentials는 비활성화하고, `Authorization`, `Content-Type` 헤더와 API 메서드를 허용한다.

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring Security, Spring MVC, MockMvc integration tests.

---

### Task 1: CORS 통합 테스트 추가

**Files:**
- Modify: `src/test/java/com/saynow/auth/SecurityAuthenticationIntegrationTest.java`

- [ ] **Step 1: Write failing preflight test**

```java
@Test
void allowsConfiguredFrontendOriginsForCorsPreflight() throws Exception {
    mockMvc.perform(options("/api/v1/sessions")
                    .header(HttpHeaders.ORIGIN, "https://saynow-fe-web.vercel.app")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://saynow-fe-web.vercel.app"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, nullValue()));
}
```

- [ ] **Step 2: Write failing localhost origin test**

```java
@Test
void allowsLocalhostFrontendOriginForCorsPreflight() throws Exception {
    mockMvc.perform(options("/api/v1/sessions")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
}
```

- [ ] **Step 3: Run RED**

Run: `./gradlew test --tests com.saynow.auth.SecurityAuthenticationIntegrationTest`

Expected: FAIL because no CORS configuration is registered.

### Task 2: CORS configuration 구현

**Files:**
- Create: `src/main/java/com/saynow/common/web/CorsProperties.java`
- Modify: `src/main/java/com/saynow/auth/security/AuthSecurityConfig.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Add properties class**

```java
// 브라우저 CORS 허용 origin 목록을 바인딩하는 설정
package com.saynow.common.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "saynow.web.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null) {
            allowedOrigins = new ArrayList<>();
        }
    }
}
```

- [ ] **Step 2: Wire Spring Security CORS**

```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsProperties.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
    configuration.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

- [ ] **Step 3: Add default origins**

```yaml
saynow:
  web:
    cors:
      allowed-origins: ${SAYNOW_CORS_ALLOWED_ORIGINS:https://saynow-fe-web.vercel.app,http://localhost:3000}
```

### Task 3: 검증과 커밋

**Files:**
- Verify only.

- [ ] **Step 1: Run targeted security test**

Run: `./gradlew test --tests com.saynow.auth.SecurityAuthenticationIntegrationTest`

Expected: PASS.

- [ ] **Step 2: Run full test suite**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 3: Run diff check**

Run: `git diff --check`

Expected: no output.

- [ ] **Step 4: Commit**

```bash
git add checklist.md context-notes.md docs/superpowers/plans/2026-05-12-cors-allowed-origins.md src/main/java/com/saynow/common/web/CorsProperties.java src/main/java/com/saynow/auth/security/AuthSecurityConfig.java src/main/resources/application.yml src/test/java/com/saynow/auth/SecurityAuthenticationIntegrationTest.java
git commit -m "cors-allowed-origins feat: 프론트 CORS origin 허용"
```
