# AI Inner Thought Runtime Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AI next-question 속마음 계약과 session-feedback 개선 표현 분리 계약을 BE 런타임에 반영한다.

**Architecture:** `next-question` 전용 scenario 요청 DTO에 `counterpartRole`을 추가해 다른 AI endpoint 계약 오염을 피한다. AI가 반환한 `innerThought`와 `innerThoughtType`은 방금 제출된 `SessionTurn`에 저장하고 `submittedTurn` 응답으로 내려준다. 최종 피드백은 `NEEDS_IMPROVEMENT`에서 `correctionExpression`/`correctionReason`을 분리 저장하고, `GOOD`은 기존 `feedbackDetail`/`benchmarkMessage` 흐름을 유지한다.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, Flyway, MockMvc, JDK HttpServer contract tests.

---

### Task 1: RED Tests

**Files:**
- Modify: `src/test/java/com/saynow/session/infrastructure/ai/RemoteAiConversationClientTest.java`
- Modify: `src/test/java/com/saynow/scenario/ScenarioFlowIntegrationTest.java`
- Modify: `src/test/java/com/saynow/OpenApiIntegrationTest.java`

- [x] Verify `next-question` request sends `scenario.counterpartRole` and maps `innerThought` / `innerThoughtType`.
- [x] Verify utterance submit persists and returns submitted turn inner thought.
- [x] Verify final feedback returns split `correctionExpression` and `correctionReason`.
- [x] Verify OpenAPI examples expose the new FE-BE fields.

### Task 2: Next Question Runtime

**Files:**
- Create: `src/main/java/com/saynow/session/infrastructure/ai/AiNextQuestionScenarioContext.java`
- Create: `src/main/java/com/saynow/session/infrastructure/ai/InnerThoughtType.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/AiNextQuestionRequest.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/AiNextQuestionResponse.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/RemoteAiConversationClient.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/LocalAiConversationClient.java`
- Modify: `src/main/java/com/saynow/session/application/SessionService.java`
- Modify: `src/main/java/com/saynow/session/domain/SessionTurn.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/SessionTurnRepository.java`
- Modify: `src/main/java/com/saynow/session/api/dto/SubmittedTurnResponse.java`

- [x] Add `counterpartRole` only to the next-question scenario context.
- [x] Validate and map AI `innerThought` and `innerThoughtType`.
- [x] Save generated inner thought on the submitted turn.
- [x] Save a default opening thought on the first turn at session start.
- [x] Return submitted turn inner thought in `UserUtteranceResponse`.

### Task 3: Final Feedback Runtime

**Files:**
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/AiSessionTurnFeedbackResponse.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/RemoteAiConversationClient.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/LocalAiConversationClient.java`
- Modify: `src/main/java/com/saynow/feedback/domain/TurnFeedback.java`
- Modify: `src/main/java/com/saynow/feedback/application/FeedbackService.java`
- Modify: `src/main/java/com/saynow/feedback/api/dto/TurnFeedbackResponse.java`

- [x] Add `correctionExpression` and `correctionReason` to the AI DTO.
- [x] Enforce `GOOD` and `NEEDS_IMPROVEMENT` conditional validation.
- [x] Persist split correction fields.
- [x] Return split correction fields to FE.

### Task 4: Verification

**Commands:**
- `./gradlew test --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.OpenApiIntegrationTest`
- `./gradlew test`
- `git diff --check`
- `git status --short`

- [x] Focused tests pass.
- [x] Full tests pass.
- [x] Diff has no whitespace errors.
- [x] Commit one runtime-contract change.
