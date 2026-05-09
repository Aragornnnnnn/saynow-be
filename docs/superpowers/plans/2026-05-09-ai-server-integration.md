# AI Server Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an optional remote AI server client so SayNow backend can call `POST /api/v1/turn-evaluations` on `http://43.202.146.182:8080` during local runtime while keeping tests deterministic with the existing local dummy client.

**Architecture:** Introduce an `AiPracticeClient` interface and make `PracticeSessionService` depend on the interface. Keep `LocalAiPracticeClient` as the default implementation for tests and normal local development. Add `RemoteAiPracticeClient`, selected by `saynow.ai.client-mode=remote`, which sends `multipart/form-data` with `audio` and JSON `payload` to the AI server and maps the response into the existing `AiTurnEvaluationResult`.

**Tech Stack:** Java 21, Spring Boot 4, Java `HttpClient`, Jackson `ObjectMapper`, JUnit 5, JDK `HttpServer`.

---

### Task 1: Branch And Plan Setup

**Files:**
- Create: `docs/superpowers/plans/2026-05-09-ai-server-integration.md`

- [x] **Step 1: Create a latest-main feature branch**

Run:

```bash
git fetch origin
git checkout -b feat/ai-server-integration origin/main
```

Expected: branch `feat/ai-server-integration` tracks `origin/main`.

- [x] **Step 2: Save this implementation plan**

Create this file under `docs/superpowers/plans/2026-05-09-ai-server-integration.md`.

### Task 2: Remote AI Client Contract Test

**Files:**
- Create: `src/test/java/com/saynow/practice/infrastructure/ai/RemoteAiPracticeClientTest.java`
- Create: `src/main/java/com/saynow/practice/infrastructure/ai/AiPracticeClient.java`
- Create: `src/main/java/com/saynow/practice/infrastructure/ai/AiClientProperties.java`
- Create: `src/main/java/com/saynow/practice/infrastructure/ai/RemoteAiPracticeClient.java`
- Modify: `src/main/java/com/saynow/practice/infrastructure/ai/LocalAiPracticeClient.java`

- [x] **Step 1: Write the failing remote mapping test**

Create `RemoteAiPracticeClientTest` with a test that:

- Builds `RemoteAiPracticeClient` with a local test HTTP server base URL.
- Uses JDK `HttpServer` to record `POST /api/v1/turn-evaluations`.
- Verifies multipart body contains `name="audio"` and `name="payload"`.
- Returns the current AI server response shape:

```json
{
  "transcript": "I want an iced americano",
  "sttConfidence": 0.86,
  "scenarioStatus": "IN_PROGRESS",
  "filledSlots": [
    {
      "slotKey": "drink",
      "slotValue": "americano"
    }
  ],
  "nextQuestion": {
    "questionText": "Would you like that hot or iced?",
    "messageText": null,
    "ttsAudio": "ignored-base64"
  },
  "resultMessage": null
}
```

Expected mapped result:

```java
assertThat(result.transcript()).isEqualTo("I want an iced americano");
assertThat(result.sttConfidence()).isEqualByComparingTo("0.86");
assertThat(result.scenarioStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
assertThat(result.filledSlots()).containsExactly(new AiFilledSlot("drink", "americano"));
assertThat(result.nextPrompt()).isEqualTo(new AiPrompt("Would you like that hot or iced?", null));
assertThat(result.resultMessage()).isNull();
```

- [x] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests com.saynow.practice.infrastructure.ai.RemoteAiPracticeClientTest
```

Expected: compilation fails because `RemoteAiPracticeClient`, `AiPracticeClient`, and `AiClientProperties` do not exist.

### Task 3: AI Client Interface And Implementations

**Files:**
- Create: `src/main/java/com/saynow/practice/infrastructure/ai/AiPracticeClient.java`
- Create: `src/main/java/com/saynow/practice/infrastructure/ai/AiClientProperties.java`
- Create: `src/main/java/com/saynow/practice/infrastructure/ai/RemoteAiPracticeClient.java`
- Modify: `src/main/java/com/saynow/practice/infrastructure/ai/LocalAiPracticeClient.java`
- Modify: `src/main/java/com/saynow/SaynowApplication.java`
- Modify: `src/main/resources/application.yml`

- [x] **Step 1: Add `AiPracticeClient` interface**

```java
package com.saynow.practice.infrastructure.ai;

