# Closing Message Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 마지막 사용자 발화 후 FE 하드코딩 종료 문구 대신 AI `closing-message` 응답을 기존 `nextTurn` shape로 반환하고 DB에 AI 턴으로 저장한다.

**Architecture:** 기존 진행 중 턴은 `next-question`을 그대로 호출한다. 다음 고정 질문이 없는 종료 분기에서는 `closing-message`를 호출하고, 응답의 마지막 AI 멘트를 `session_turns.sequence = totalQuestionCount + 1` row로 저장한다. 최종 피드백은 기존처럼 사용자 발화가 있는 고정 질문 턴만 대상으로 삼아 종료 멘트 AI 턴을 피드백 대상에서 제외한다.

**Tech Stack:** Java 21, Spring Boot, Spring MVC, Spring Data JPA, Flyway existing schema, MockMvc integration tests, JDK HttpServer remote-client tests.

---

### Task 1: AI Client Closing Contract

**Files:**
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/AiConversationClient.java`
- Create: `src/main/java/com/saynow/session/infrastructure/ai/AiClosingMessageRequest.java`
- Create: `src/main/java/com/saynow/session/infrastructure/ai/AiClosingMessageResponse.java`
- Create: `src/main/java/com/saynow/session/infrastructure/ai/ClosingReason.java`
- Create: `src/main/java/com/saynow/session/infrastructure/ai/GoalCompletionStatus.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/AiClientProperties.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/RemoteAiConversationClient.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/LocalAiConversationClient.java`
- Test: `src/test/java/com/saynow/session/infrastructure/ai/RemoteAiConversationClientTest.java`

- [ ] Add a RED test that posts to `/api/v1/conversation/closing-message`, asserts request fields `sessionId`, `submittedTurnId`, `submittedSequence`, `scenario.counterpartRole`, `currentTurn.userUtterance`, `closingReason`, `goalCompletionStatus`, and maps `aiMessage`, `translatedMessage`, `innerThought`, `innerThoughtType`.
- [ ] Run `./gradlew test --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest` and verify it fails because closing-message types/client method do not exist.
- [ ] Add the closing DTOs, enums, client method, properties default path, remote response validation, and local deterministic response.
- [ ] Re-run the focused test and verify it passes.

### Task 2: Submit Utterance Closing Flow

**Files:**
- Modify: `src/main/java/com/saynow/session/application/SessionService.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/SessionTurnRepository.java`
- Test: `src/test/java/com/saynow/scenario/ScenarioFlowIntegrationTest.java`

- [ ] Add a RED integration test that the fourth answer calls `closing-message`, returns `nextTurn` with `sequence=5`, `completed=true`, and maps closing `innerThought` to `submittedTurn`.
- [ ] Assert DB stores the closing AI turn as `session_turns.sequence=5`, `ai_question=<closing aiMessage>`, `translated_question=<translatedMessage>`, and `user_utterance IS NULL`.
- [ ] Assert the final user turn stores closing `innerThought` and `innerThoughtType`.
- [ ] Run `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest` and verify it fails against the current `nextTurn=null` behavior.
- [ ] Implement closing-message call in the no-next-question branch, then persist the closing AI turn in the same write transaction.
- [ ] Update pending-turn lookup so the saved closing AI row is not treated as a user-answerable pending turn.
- [ ] Re-run the focused test and verify it passes.

### Task 3: Feedback Excludes Closing AI Turn

**Files:**
- Modify: `src/main/java/com/saynow/feedback/application/FeedbackService.java`
- Test: `src/test/java/com/saynow/scenario/ScenarioFlowIntegrationTest.java`

- [ ] Extend the integration test to create final feedback after a closing AI turn exists and assert `turnFeedbacks.length() == 4`.
- [ ] Assert `AiSessionFeedbackRequest.expectedTurnIds` contains only the four answered user turns and not the closing AI turn.
- [ ] Run the focused test and verify it fails if all `session_turns` rows are included.
- [ ] Filter feedback readiness, AI request turn IDs, persistence, and response assembly to answered turns only.
- [ ] Re-run focused tests and verify they pass.

### Task 4: OpenAPI Example And Regression Verification

**Files:**
- Modify: `src/main/java/com/saynow/common/openapi/OpenApiResponseCustomizer.java`
- Test: `src/test/java/com/saynow/OpenApiIntegrationTest.java`

- [ ] Add or update the utterance success example so completed responses still include `nextTurn` with the final AI message and `progress.currentSequence=5`.
- [ ] Run `./gradlew test --tests com.saynow.OpenApiIntegrationTest` and verify the example contract passes.
- [ ] Run `./gradlew test --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.OpenApiIntegrationTest`.
- [ ] Run `./gradlew test`.
- [ ] Run `git diff --check`.
