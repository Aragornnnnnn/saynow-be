# AI Next Question DB Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AI next-question 속마음 계약과 turn-feedback 개선 표현 분리 계약을 저장할 수 있도록 DB 구조를 먼저 확장한다.

**Architecture:** 현재 브랜치에는 `session_message` 테이블이 없고 대화 메시지 row는 `session_turns`가 담당한다. 따라서 속마음 저장 컬럼은 `session_turns`에 추가하고, AI 요청용 상대 역할은 `scenarios`에 추가한다. 턴 피드백 개선 표현 분리는 기존 `turn_feedbacks.feedback_detail`을 GOOD 설명용으로 유지하면서 `correction_expression`, `correction_reason`을 추가한다.

**Tech Stack:** Java 21, Spring Boot, JPA, Flyway, PostgreSQL, H2 PostgreSQL mode tests.

---

### Task 1: Schema Contract Test

**Files:**
- Modify: `src/test/java/com/saynow/scenario/ScenarioSchemaIntegrationTest.java`

- [x] Add a failing schema test that verifies `scenarios.counterpart_role`, `session_turns.inner_thought`, `session_turns.inner_thought_type`, `turn_feedbacks.correction_expression`, and `turn_feedbacks.correction_reason`.
- [x] Verify the focused schema test fails before migration exists.

### Task 2: Flyway Migration

**Files:**
- Create: `src/main/resources/db/migration/V16__add_ai_inner_thought_contract.sql`

- [x] Add nullable `session_turns.inner_thought` and `session_turns.inner_thought_type`.
- [x] Add `scenarios.counterpart_role`, backfill existing scenarios to `friend`, then make it NOT NULL.
- [x] Add nullable `turn_feedbacks.correction_expression` and `turn_feedbacks.correction_reason`.
- [x] Drop NOT NULL from `turn_feedbacks.feedback_detail` so NEEDS_IMPROVEMENT rows can store split correction fields without forcing mixed detail text.

### Task 3: Entity Mapping

**Files:**
- Modify: `src/main/java/com/saynow/scenario/domain/Scenario.java`
- Modify: `src/main/java/com/saynow/session/domain/SessionTurn.java`
- Modify: `src/main/java/com/saynow/feedback/domain/TurnFeedback.java`

- [x] Map only the new DB columns needed by later contract work.
- [x] Avoid service or API behavior changes in this DB-first step.

### Task 4: Verification

**Commands:**
- `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest`
- `git diff --check`
- `git status --short`

- [x] Focused schema test passes.
- [x] Diff has no whitespace errors.
- [x] Changed files match the DB-first scope.