public interface AiPracticeClient {

    AiTurnEvaluationResult evaluateTurn(AiTurnEvaluationRequest request);
}
```

- [x] **Step 2: Make local client conditional and implement the interface**

`LocalAiPracticeClient` should implement `AiPracticeClient` and be active when `saynow.ai.client-mode=local` or the property is missing.

```java
@Component
@ConditionalOnProperty(prefix = "saynow.ai", name = "client-mode", havingValue = "local", matchIfMissing = true)
public class LocalAiPracticeClient implements AiPracticeClient {
}
```

- [x] **Step 3: Add AI client properties**

`AiClientProperties` should expose:

```java
@ConfigurationProperties(prefix = "saynow.ai")
public record AiClientProperties(
        URI baseUrl,
        String clientMode,
        String turnEvaluationPath
) {
    public AiClientProperties {
        if (turnEvaluationPath == null || turnEvaluationPath.isBlank()) {
            turnEvaluationPath = "/api/v1/turn-evaluations";
        }
    }
}
```

Enable configuration properties in `SaynowApplication` with:

```java
@ConfigurationPropertiesScan
```

- [x] **Step 4: Add remote client**

`RemoteAiPracticeClient` should:

- Be active with `saynow.ai.client-mode=remote`.
- Use Java `HttpClient` and force HTTP/1.1 for FastAPI multipart compatibility.
- Serialize a JSON `payload` string using Jackson.
- Send multipart fields:
  - `audio`: byte array resource with filename and content type.
  - `payload`: JSON string.
- Ignore `ttsAudio` and map `questionText` or `messageText` into `AiPrompt.text`.
- Throw `ApiException(ErrorCode.AI_RESPONSE_INVALID)` for invalid response or HTTP client/server failures.

- [x] **Step 5: Add application defaults**

Update `application.yml`:

```yaml
saynow:
  ai:
    base-url: ${SAYNOW_AI_BASE_URL:http://43.202.146.182:8080}
    client-mode: ${SAYNOW_AI_CLIENT_MODE:local}
    turn-evaluation-path: ${SAYNOW_AI_TURN_EVALUATION_PATH:/api/v1/turn-evaluations}
```

### Task 4: Service Injection Refactor

**Files:**
- Modify: `src/main/java/com/saynow/practice/application/PracticeSessionService.java`

- [x] **Step 1: Replace concrete dependency**

Change:

```java
private final LocalAiPracticeClient aiPracticeClient;
```

to:

```java
private final AiPracticeClient aiPracticeClient;
```

Update constructor import and parameter accordingly.

- [x] **Step 2: Run existing practice API tests**

Run:

```bash
./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest
```

Expected: existing tests still pass because default client mode remains `local`.

### Task 5: Verification And Runtime Smoke Test

**Files:**
- No production file changes beyond Tasks 2-4.

- [x] **Step 1: Run full test suite**

Run:

```bash
./gradlew test
```

Expected: all tests pass.

- [x] **Step 2: Verify AI server health**

Run:

```bash
curl -sS http://43.202.146.182:8080/health
```

Expected:

```json
{"status":"ok"}
```

- [ ] **Step 3: Start backend in remote AI mode**

Run:

```bash
SAYNOW_AI_CLIENT_MODE=remote ./gradlew :bootRun --args='--spring.profiles.active=local'
```

Expected: backend starts and selects `RemoteAiPracticeClient`.

Actual: blocked because local PostgreSQL is not running at `localhost:5432`.

- [x] **Step 4: Submit a turn through backend session API to the remote AI server**

Run:

```bash
SAYNOW_REMOTE_AI_SMOKE_TEST=true ./gradlew test --tests com.saynow.practice.RemoteAiServerApiSmokeTest
```

Actual: after the AI server fix and the backend HTTP/1.1 multipart fix, the smoke test passed against `http://43.202.146.182:8080`.

### Task 6: Commit

**Files:**
- Stage all files touched by Tasks 2-5.

- [x] **Step 1: Review diff**

Run:

```bash
git diff --stat
git diff --check
```

Expected: no whitespace errors; diff limited to AI client integration.

- [ ] **Step 2: Commit**

Branch is `feat/ai-server-integration`, so commit message should be:

```bash
git commit -m "ai-server-integration feat: AI 서버 턴 평가 클라이언트 추가"
```
