# Session NPS API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 특정 사용자가 특정 완료 세션에 대해 1~5점 NPS 평가를 한 번 제출할 수 있는 API를 만든다.

**Architecture:** `nps` 기능 패키지를 새로 두고 컨트롤러, 서비스, 엔티티, 저장소를 분리한다. 세션 소유권과 완료 상태는 기존 `Session` 도메인을 조회해 검증하고, NPS 데이터는 별도 `session_nps_responses` 테이블에 저장한다.

**Tech Stack:** Java 21, Spring Boot, Spring MVC, Spring Data JPA, Flyway, PostgreSQL, H2 PostgreSQL mode tests.

---

### Task 1: 작업 기록 준비

**Files:**
- Modify: `checklist.md`
- Modify: `context-notes.md`

- [ ] 체크리스트에 NPS API 작업 항목을 추가한다.
- [ ] 컨텍스트 노트에 API 계약과 저장 정책을 기록한다.

### Task 2: RED 테스트 작성

**Files:**
- Create: `src/test/java/com/saynow/nps/SessionNpsApiIntegrationTest.java`
- Create: `src/test/java/com/saynow/nps/SessionNpsSchemaIntegrationTest.java`
- Modify: `src/test/java/com/saynow/OpenApiIntegrationTest.java`

- [ ] 성공 제출은 `201 Created`, `data: null`을 반환한다.
- [ ] `score`가 1~5 범위를 벗어나면 `INVALID_REQUEST`를 반환한다.
- [ ] 진행 중인 세션이면 `SESSION_IN_PROGRESS`를 반환한다.
- [ ] 다른 사용자의 세션이면 `FORBIDDEN`을 반환한다.
- [ ] 같은 사용자가 같은 세션에 다시 제출하면 `NPS_ALREADY_SUBMITTED`를 반환한다.
- [ ] schema 테스트는 `session_nps_responses` 테이블과 `user_id`, `session_id`, `score`, `low_score_reason` 컬럼을 검증한다.
- [ ] OpenAPI 테스트는 `/api/v1/sessions/{sessionId}/nps` 경로와 성공 예시의 `data: null`을 검증한다.

### Task 3: GREEN 구현

**Files:**
- Create: `src/main/java/com/saynow/nps/api/NpsController.java`
- Create: `src/main/java/com/saynow/nps/api/dto/NpsSubmitRequest.java`
- Create: `src/main/java/com/saynow/nps/application/NpsService.java`
- Create: `src/main/java/com/saynow/nps/domain/SessionNpsResponse.java`
- Create: `src/main/java/com/saynow/nps/infrastructure/SessionNpsResponseRepository.java`
- Create: `src/main/resources/db/migration/V6__create_session_nps_responses.sql`
- Modify: `src/main/java/com/saynow/common/exception/ErrorCode.java`
- Modify: `src/main/java/com/saynow/common/openapi/OpenApiResponseCustomizer.java`

- [ ] `NPS_ALREADY_SUBMITTED` 오류 코드를 추가한다.
- [ ] Flyway migration으로 `session_nps_responses` 테이블과 `(user_id, session_id)` unique 제약을 추가한다.
- [ ] JPA 엔티티와 저장소를 추가한다.
- [ ] 서비스에서 세션 존재, 소유권, 완료 상태, 중복 제출, 점수 범위, 고점수 사유 전송을 검증한다.
- [ ] 컨트롤러에서 `POST /api/v1/sessions/{sessionId}/nps`를 `201 Created`와 `data: null`로 반환한다.
- [ ] OpenAPI customizer에 성공과 오류 예시를 추가한다.

### Task 4: 검증과 배포

**Files:**
- All changed files.

- [ ] RED 테스트가 먼저 실패했는지 확인한다.
- [ ] 관련 테스트를 통과시킨다.
- [ ] `./gradlew test` 전체 테스트를 통과시킨다.
- [ ] `git diff --check`를 통과시킨다.
- [ ] 커밋 후 `develop`으로 push한다.
- [ ] `Deploy Dev EC2` workflow를 실행한다.
- [ ] `https://dev-saynow.p-e.kr/actuator/health`가 `UP`인지 확인한다.
