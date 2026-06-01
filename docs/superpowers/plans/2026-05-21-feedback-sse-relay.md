# Feedback SSE Relay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 프론트가 `POST /api/v1/sessions/{sessionId}/feedback/stream`만 호출해도 백엔드가 AI 서버의 피드백 SSE를 받아 순서대로 중계하고, `done` 수신 후 기존 피드백 테이블에 저장한다.

**Architecture:** 기존 동기 피드백 생성 API는 유지한다. 새 스트림 경로는 기존 세션 검증과 AI 요청 DTO를 재사용하고, AI SSE 이벤트는 `AiFeedbackStreamClient`가 `Flux<AiFeedbackStreamEvent>`로 노출한다. 컨트롤러 응답은 MVC `StreamingResponseBody`로 직접 `text/event-stream` 형식을 쓰며, 정상 종료 전 실패한 스트림은 저장하지 않는다.

**Tech Stack:** Java 21, Spring Boot MVC, Spring Data JPA, Reactor `Flux`, Spring `WebClient`, MockMvc 통합 테스트.

---

### Task 1: RED 테스트 추가

**Files:**
- Create: `src/test/java/com/saynow/feedback/FeedbackStreamIntegrationTest.java`
- Modify: `checklist.md`
- Modify: `context-notes.md`

- [ ] 정상 스트림이 `summary`, `turnFeedback`, `done` 순서로 내려오고 저장되는 통합 테스트를 추가한다.
- [ ] AI 서버 `error` 이벤트가 그대로 내려오고 저장되지 않는 통합 테스트를 추가한다.
- [ ] AI SSE 연결 실패가 백엔드 `error` 이벤트로 내려오는 통합 테스트를 추가한다.
- [ ] 기존 동기 피드백 API가 계속 동작하는 회귀 테스트를 추가한다.
- [ ] `./gradlew test --tests com.saynow.feedback.FeedbackStreamIntegrationTest`를 실행해 실패를 확인한다.

### Task 2: AI 스트림 클라이언트 계약 추가

**Files:**
- Create: `src/main/java/com/saynow/session/infrastructure/ai/AiFeedbackStreamClient.java`
- Create: `src/main/java/com/saynow/session/infrastructure/ai/AiFeedbackStreamEvent.java`
- Create: `src/main/java/com/saynow/session/infrastructure/ai/AiFeedbackStreamException.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/LocalAiConversationClient.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/RemoteAiConversationClient.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/AiClientProperties.java`
- Modify: `src/main/resources/application.yml`
- Modify: `build.gradle.kts`

- [ ] `spring-boot-starter-webflux`를 추가해 `WebClient`와 Reactor 타입을 사용한다.
- [ ] 스트림 이벤트를 `event` 이름과 raw JSON `data` 문자열로 표현한다.
- [ ] 로컬 클라이언트는 동기 피드백 결과를 `summary`, `turnFeedback`, `done` 이벤트로 변환한다.
- [ ] 원격 클라이언트는 `/api/v1/conversation/feedback/stream`에 JSON body와 `Accept: text/event-stream`으로 요청한다.
- [ ] 4xx, 5xx, timeout, 연결 실패는 `AiFeedbackStreamException`으로 변환한다.

### Task 3: 백엔드 SSE relay와 저장 처리 추가

**Files:**
- Modify: `src/main/java/com/saynow/feedback/application/FeedbackService.java`
- Modify: `src/main/java/com/saynow/feedback/api/FeedbackController.java`

- [ ] 기존 세션 소유자, 완료 가능 상태, 중복 피드백 검증을 스트림 API에도 적용한다.
- [ ] AI 요청 body는 기존 동기 피드백과 동일하게 구성한다.
- [ ] `summary`와 `turnFeedback` 이벤트는 프론트에 즉시 relay하고 내부 accumulator에 보관한다.
- [ ] AI `error` 이벤트는 relay 후 저장 없이 종료한다.
- [ ] `done` 수신 시 summary와 모든 answered turn 피드백이 모였는지 검증하고 저장한다.
- [ ] AI 연결 실패, timeout, invalid event는 백엔드 `error` 이벤트로 반환한다.

### Task 4: 검증과 커밋

**Files:**
- Modify: `checklist.md`
- Modify: `context-notes.md`

- [ ] `./gradlew test --tests com.saynow.feedback.FeedbackStreamIntegrationTest`를 실행한다.
- [ ] `./gradlew test`를 실행한다.
- [ ] `git diff --check`와 `git status --short`로 변경 범위를 확인한다.
- [ ] 의미 있는 한 단위로 커밋한다.
