# 3차 MVP BE 성능 안정화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 3차 MVP BE의 AI 연동 경로에서 DB 트랜잭션 유지 시간을 줄이고, 반복 조회 경로의 불필요한 DB I/O를 줄인다.

**Architecture:** 외부 AI 호출은 DB 트랜잭션 밖에서 수행한다. 정적 시나리오 조회와 사용자별 진행도 조회는 분리하고, 피드백 재조회와 턴 조회는 더 좁은 repository 메서드로 정리한다.

**Tech Stack:** Java 21, Spring Boot, Spring MVC, Spring Data JPA, TransactionTemplate, H2/PostgreSQL mode tests.

---

### Task 1: GitHub Sub-Issue 생성과 연결

**Files:**
- Modify: `checklist.md`
- Modify: `context-notes.md`

- [ ] #39 `AI 원격 호출 트랜잭션 분리`를 생성하고 #20 하위 이슈로 연결한다.
- [ ] #40 `시나리오 목록 조회 최적화`를 생성하고 #20 하위 이슈로 연결한다.
- [ ] #41 `최종 피드백 재조회 경로 최적화`를 생성하고 #20 하위 이슈로 연결한다.
- [ ] #42 `턴 조회와 피드백 저장 경량화`를 생성하고 #20 하위 이슈로 연결한다.

### Task 2: 트랜잭션 분리

**Files:**
- Modify: `src/main/java/com/saynow/session/application/SessionService.java`
- Modify: `src/main/java/com/saynow/feedback/application/FeedbackService.java`

- [ ] `submitUtterance`를 context 조회, AI 호출, DB 반영 단계로 분리한다.
- [ ] `createFeedback`을 context 조회, AI 호출, DB 저장 단계로 분리한다.
- [ ] AI 호출 실패 시 기존처럼 DB 반영이 일어나지 않는 구조를 유지한다.

### Task 3: 조회와 저장 경량화

**Files:**
- Modify: `src/main/java/com/saynow/scenario/application/ScenarioService.java`
- Modify: `src/main/java/com/saynow/scenario/infrastructure/ScenarioQuestionRepository.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/SessionTurnRepository.java`
- Modify: `src/main/java/com/saynow/feedback/infrastructure/TurnFeedbackRepository.java`

- [ ] 시나리오 첫 질문 조회를 1번 질문만 가져오는 메서드로 좁힌다.
- [ ] 미응답 턴 조회를 첫 건 조회 방식으로 변경한다.
- [ ] 기존 피드백 재조회에 필요한 턴별 피드백을 턴까지 함께 가져온다.
- [ ] 턴별 피드백 저장은 `saveAll`로 정리한다.

### Task 4: 검증과 이슈 완료 처리

**Files:**
- Modify: `checklist.md`
- Modify: `context-notes.md`

- [ ] 관련 테스트를 먼저 실행해 회귀 여부를 확인한다.
- [ ] 전체 `./gradlew test`를 실행한다.
- [ ] `git diff --check`와 `git status --short`를 확인한다.
- [ ] 완료된 GitHub sub-issue 체크리스트를 체크하고 이슈를 close 상태로 변경한다.
