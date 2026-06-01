# Session Guide Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a stateless guide-mode API that answers English-learning questions during an active session without storing guide turns or including them in final feedback.

**Architecture:** The frontend calls a session-scoped backend endpoint with only `question`. The backend validates session ownership/status, blocks obvious non-English-learning or prompt-injection requests locally, and sends trusted scenario context from the session to the AI server. Guide requests are not persisted.

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring MVC, MockMvc integration tests, Java HttpClient, Gradle.

---

### File Structure

- Modify `src/main/java/com/saynow/session/api/SessionController.java` to expose `POST /api/v1/sessions/{sessionId}/guide`.
- Create `src/main/java/com/saynow/session/api/dto/GuideQuestionRequest.java` for the frontend request.
- Create `src/main/java/com/saynow/session/api/dto/GuideQuestionResponse.java` for the frontend response.
- Modify `src/main/java/com/saynow/session/application/SessionService.java` to validate the session, apply the local guard, and call the AI client.
- Modify `src/main/java/com/saynow/session/infrastructure/ai/AiConversationClient.java` to add `generateGuide`.
- Create `src/main/java/com/saynow/session/infrastructure/ai/AiGuideRequest.java` for the backend-to-AI guide payload.
- Create `src/main/java/com/saynow/session/infrastructure/ai/AiGuideResponse.java` for the backend-to-AI guide response.
- Modify `src/main/java/com/saynow/session/infrastructure/ai/AiClientProperties.java` and `src/main/resources/application.yml` to configure the guide path.
- Modify `src/main/java/com/saynow/session/infrastructure/ai/RemoteAiConversationClient.java` and `LocalAiConversationClient.java` to implement guide answer generation.
- Modify `src/main/java/com/saynow/common/exception/ErrorCode.java` to add `AI_GENERATION_FAILED`.
- Modify `src/main/java/com/saynow/common/openapi/OpenApiResponseCustomizer.java` to document guide API responses.
- Test through `src/test/java/com/saynow/scenario/ScenarioFlowIntegrationTest.java`, `src/test/java/com/saynow/session/infrastructure/ai/RemoteAiConversationClientTest.java`, and `src/test/java/com/saynow/OpenApiIntegrationTest.java`.

### Task 1: Frontend API Behavior

**Files:**
- Modify: `src/test/java/com/saynow/scenario/ScenarioFlowIntegrationTest.java`
- Modify: `src/main/java/com/saynow/session/api/SessionController.java`
- Create: `src/main/java/com/saynow/session/api/dto/GuideQuestionRequest.java`
- Create: `src/main/java/com/saynow/session/api/dto/GuideQuestionResponse.java`
- Modify: `src/main/java/com/saynow/session/application/SessionService.java`

- [ ] **Step 1: Write the failing integration test**

Add a test that starts an Airport session, calls `POST /api/v1/sessions/{sessionId}/guide`, asserts the answer, asserts the AI guide request includes scenario context, completes the session, and confirms final feedback still contains only normal session turns.

- [ ] **Step 2: Run RED verification**

Run: `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest`

Expected: fail because the guide endpoint and AI guide contract do not exist.

- [ ] **Step 3: Implement the smallest API path**

Add the request/response DTOs, controller method, and `SessionService.generateGuideAnswer`. Validate blank `question` as `INVALID_REQUEST`, require an owned in-progress session, return a non-persisted response, and keep `SessionTurn` untouched.

- [ ] **Step 4: Run GREEN verification**

Run: `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest`

Expected: pass.

### Task 2: Guardrail and AI Client Contract

**Files:**
- Modify: `src/test/java/com/saynow/scenario/ScenarioFlowIntegrationTest.java`
- Modify: `src/test/java/com/saynow/session/infrastructure/ai/RemoteAiConversationClientTest.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/AiConversationClient.java`
- Create: `src/main/java/com/saynow/session/infrastructure/ai/AiGuideRequest.java`
- Create: `src/main/java/com/saynow/session/infrastructure/ai/AiGuideResponse.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/AiClientProperties.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/RemoteAiConversationClient.java`
- Modify: `src/main/java/com/saynow/session/infrastructure/ai/LocalAiConversationClient.java`
- Modify: `src/main/java/com/saynow/common/exception/ErrorCode.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Write failing contract and guard tests**

Add tests for allowed guide requests, prompt-injection style blocked requests, and remote AI guide request/response mapping for `/api/v1/conversation/guide`.

- [ ] **Step 2: Run RED verification**

Run: `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest`

Expected: fail because `generateGuide`, guide DTOs, and guide path are missing.

- [ ] **Step 3: Implement minimal contract**

Add guide DTO records, `generateGuide`, guide path property, remote mapping, and local deterministic answer. Map AI guide failures to `AI_GENERATION_FAILED`.

- [ ] **Step 4: Run GREEN verification**

Run: `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest`

Expected: pass.

### Task 3: OpenAPI and Final Verification

**Files:**
- Modify: `src/test/java/com/saynow/OpenApiIntegrationTest.java`
- Modify: `src/main/java/com/saynow/common/openapi/OpenApiResponseCustomizer.java`
- Modify: `checklist.md`
- Modify: `context-notes.md`

- [ ] **Step 1: Write failing OpenAPI expectations**

Assert `/api/v1/sessions/{sessionId}/guide` exists and has examples for success, `INVALID_REQUEST`, `SESSION_ALREADY_COMPLETED`, `FORBIDDEN`, `SESSION_NOT_FOUND`, `AUTH_REQUIRED`, and `AI_GENERATION_FAILED`.

- [ ] **Step 2: Run RED verification**

Run: `./gradlew test --tests com.saynow.OpenApiIntegrationTest`

Expected: fail because OpenAPI does not document the guide endpoint.

- [ ] **Step 3: Add OpenAPI examples**

Add the endpoint document in `OpenApiResponseCustomizer` using the same response wrapper style as existing session APIs.

- [ ] **Step 4: Run targeted and full verification**

Run:

```bash
./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.OpenApiIntegrationTest
./gradlew test
git diff --check
```

Expected: all commands pass.

### Self-Review

- Spec coverage: session-scoped frontend API, trusted backend-to-AI context, stateless guide handling, local guardrail, AI contract, and OpenAPI coverage are represented.
- Placeholder scan: no implementation placeholders remain in this plan.
- Type consistency: frontend DTOs use `question` and `answer`; backend-to-AI DTOs use `question`, `scenarioTitle`, `scenarioGoal`, `scenarioSituation`, and `aiRole`.
