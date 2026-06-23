# Roommate Scenario Seed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 기존 Free Talk 시나리오 seed를 교환학생 룸메이트 중심 카테고리와 시나리오 데이터로 교체한다.

**Architecture:** Flyway migration을 새로 추가해 배포된 기존 migration은 수정하지 않는다. API는 기존 시나리오 목록, 세션 시작, 발화 제출 응답 구조를 유지하고 seed 데이터와 OpenAPI 예시만 새 내용에 맞춘다.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, PostgreSQL, Flyway, MockMvc integration tests.

---

### Task 1: Seed Contract Test

**Files:**
- Modify: `src/test/java/com/saynow/scenario/ScenarioSchemaIntegrationTest.java`
- Modify: `src/test/java/com/saynow/scenario/ScenarioFlowIntegrationTest.java`

- [x] **Step 1: Update seed expectations**

`ScenarioSchemaIntegrationTest.freeTalkScenariosAreSeededWithFourFixedQuestionsEach`를 새 카테고리와 룸메이트 질문 기대값으로 바꾼다. 카테고리는 `룸메이트`, `수업`, `여행` 세 개만 기대하고, `룸메이트`에는 시나리오 A/B/C를 기대한다.

- [x] **Step 2: Verify RED**

Run: `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest`.
Expected: FAIL because the current seed still returns `Free Talk`, `Airport`, `Hotel`, `Restaurant`.

### Task 2: Migration

**Files:**
- Create: `src/main/resources/db/migration/V17__replace_free_talk_with_roommate_scenarios.sql`

- [x] **Step 1: Add migration**

Add a Flyway migration that updates categories 1~3 to `룸메이트`, `수업`, `여행`, removes category 4, moves scenarios 1~3 into `룸메이트`, sets `counterpart_role='roommate'`, and updates scenario questions. Scenarios A/B/C have 4 questions each.

- [x] **Step 2: Verify GREEN**

Run: `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest`.
Expected: PASS.

### Task 3: API Example Sync

**Files:**
- Modify: `src/main/java/com/saynow/common/openapi/OpenApiResponseCustomizer.java`
- Modify: `src/test/java/com/saynow/OpenApiIntegrationTest.java`
- Modify: `src/test/java/com/saynow/scenario/ScenarioFlowIntegrationTest.java`

- [x] **Step 1: Update OpenAPI and flow examples**

Change example category, first scenario, first question, session start total count, and feedback original question to the new roommate scenario seed.

- [x] **Step 2: Verify focused tests**

Run: `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.scenario.ScenarioSchemaIntegrationTest --tests com.saynow.OpenApiIntegrationTest`.
Expected: PASS.

### Task 4: Final Verification

**Files:**
- All touched files.

- [x] **Step 1: Run full regression**

Run: `./gradlew test`.
Expected: PASS.

- [x] **Step 2: Check whitespace and status**

Run: `git diff --check` and `git status --short`.
Expected: no whitespace errors and only intended files changed.
