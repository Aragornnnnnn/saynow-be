# turnClassification 기반 하트/슬롯 정책 컨텍스트 노트

## 2026-05-30

- 사용자는 AI 서버의 `turnClassification`을 BE 정책 판단의 단일 기준으로 두라고 요청했다.
- 현재 코드 확인 결과 `SessionService.shouldDeductHeart(...)`는 이미 `INVALID_RESPONSE`일 때만 하트를 차감한다.
- 현재 코드 확인 결과 `SessionService.submitUtterance(...)`는 `applyFilledSlots(...)`를 classification과 무관하게 호출한다. 따라서 `INVALID_RESPONSE` 또는 `ASSISTANCE_REQUEST`에 `filledSlots`가 들어오면 슬롯이 잘못 충족될 수 있다.
- 이번 구현은 하트 정책을 새로 확장하지 않고, 슬롯 적용 조건을 `ANSWER`로 제한하는 데 집중한다.
- 세션 실패 정책은 기존처럼 남은 하트가 0 이하가 된 경우 `FAILURE`로 완료한다. 하트는 `INVALID_RESPONSE`에서만 줄기 때문에 실패 누적 기준도 invalid 응답 누적에만 연결된다.
- 테스트는 공항 시나리오 5, 6을 사용한다. 사용자 계획의 세션 159, 160 사례는 실제 DB seed의 고정 세션 ID가 아니라 AI 검증 사례로 보고, BE 통합 테스트에서는 동일 발화와 슬롯명 정책을 시나리오별 세션으로 재현한다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest`를 실행했고, `INVALID_RESPONSE`가 반환한 `boarding_possibility`, `contact_info` 슬롯이 잘못 충족되어 실패했다.
- `SessionService.submitUtterance(...)`에서 `turnClassification == ANSWER`일 때만 `applyFilledSlots(...)`를 호출하도록 변경했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest`를 실행했고 통과했다.
- OpenAPI 검증은 사용자 발화 응답 예시에 `remainingHearts`, `heartDeducted`, `turnClassification`이 모두 포함되는지 확인하도록 보강했다.
- 계약 검증으로 `./gradlew test --tests com.saynow.OpenApiIntegrationTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- Obsidian 문서 `동문서답 턴 분류 개선 기록.md`의 백엔드 후속 작업을 실제 BE 반영 사항, 테스트 결과, 남은 후속 작업으로 교체했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.

---

# AI SSE 피드백 스트림 중계 컨텍스트 노트

# AI 피드백 슬롯 데이터 요청 추가 컨텍스트 노트

## 2026-05-26

- 사용자는 AI `feedback`, `feedback/stream` 요청에도 `slots` 배열을 추가했다고 공유했다.
- `slots`의 각 원소는 기존 `AiSlotStatus`와 같은 `slotName`, `description`, `filled` 구조를 사용한다.
- `next-question` 요청은 이미 `slots[].description`을 포함하고 있으므로, 이번 변경은 피드백 요청 DTO와 피드백 공통 요청 생성 경로에 집중한다.
- `FeedbackService.toAiFeedbackRequest(...)`는 동기 피드백과 SSE 피드백이 같이 쓰는 빌더이므로, 이 경로에서 세션 슬롯 충족 상태와 원본 시나리오 슬롯 설명을 조합한다.
- 프론트 응답 계약과 DB 스키마는 변경하지 않는다.
- RED 검증으로 `./gradlew test --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.feedback.FeedbackStreamIntegrationTest`를 실행했고, `AiFeedbackRequest.slots()`와 새 생성자 인자가 없어 컴파일 실패했다.
- `AiFeedbackRequest`에 `slots`를 추가하고, `FeedbackService.toAiFeedbackRequest(...)`가 `SessionSlotStatus`와 `ScenarioSlot.description`을 조합해 `AiSlotStatus` 목록을 만들도록 변경했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.feedback.FeedbackStreamIntegrationTest`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.

---

# 시나리오 슬롯 설명 필드 추가 컨텍스트 노트

## 2026-05-26

- 사용자는 확정한 슬롯 description 표를 기준으로 DB에 삽입하고 `develop`으로 push하라고 요청했다.
- 기존 V1/V3 migration은 이미 배포됐을 수 있으므로 새 Flyway migration으로 `scenario_slots.description`을 추가한다.
- description은 프론트 시나리오 목록 응답에는 추가하지 않고, AI `next-question` 요청의 `slots[].description`으로만 전달한다.
- `SessionSlotStatus`에는 슬롯 충족 상태만 남기고, description은 원본 `ScenarioSlot`에서 읽어 `AiSlotStatus`에 매핑한다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest`를 실행했고, `AiSlotStatus.description()`과 새 생성자 인자가 없어 컴파일 실패했다.
- `V5__add_scenario_slot_description.sql`로 `scenario_slots.description`을 추가하고 사용자가 확정한 15개 슬롯 설명을 모두 업데이트한 뒤 NOT NULL로 전환했다.
- `ScenarioSlot.description`, `AiSlotStatus.description`을 추가하고, `SessionService`가 원본 `ScenarioSlot`에서 description을 읽어 `next-question` 요청의 `slots[].description`으로 전달하게 했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.
- 커밋 `ea5d516`이 `origin/develop`에 반영된 것을 `git fetch origin develop`과 `git rev-parse`로 확인했다.

---

# 시나리오 AI 역할 필드 추가 컨텍스트 노트

## 2026-05-26

- 사용자는 시나리오 테이블에 `aiRole` 필드를 추가하고, AI 요청 DTO 3개 API에 모두 포함하되 프론트에는 주지 않기로 결정했다.
- AI 요청 3개 API는 `POST /api/v1/conversation/next-question`, `POST /api/v1/conversation/feedback`, `POST /api/v1/conversation/feedback/stream`이다. 백엔드는 `AiNextQuestionRequest`와 공통 `AiFeedbackRequest`에 `aiRole`을 추가하면 세 경로에 반영된다.
- 프론트 응답 DTO인 `ScenarioResponse`에는 `aiRole`을 추가하지 않는다.
- 스키마는 새 Flyway migration으로 `scenarios.ai_role`을 추가한다. 기존 dev DB에 V1~V3가 이미 적용됐을 수 있으므로 기존 migration은 수정하지 않는다.
- seed 값은 카페와 공항 기존 6개 시나리오 모두 채운다. 공항 역할은 입국심사관, 항공사 수하물 서비스 직원, 공항 환승 안내 직원 기준으로 둔다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.feedback.FeedbackStreamIntegrationTest`를 실행했고, `AiNextQuestionRequest`, `AiFeedbackRequest`에 `aiRole` 필드가 없어 컴파일 실패했다.
- `V4__add_scenario_ai_role.sql`로 `scenarios.ai_role`을 추가하고 기존 시나리오 6개의 역할을 채웠다.
- `Scenario.aiRole`을 엔티티에 매핑하고, `AiNextQuestionRequest`와 `AiFeedbackRequest`에 `aiRole`을 추가했다.
- `SessionService`는 next-question 요청에 `session.getScenario().getAiRole()`을 넘기고, `FeedbackService`는 동기 feedback과 SSE feedback이 공유하는 `AiFeedbackRequest`에 같은 값을 넘긴다.
- `ScenarioResponse`에는 `aiRole`을 추가하지 않았고, 통합 테스트에서 시나리오 목록 응답에 `aiRole`이 노출되지 않음을 확인한다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.feedback.FeedbackStreamIntegrationTest`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.

---

# 공항 시나리오 dev 실환경 진행 컨텍스트 노트

## 2026-05-26

- 사용자는 임시 사용자를 만들어 공항 시나리오 1~3단계를 실제 API로 모두 진행하고, 품질 판단이 가능하도록 과정과 결과를 설명해 달라고 요청했다.
- 기준 환경은 사용자가 직전에 `develop` push를 요청한 dev 배포 환경 `https://dev-saynow.p-e.kr`이다.
- 가짜 OIDC 로그인은 dev 환경에서 실제 ID token 검증에 막힐 수 있으므로, 필요하면 dev DB에 임시 사용자를 만들고 백엔드 토큰 서명 방식과 같은 HS256 access token으로 API를 검증한다.
- 검증 결과는 세션별 request/response JSON과 함께 성공 여부, 잠금 해제 흐름, AI 응답 품질 판단 포인트를 나눠 기록한다.
- GitHub Actions `Deploy Dev EC2` run `26409253056`은 `develop` HEAD `5b04918` 기준으로 성공했다.
- 첫 dev smoke 실행은 임시 사용자와 Bearer token 준비, 시나리오 목록 조회까지 성공했다.
- 시나리오 1 첫 발화에서 백엔드가 `503 AI_RESPONSE_INVALID`를 반환했고, 같은 payload를 AI 서버 `POST /api/v1/conversation/next-question`에 직접 보내도 `400 INVALID_REQUEST`가 반환됐다.
- 현재 `saynow-ai` 요청 모델은 `scenarioSituation`을 `next-question`, `feedback`, `feedback/stream` 필수 필드로 요구한다. 백엔드 AI 요청 DTO에는 아직 이 필드가 없어 dev AI 서버와 계약이 어긋난 상태다.
- 해결 범위는 저장된 `Scenario.situation`을 AI 요청 DTO에 포함하는 것으로 제한한다. 프론트 응답 필드 `scenarioSituation`은 이미 반영되어 있으므로 별도 변경하지 않는다.
- RED 검증으로 `./gradlew test --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.feedback.FeedbackStreamIntegrationTest`를 실행했고, `AiNextQuestionRequest`, `AiFeedbackRequest`에 `scenarioSituation` 필드가 없어 컴파일 실패했다.
- `AiNextQuestionRequest`, `AiFeedbackRequest`에 `scenarioSituation` 필드를 추가하고, `SessionService`와 `FeedbackService`가 `session.getScenario().getSituation()`을 넘기도록 수정했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.feedback.FeedbackStreamIntegrationTest`를 실행했고 통과했다.
- 같은 AI `next-question` payload에 `scenarioSituation`을 추가해 직접 호출하니 dev AI 서버가 `200 OK`와 `filledSlots=[visit_purpose, stay_duration, accommodation]`, `turnClassification=ANSWER`를 반환했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- 커밋 `5849ac0`을 `develop`에 push했고, GitHub Actions `Deploy Dev EC2` run `26409775332`가 성공했다.
- 재배포 후 health는 `{"status":"UP"}`였다.
- 새 임시 사용자 `airport-smoke-20260525162051@saynow.dev`를 만들고 공항 시나리오 1~3단계를 진행했다. 전체 request/response evidence는 `/private/tmp/saynow-airport-smoke-20260525T162050Z.json`에 저장했다.
- 초기 Airport 목록은 1단계만 `locked=false`, 2~3단계는 `PREVIOUS_SCENARIO_NOT_CLEARED`였다.
- 최종 Airport 목록은 1~3단계 모두 `cleared=true`, `locked=false`였다.
- 1단계는 세션 `6`, 발화 1회, `scenarioResult=SUCCESS`, 점수 90, 하트 차감 없음, 남은 하트 3으로 완료됐다.
- 2단계는 세션 `7`, 발화 3회, `scenarioResult=SUCCESS`, 점수 90, 하트 차감 없음, 남은 하트 3으로 완료됐다.
- 3단계는 세션 `8`, 발화 2회, `scenarioResult=SUCCESS`, 점수 85, 하트 차감 없음, 남은 하트 3으로 완료됐다.
- 3단계 품질 관찰점은 AI가 첫 발화 뒤 `Can you ask the staff if you can still board your flight?`처럼 사용자에게 다시 "직원에게 물어볼 수 있냐"고 묻는 점이다. 시나리오 목표와 맞지만 대화 상대가 공항 직원이라는 설정에서는 약간 메타적으로 들릴 수 있다.

---

## 2026-05-24

- AI 피드백 생성 요청에 세션 성공/실패 결과를 함께 보내기로 했다. 필드명은 AI 계약 기준으로 `sessionResult`이고 값은 현재 세션 상태의 `SUCCESS` 또는 `FAILURE` 문자열이다.
- 기존 기본 피드백 API와 SSE 피드백 API는 모두 `FeedbackService.toAiFeedbackRequest(...)`를 통해 AI 요청 DTO를 만들기 때문에, DTO 필드와 공통 매핑을 함께 바꾸면 두 API에 반영된다.
- `loadFeedbackContext(...)`가 이미 `SUCCESS`, `FAILURE` 세션만 피드백 생성 가능하게 제한하므로 `sessionResult`에는 `IN_PROGRESS`, `ABANDONED`가 들어가지 않는다.

- 사용자가 피드백 생성 대기 시간을 줄이기 위해 피드백 생성과 무관하게 시나리오 성공/실패 여부를 먼저 조회하는 API를 요청했다.
- 현재 세션 성공/실패는 `SessionStatus.SUCCESS`, `SessionStatus.FAILURE`로 백엔드가 이미 알고 있다. 피드백 생성 결과나 AI 최종 피드백 응답에 의존하지 않는다.
- 새 API는 세션 도메인 책임으로 둔다. 경로는 기존 세션 라우팅을 따라 `GET /api/v1/sessions/{sessionId}/result`로 추가한다.
- 응답 데이터는 목적에 맞게 `scenarioResult` 하나만 반환한다. `sessionId`, `remainingHearts`, `feedbackAvailable`은 path나 기존 API에서 알 수 있으므로 이번 응답에는 포함하지 않는다.
- `SUCCESS`, `FAILURE`만 정상 응답한다. `IN_PROGRESS`, `ABANDONED`는 성공/실패가 확정되지 않았거나 피드백 결과 보기 흐름이 아니므로 `SESSION_NOT_COMPLETABLE`로 거부한다.

---

## 2026-05-21

- 작업 브랜치는 `feat/feedback-sse-stream`이다.
- 현재 백엔드는 `spring-boot-starter-webmvc` 기반이며, 동기 AI 호출은 `RemoteAiConversationClient`에서 Java `HttpClient`로 처리한다.
- 기존 동기 피드백 API `POST /api/v1/sessions/{sessionId}/feedback`는 유지한다.
- 새 백엔드 SSE API는 기존 라우팅 규칙을 따라 `POST /api/v1/sessions/{sessionId}/feedback/stream`으로 둔다.
- 현재 DB에는 `feedbackStatus` 컬럼이 없다. 이번 MVP에서는 `summary`와 `turnFeedback`를 프론트에 즉시 relay하되, DB 저장은 `done` 수신 후 한 번에 확정한다.
- 중간 실패나 AI `error` 이벤트가 오면 `session_feedbacks`를 만들지 않는다. 이 정책이면 기존 동기 API나 스트림 API를 다시 시도할 수 있다.
- AI 요청 body는 기존 동기 피드백과 같은 `AiFeedbackRequest`를 사용한다.
- 원격 SSE 호출은 사용자 요청의 참고 방향에 맞춰 `WebClient`와 Reactor `Flux`를 사용한다. MVC 앱은 그대로 유지하고, 컨트롤러 응답은 `StreamingResponseBody`로 `text/event-stream`을 직접 쓴다.
- RED 검증으로 `./gradlew test --tests com.saynow.feedback.FeedbackStreamIntegrationTest`를 실행했고, `AiFeedbackStreamClient`, `AiFeedbackStreamEvent`, `AiFeedbackStreamException`, Reactor `Flux`가 없어 컴파일 실패하는 것을 확인했다.
- `spring-boot-starter-webflux`를 추가하고, `AiFeedbackStreamClient` 계약과 로컬/원격 구현을 추가했다.
- 원격 SSE 호출은 `POST /api/v1/conversation/feedback/stream`, `Accept: text/event-stream`, `Content-Type: application/json` 기준이다. 설정 override는 `SAYNOW_AI_FEEDBACK_STREAM_PATH`, `SAYNOW_AI_FEEDBACK_STREAM_TIMEOUT`으로 둔다.
- 백엔드 SSE endpoint는 `POST /api/v1/sessions/{sessionId}/feedback/stream`이다.
- MockMvc SSE 테스트에서 ASYNC dispatch가 Bearer token 없이 다시 Security 필터를 타면서 `AuthorizationDeniedException`이 발생했다. 초기 REQUEST는 이미 인증되므로 `DispatcherType.ASYNC`, `DispatcherType.ERROR`는 permit 처리했다.
- 전체 테스트 첫 실행에서 dev/prod 원격 AI 프로필 컨텍스트가 `WebClient.Builder` 빈 부재로 실패했다. `AiClientConfiguration`에 명시적인 `WebClient.Builder` Bean을 추가했다.
- 원격 스트림에는 `timeout`과 `take`를 모두 적용한다. idle gap과 전체 스트림 시간이 `saynow.ai.feedback-stream-timeout`을 넘으면 백엔드는 `done` 없이 종료된 스트림으로 보고 `error` 이벤트를 내려준다.
- 관련 검증으로 `./gradlew test --tests com.saynow.feedback.FeedbackStreamIntegrationTest`를 실행했고 통과했다.
- 컨텍스트 회귀 검증으로 `./gradlew test --tests com.saynow.DevAiClientModeTest --tests com.saynow.DevOpenApiIntegrationTest --tests com.saynow.ProdAiClientModeTest --tests com.saynow.SentryProdConfigurationTest`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.

---

# 로컬 네트워크 CORS origin 추가 컨텍스트 노트

## 2026-05-12

- 사용자가 main에서 직접 작업하라고 요청했다.
- `/Users/sangmin8817/Desktop/Soma/saynow-be` main worktree가 최신 `origin/main`보다 뒤처져 있어 `git fetch origin`과 `git merge --ff-only origin/main`으로 최신화했다.
- 추가 origin은 `http://172.30.1.89:3000`, `http://10.0.2.2:3000`이다.
- RED 검증으로 `./gradlew test --tests com.saynow.auth.SecurityAuthenticationIntegrationTest`를 실행했고, 두 신규 preflight 테스트가 200 대신 403으로 실패하는 것을 확인했다.
- `application.yml`, `.env.example`, README의 기본 CORS origin 목록에 두 origin을 추가했다.
- GitHub Actions 배포 workflow는 이미 `SAYNOW_CORS_ALLOWED_ORIGINS`를 SSM 선택 파라미터로 읽으므로 workflow 수정은 필요하지 않다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.auth.SecurityAuthenticationIntegrationTest`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.

---

# Swagger 서버 도메인 설정 컨텍스트 노트

## 2026-05-12

- 작업 브랜치는 기존 CORS PR 브랜치인 `feat/cors-allowed-origins`다.
- 운영 백엔드 도메인은 `https://saynow.p-e.kr`로 변경됐다.
- 현재 `OpenApiConfig`는 title, version, description만 설정하고 OpenAPI `servers`를 설정하지 않는다.
- Swagger UI에서 운영 도메인을 기본 서버로 보여주기 위해 OpenAPI document의 `servers[0].url`을 `https://saynow.p-e.kr`로 설정한다.
- 기본값은 `application.yml`에 두고, 필요하면 `SAYNOW_OPENAPI_SERVER_URL`로 override할 수 있게 한다.
- RED 검증으로 `./gradlew test --tests com.saynow.OpenApiIntegrationTest.exposesOpenApiDocumentForMvpApis`를 실행했고, OpenAPI `servers[0].url`이 `http://localhost`라서 실패하는 것을 확인했다.
- `OpenApiConfig`에 `saynow.openapi.server-url` 값을 주입하고 `OpenAPI.servers`에 서버 URL을 추가했다.
- `application.yml` 기본값은 `SAYNOW_OPENAPI_SERVER_URL` 없을 때 `https://saynow.p-e.kr`이다.
- `.env.example`, README, EC2 배포 workflow의 선택 SSM 파라미터 목록에 `SAYNOW_OPENAPI_SERVER_URL`을 추가했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.OpenApiIntegrationTest.exposesOpenApiDocumentForMvpApis`를 실행했고 통과했다.
- 관련 회귀 검증으로 `./gradlew test --tests com.saynow.OpenApiIntegrationTest --tests com.saynow.auth.SecurityAuthenticationIntegrationTest`를 실행했고 통과했다.
- workflow YAML 검증으로 `ruby -e 'require "yaml"; YAML.load_file(".github/workflows/deploy-prod-ec2.yml"); puts "yaml ok"'`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.

---

# 프론트 CORS origin 허용 컨텍스트 노트

## 2026-05-12

- 작업 브랜치는 `feat/cors-allowed-origins`다.
- 기준 커밋은 `origin/main`의 `031be36`이며, 회원 탈퇴 PR #10 merge commit 위에서 시작했다.
- 현재 코드에는 `cors`, `CorsConfigurationSource`, `Access-Control` 관련 설정이 없다.
- 허용할 프론트 origin은 `https://saynow-fe-web.vercel.app`, `http://localhost:3000` 두 개다.
- 현재 인증은 cookie가 아니라 Bearer access token 기반이다. 따라서 CORS `allowCredentials`는 `false`로 두고, `Authorization`, `Content-Type` 헤더를 허용한다.
- 운영과 로컬 기본값은 `application.yml`에 두되, 필요하면 `SAYNOW_CORS_ALLOWED_ORIGINS`로 교체할 수 있게 한다.
- RED 검증으로 `./gradlew test --tests com.saynow.auth.SecurityAuthenticationIntegrationTest`를 실행했고, preflight 요청이 CORS 처리 없이 인증 필터로 흘러 401로 실패하는 것을 확인했다.
- `CorsProperties`를 추가하고 `AuthSecurityConfig`에서 Spring Security `cors`를 활성화했다.
- `application.yml` 기본 CORS origin은 `https://saynow-fe-web.vercel.app,http://localhost:3000`이다.
- `.env.example`, README, EC2 배포 workflow의 선택 SSM 파라미터 목록에 `SAYNOW_CORS_ALLOWED_ORIGINS`를 추가했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.auth.SecurityAuthenticationIntegrationTest`를 실행했고 통과했다.
- workflow YAML 검증으로 `ruby -e 'require "yaml"; YAML.load_file(".github/workflows/deploy-prod-ec2.yml"); puts "yaml ok"'`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.

---

# 회원 탈퇴 기능 컨텍스트 노트

## 2026-05-11

- 작업 브랜치는 `feat/member-withdrawal`이다.
- 현재 회원 row는 `practice_sessions.member_id` FK의 참조 대상이다. 따라서 회원 row를 물리 삭제하면 기존 세션 데이터까지 함께 정리해야 하므로 범위가 커진다.
- 이번 구현은 회원 row를 보존하고 `withdrawn_at`으로 탈퇴 상태를 기록한다. 과거 세션 FK는 유지하되, 탈퇴 회원 access token은 인증 필터에서 거부한다.
- 탈퇴 시 refresh token은 모두 폐기한다. access token은 서버 저장 토큰이 아니므로, 인증 필터가 회원 active 상태를 확인해 차단한다.
- 탈퇴 시 social account 연결은 삭제한다. 같은 provider subject로 재로그인하면 새 회원으로 가입되는 동작을 목표로 한다.
- 프로덕션 코드 변경 전 `SocialAuthApiIntegrationTest`에 탈퇴 통합 테스트를 추가하고 RED를 확인한다.
- RED 검증으로 `./gradlew test --tests com.saynow.auth.SocialAuthApiIntegrationTest.withdrawRevokesTokensRejectsExistingAccessTokenAndAllowsFreshSocialSignup`를 실행했고, `DELETE /api/v1/auth/me`가 매핑되지 않아 500 `INTERNAL_SERVER_ERROR`로 실패하는 것을 확인했다.
- `V4__member_withdrawal.sql`로 `members.withdrawn_at`을 추가했다.
- `AuthService.withdraw`는 active member만 조회하고, 같은 transaction에서 active refresh token을 폐기한 뒤 social account 연결을 삭제하고 회원 프로필을 비운다.
- `AuthTokenFilter`는 access token 서명 검증 후 `MemberRepository.existsByIdAndWithdrawnAtIsNull`로 회원 active 상태를 확인한다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.auth.SocialAuthApiIntegrationTest.withdrawRevokesTokensRejectsExistingAccessTokenAndAllowsFreshSocialSignup`를 실행했고 통과했다.
- 관련 회귀 검증으로 `./gradlew test --tests com.saynow.auth.SocialAuthApiIntegrationTest --tests com.saynow.auth.SecurityAuthenticationIntegrationTest --tests com.saynow.practice.PracticeSessionAuthorizationIntegrationTest`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.

---

# 소셜 로그인 OIDC 컨텍스트 노트

---

# 2차 MVP API 재설계 컨텍스트 노트

## 확정된 기준

- `develop`은 2차 MVP breaking change 브랜치로 사용한다.
- `main`은 1차 MVP 운영 서버 기준으로 유지한다.
- 프론트 API 응답은 기존 공통 응답 객체 `ApiResponse<T>`로 계속 감싼다.
- `ApiResponse<T>` 성공 응답은 API별 HTTP status를 명시할 수 있어야 한다. 세션 시작 API는 201 Created를 유지한다.
- 기존 1차 MVP 엔티티, 클래스, API, DTO, 서비스는 호환성 유지 없이 삭제하거나 재구성해도 된다.
- Flyway migration은 `develop` 전용으로 기존 V1~V4를 정리하고 2차 MVP 새 스키마 기준으로 다시 시작한다.
- 사용자 발화 필드명은 API, DB, AI 계약에서 `userUtterance`로 통일한다.
- `SCENARIOS.originalQuestion`은 세션 시작 시 첫 AI 질문이다.

## 구현 결정

- 세션 ID는 API 명세와 ERD에 맞춰 DB PK `Long`을 사용한다.
- `users` 테이블은 기존 `members` 개념을 대체하고, 소셜 로그인 식별자인 `provider`, `sub`를 직접 가진다.
- 탈퇴 사용자는 `deleted_at`을 기록하고, 재가입을 허용하기 위해 `provider`, `sub`를 비운다.
- refresh token은 인증 흐름 유지를 위해 계속 별도 테이블로 관리한다.
- 시나리오 시작 시 첫 턴을 `session_turns`에 `sequence=1`, `user_utterance=null` 상태로 생성한다.
- 사용자 발화 제출은 현재 답변이 비어 있는 가장 이른 턴을 채우고, AI가 다음 질문을 반환하면 다음 턴을 생성한다.
- 하트는 세션 시작 시 `scenarios.heart`로 시작하고, 이번 2차 MVP에서는 발화 제출 1회마다 1개 감소한다.
- 모든 슬롯이 충족되면 세션은 성공 완료 가능 상태가 된다.
- 하트가 0이 되면 세션은 실패 완료 가능 상태가 된다.
- 성공 또는 실패로 완료 가능해진 세션에서만 피드백 생성 API를 호출할 수 있다.
- 잠긴 시나리오나 잠긴 카테고리는 세션 시작을 막는다.
- `DELETE /api/v1/sessions/{sessionId}`는 `ApiResponse<T>` 유지 기준에 맞춰 200 OK와 `ApiResponse.success(null)`을 반환한다.
- `session_feedbacks`는 세션 단위 피드백 결과를 저장한다. `turn_feedbacks`는 `session_feedbacks`에 속한 턴별 피드백 결과를 저장한다.
- 사용자별 시나리오 진행 상태 테이블은 `user_scenario_progress`로 명명한다. boolean 저장 컬럼은 `is_cleared`가 아니라 상태명인 `cleared`를 사용한다.
- dev 배포는 prod EC2 배포 방식과 동일하게 GitHub Actions 수동 실행, EC2 SSH 업로드, SSM Parameter Store 기반 `.env` 생성, systemd restart, health check 순서로 처리한다.
- dev 배포는 GitHub Environment `develop`, SSM path `/saynow/develop`, Spring profile `dev`를 사용한다.

## 검증 결과

- `./gradlew test`를 실행했고 통과했다.
- `ApiResponse.success(HttpStatus, data)` 헬퍼 추가 전 `./gradlew test --tests com.saynow.common.response.ApiResponseTest`가 메서드 미존재 컴파일 오류로 실패하는 것을 확인했다.
- `ApiResponse.success(HttpStatus, data)` 헬퍼를 추가하고 2차 MVP 시나리오, 세션, 피드백 컨트롤러가 명시적인 HTTP status를 반환하도록 정리했다.
- 관련 검증으로 `./gradlew test --tests com.saynow.common.response.ApiResponseTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.OpenApiIntegrationTest`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.
- `UserScenarioProgress` 스키마 테스트 추가 전 `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest`가 `user_scenario_progress` 테이블 부재로 실패하는 것을 확인했다.
- `user_scenario_clears.is_cleared`를 `user_scenario_progress.cleared`로 변경하고, 도메인과 저장소 이름을 `UserScenarioProgress` 기준으로 맞췄다.
- 관련 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest --tests com.saynow.scenario.ScenarioFlowIntegrationTest`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- dev 배포 workflow와 dev profile 테스트 추가 전 `./gradlew test --tests com.saynow.DevDeploymentWorkflowTest --tests com.saynow.DevAiClientModeTest`가 workflow 파일 부재와 dev AI mode `local` 기본값으로 실패하는 것을 확인했다.
- `Deploy Dev EC2` workflow는 prod 배포와 같은 build, OIDC, 임시 SSH ingress, jar upload, SSM `.env` 생성, systemd restart, health check, ingress revoke 흐름으로 구성했다.
- dev workflow는 GitHub Environment `develop`의 `AWS_ROLE_ARN`, `EC2_HOST`, `EC2_USER`, `EC2_SECURITY_GROUP_ID`, `EC2_SSH_KEY`를 사용하고, `/saynow/develop` SSM path에서 런타임 환경변수를 읽는다.
- dev profile은 PostgreSQL, Flyway validate, 원격 AI 클라이언트 기본값, Sentry environment `dev` 기준으로 추가했다.
- 관련 검증으로 `./gradlew test --tests com.saynow.DevDeploymentWorkflowTest --tests com.saynow.DevAiClientModeTest`를 실행했고 통과했다.
- YAML 검증으로 `ruby -e 'require "yaml"; YAML.load_file(ARGV[0]); puts "yaml ok"' .github/workflows/deploy-dev-ec2.yml`와 같은 명령을 `application-dev.yml`에도 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`와 `./gradlew build`를 실행했고 통과했다.
- dev 배포 SSM path는 `/saynow/dev`가 아니라 `/saynow/develop`로 확정했다.
- 회귀 테스트 기대값을 먼저 `/saynow/develop`로 변경한 뒤 `./gradlew test --tests com.saynow.DevDeploymentWorkflowTest`가 실패하는 것을 확인했고, workflow와 README, 작업 메모를 같은 기준으로 수정한 뒤 해당 테스트와 전체 테스트가 통과했다.
- GitHub Actions Environment 이름은 `develop`로 확정했다.
- dev 프로필 Swagger/OpenAPI server URL 기본값은 `https://dev-saynow.p-e.kr`로 설정한다. SSM의 `/saynow/develop/SAYNOW_OPENAPI_SERVER_URL` 값이 있으면 그 값으로 override할 수 있다.
- RED 검증으로 `./gradlew test --tests com.saynow.DevDeploymentWorkflowTest --tests com.saynow.DevOpenApiIntegrationTest`를 실행했고, workflow environment와 dev OpenAPI server URL 기대값이 맞지 않아 실패했다.
- workflow `environment`를 `develop`로 수정하고 `application-dev.yml`에 `saynow.openapi.server-url` 기본값을 추가한 뒤 같은 테스트가 통과했다.
- dev 서버 도메인은 `https://dev-api.p-e.kr`에서 `https://dev-saynow.p-e.kr`로 변경했다.
- RED 검증으로 `./gradlew test --tests com.saynow.DevOpenApiIntegrationTest`를 실행했고, dev OpenAPI server URL 기대값이 맞지 않아 실패했다.
- SSM `/saynow/develop/SAYNOW_OPENAPI_SERVER_URL` 값이 기존 `https://dev-api.p-e.kr`로 설정되어 있어, 배포 시 코드 기본값보다 SSM override가 우선 적용되는 상태였다.
- SSM `/saynow/develop/SAYNOW_OPENAPI_SERVER_URL` 값을 `https://dev-saynow.p-e.kr`로 갱신했고, 재조회로 새 값이 반영된 것을 확인했다.
- Swagger UI에 Bearer token 입력 칸이 나오려면 OpenAPI `components.securitySchemes.bearerAuth`와 security requirement가 필요하다.
- 공개 auth API인 `socialLogin`, `refresh`는 Bearer security requirement를 빈 배열로 override하고, 그 외 API는 전역 Bearer security requirement를 따른다.
- RED 검증으로 `./gradlew test --tests com.saynow.OpenApiIntegrationTest`를 실행했고, `components.securitySchemes.bearerAuth`가 없어 실패했다.
- nonce 검증은 기본값 `true`로 유지하고, dev 프로필에서는 `saynow.auth.oidc.nonce-required=false`를 기본값으로 둔다.
- `SocialLoginRequest.nonce`는 설정 기반 검증을 위해 validation 필수값에서 제외한다. nonce가 필수인 환경에서는 verifier가 `OIDC_NONCE_MISMATCH`를 반환한다.
- RED 검증으로 `./gradlew test --tests com.saynow.auth.SocialAuthApiIntegrationTest.socialLoginRejectsMissingNonceByDefault --tests com.saynow.auth.DevSocialAuthNonceIntegrationTest`를 실행했고, 기본 validation과 dev 비활성화 설정이 기존 구현과 맞지 않아 실패했다.
- dev SSM `/saynow/develop/SAYNOW_AUTH_OIDC_NONCE_REQUIRED`는 존재하지 않았다. 배포 시 `application-dev.yml` 기본값 `false`가 적용된다.
- 2026-05-24 작업은 사용자가 요청한 대로 `origin/develop` 기준 새 브랜치 `feat/develop-ai-turn-policy`에서 다시 진행한다.
- AI next-question 응답에는 `turnClassification`을 추가한다. 값은 `ANSWER`, `ASSISTANCE_REQUEST`, `INVALID_RESPONSE`만 허용한다.
- 하트 차감은 `filledSlots`가 비어 있는지와 무관하게 `turnClassification == INVALID_RESPONSE`일 때만 수행한다.
- `ASSISTANCE_REQUEST`는 메뉴 추천, 메뉴 보기, 옵션 확인 같은 정상 대화 턴이다. 슬롯을 채우지 않아도 실패 턴으로 처리하지 않고 AI가 내려준 `nextQuestion`을 다음 질문으로 사용한다.
- 사용자 발화 API 응답에는 프론트가 정책을 추론하지 않도록 백엔드 계산값 `heartDeducted`, `remainingHearts`, `turnClassification`을 포함한다.
- 최소 검증 케이스는 `Can you recommend a menu?`, `Can I see the menu?`, `That’s all.`, `I want coffee.`, `I want drink.` 다섯 발화다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest`를 실행했고, `TurnClassification` 타입이 아직 없어 테스트 컴파일이 실패하는 것을 확인했다.
- `TurnClassification` enum을 추가하고 AI next-question 내부 DTO, 원격 AI 응답 매핑, 로컬 AI 대체 클라이언트, 사용자 발화 응답 DTO를 같은 분류값 기준으로 연결했다.
- `SessionService`는 이제 `filledSlots` 비어 있음이 아니라 `turnClassification == INVALID_RESPONSE`일 때만 하트를 차감한다.
- 관련 GREEN 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest`, `./gradlew test --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest`, `./gradlew test --tests com.saynow.OpenApiIntegrationTest`를 실행했고 모두 통과했다.
- 전체 검증으로 `./gradlew test`와 `git diff --check`를 실행했고 모두 통과했다.

## 2026-05-09

- 작업 브랜치는 `feat/3`이다.
- 기준 문서는 `/Users/sangmin8817/Desktop/Obsidian/SayNow/SayNow 소셜 로그인 OIDC 책임 분리 및 API 명세.md`다.
- MVP에서는 refresh token을 JSON 응답으로 반환한다. 프론트는 보안 저장소에 저장하고, 백엔드는 원문이 아니라 hash만 저장한다.
- provider 프로필 이미지는 수집, 저장, 응답하지 않는다.
- 사용자가 요구사항을 갱신했다. 기존 MVP API 권한 정책을 유지하지 않고, 연습 세션과 피드백 API는 로그인 필수로 전환한다.
- 연습 세션은 회원에게 귀속해야 한다. `practice_sessions.member_id`를 추가하고 세션 생성 시 현재 회원을 저장한다.
- 시나리오 조회 API는 탐색 화면 용도로 공개 유지한다.
- DB에는 현재 보호해야 할 운영 데이터가 없다고 사용자가 확인했다. 그래도 Flyway 히스토리를 명확히 하기 위해 기존 `V1`, `V2`는 수정하지 않고 새 `V3__...sql`로 인증 테이블과 `practice_sessions.member_id`를 추가한다.
- TDD 원칙에 따라 auth API, 인증 필수, 세션 소유자 검증 테스트를 먼저 작성하고 실패를 확인한 뒤 production code를 수정한다.
- RED 검증으로 `./gradlew test --tests com.saynow.auth.SocialAuthApiIntegrationTest --tests com.saynow.practice.PracticeSessionAuthorizationIntegrationTest`를 먼저 실행했고, auth API 미구현과 세션 API 비인증 동작 때문에 실패하는 것을 확인했다.
- 초기 구현은 Spring Security 의존성 없이 MVC interceptor로 보호 API를 제한했다. 이후 사용자 피드백을 반영해 Spring Security 필터 구조로 전환했다.
- access token은 HMAC SHA-256 서명 JWT 형태로 자체 발급한다. refresh token은 랜덤 opaque 문자열로 발급하고 DB에는 SHA-256 hash만 저장한다.
- 테스트에서는 `saynow.auth.oidc.fake-enabled=true`로 fake OIDC verifier를 사용한다. 운영 기본 verifier는 Google/Kakao JWKS를 조회해 RS256 서명과 issuer, audience, exp, iat, nonce를 검증한다.
- 세션 생성 시 현재 회원을 `PracticeSession.member`에 저장하고, 세션/피드백 조회와 수정은 현재 회원 소유인지 확인한다. 다른 회원 세션이면 `SESSION_ACCESS_DENIED`를 반환한다.
- OpenAPI 예시에 auth API와 protected session API의 `AUTH_REQUIRED`, `SESSION_ACCESS_DENIED` 응답을 추가했다.
- provider가 재로그인 시 optional claim인 email/nickname을 누락하면 기존 회원 프로필을 null로 덮지 않는다. 이를 `SocialAuthApiIntegrationTest.socialLoginKeepsExistingProfileWhenProviderOmitsOptionalClaims`로 RED/GREEN 검증했다.
- 검증 명령은 `./gradlew test --tests com.saynow.auth.SocialAuthApiIntegrationTest --tests com.saynow.practice.PracticeSessionAuthorizationIntegrationTest --tests com.saynow.practice.PracticeSessionApiIntegrationTest`, `./gradlew test --tests com.saynow.OpenApiIntegrationTest`, `./gradlew test`를 실행했다.
- 사용자 피드백으로 인증 구조를 재검토했다. 재검토 시점의 구현은 `HandlerInterceptor`가 access token을 검증하고 `ThreadLocal`에 `memberId`를 저장하는 방식이었다.
- 보완 방향은 각 컨트롤러가 토큰을 직접 파싱하지 않고, `OncePerRequestFilter`가 Bearer token을 한 번 검증한 뒤 Spring Security `SecurityContext`에 인증 principal을 저장하는 구조다.
- 서비스 계층은 기존처럼 현재 회원 ID를 요구하되, `ThreadLocal`이 아니라 `SecurityContext`의 principal에서 `memberId`를 읽도록 바꾼다.
- RED 검증으로 `./gradlew test --tests com.saynow.auth.SecurityAuthenticationIntegrationTest`를 실행했고, `authTokenFilter` 빈이 없어 실패하는 것을 확인했다.
- 구현은 Spring Security 의존성을 추가하고, `AuthTokenFilter extends OncePerRequestFilter`가 Bearer token을 검증해 `AuthMemberPrincipal implements UserDetails`를 `SecurityContext`에 저장하는 구조로 전환했다.
- 컨트롤러는 토큰 원문을 받지 않고 `@AuthenticationPrincipal AuthMemberPrincipal`에서 `memberId`를 꺼내 서비스에 명시적으로 전달한다.
- 기존 `AuthInterceptor`, `AuthWebConfig`, `CurrentMemberContext`는 제거했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.auth.SecurityAuthenticationIntegrationTest --tests com.saynow.practice.PracticeSessionAuthorizationIntegrationTest`를 실행했고 통과했다.
- 연관 회귀 검증으로 `./gradlew test --tests com.saynow.auth.SocialAuthApiIntegrationTest --tests com.saynow.auth.SecurityAuthenticationIntegrationTest --tests com.saynow.practice.PracticeSessionAuthorizationIntegrationTest --tests com.saynow.practice.PracticeSessionApiIntegrationTest --tests com.saynow.OpenApiIntegrationTest`를 실행했고 통과했다.
- 전체 검증으로 `./gradlew test`를 실행했고 통과했다.
- 필터의 `ApiException` catch 범위는 access token 파싱으로만 제한했다. 컨트롤러 이후 서비스 예외는 기존 `GlobalExceptionHandler` 흐름을 타야 하기 때문이다.
- catch 범위 조정 후 `./gradlew test --tests com.saynow.auth.SecurityAuthenticationIntegrationTest --tests com.saynow.practice.PracticeSessionAuthorizationIntegrationTest`와 `./gradlew test`를 다시 실행했고 통과했다.
- Google Web OAuth Client ID는 `1062331189445-ov26of8u6pb8iauq0c4n68ni1flipm1q.apps.googleusercontent.com`이고, callback URI는 `http://localhost:3000/auth/google/callback`이다.
- 백엔드는 callback URI를 직접 사용하지 않는다. Google ID Token의 `aud` 검증을 위해 `SAYNOW_AUTH_OIDC_GOOGLE_AUDIENCES`에 Google Client ID를 설정한다.
- `SAYNOW_AUTH_TOKEN_SECRET`, `SAYNOW_AUTH_OIDC_GOOGLE_AUDIENCES`, `SAYNOW_AUTH_OIDC_KAKAO_AUDIENCES`를 `application.yml` env placeholder와 `.env.example`, README, prod EC2 deploy workflow에 반영했다.
- env placeholder가 빈 문자열로 바인딩되는 경우를 피하기 위해 `RemoteOidcTokenVerifier`에서 blank audience를 무시한다.
- 검증으로 `./gradlew test --rerun-tasks`와 `./gradlew build`를 실행했고 통과했다.

## 2026-05-10

- 운영 소셜 로그인 환경 보강 범위는 Parameter Store audience 값 보강과 배포 워크플로의 `.env` 주입 목록 수정이다.
- 백엔드 OIDC verifier는 Google/Kakao ID token의 issuer, audience, signature, expiry, nonce를 검증한다.
- 운영에서 `SAYNOW_AUTH_OIDC_GOOGLE_AUDIENCES`, `SAYNOW_AUTH_OIDC_KAKAO_AUDIENCES`, `SAYNOW_AUTH_TOKEN_SECRET`이 서비스 환경에 들어가지 않으면 소셜 로그인은 실제 ID token 검증 단계에서 실패한다.
- 사용자가 전달한 key 원문은 리포지토리 파일에 기록하지 않고 AWS Parameter Store에만 반영한다.
- AWS Parameter Store `/saynow/prod`의 Google audience는 web client id와 Android client id를 모두 포함하도록 갱신했다.
- AWS Parameter Store `/saynow/prod`의 Kakao audience는 REST API key와 Native App Key를 모두 포함하도록 갱신했다.
- 배포 워크플로의 required 환경변수 목록에 `SAYNOW_AUTH_OIDC_GOOGLE_AUDIENCES`, `SAYNOW_AUTH_OIDC_KAKAO_AUDIENCES`, `SAYNOW_AUTH_TOKEN_SECRET`을 추가했다.
- 검증 명령은 `git diff --check`, `ruby -e 'require "yaml"; YAML.load_file(ARGV[0]); puts "yaml ok"' .github/workflows/deploy-prod-ec2.yml`, 더미 SSM JSON 기반 `.env` 출력 시뮬레이션, `./gradlew test --tests com.saynow.auth.SocialAuthApiIntegrationTest --tests com.saynow.auth.SecurityAuthenticationIntegrationTest --rerun-tasks`를 실행했고 모두 통과했다.
- PR #4는 최신 `origin/main` 기준 GitHub에서 `mergeable=CONFLICTING`, `mergeStateStatus=DIRTY`였다.
- 로컬 `feat/3`은 `origin/feat/3`보다 1커밋 뒤처져 있어 먼저 fast-forward로 원격 PR head `eb61c4c`에 맞췄다.
- `origin/main` 병합 충돌은 `src/test/java/com/saynow/OpenApiIntegrationTest.java` 하나에서 발생했다. 소셜 인증 OpenAPI 검증은 유지하고, `GET /api/v1/scenarios/{scenarioId}`는 존재하지 않는다는 `main` 계약을 함께 보존했다.
- 병합 후 `PracticeSessionApiIntegrationTest` 컴파일이 실패했다. `main`의 새 최대 follow-up 테스트가 `feat/3`의 인증 필수 계약을 반영하지 않아 `startSession(scenarioId, accessToken)`과 Authorization 헤더를 쓰도록 맞췄다.
- 충돌 해결 검증으로 `./gradlew test --tests com.saynow.OpenApiIntegrationTest --tests com.saynow.auth.SocialAuthApiIntegrationTest --tests com.saynow.auth.SecurityAuthenticationIntegrationTest --tests com.saynow.practice.PracticeSessionAuthorizationIntegrationTest --tests com.saynow.practice.PracticeSessionApiIntegrationTest --tests com.saynow.scenario.ScenarioApiIntegrationTest`, `git diff --check`, `./gradlew test`를 실행했고 통과했다.

---

# AI 서버 연동 컨텍스트 노트

## 2026-05-10

- PR #7은 최신 `origin/main` 기준 GitHub에서 `mergeable=CONFLICTING`, `mergeStateStatus=DIRTY`였다.
- `origin/main` 병합은 처음에 `.git/worktrees/.../ORIG_HEAD.lock` 권한 문제로 실패했고, 권한 상승 후 실제 충돌은 `checklist.md` 하나에서만 발생했다.
- `checklist.md` 충돌은 `origin/main`의 턴 제출 관련 작업 기록과 `feat/6`의 시나리오 API 작업 기록을 모두 보존하는 방식으로 해결했다.
- 병합 후 `PracticeSessionApiIntegrationTest.failsSessionAndMakesFeedbackAvailableAfterMaxFollowUps`가 400 `VALIDATION_FAILED`로 실패했다. 최신 컨트롤러는 `request` multipart part를 요구하지만 해당 테스트만 옛 form field 방식을 남겨둔 것이 원인이어서 `turnRequest(...)` helper 사용으로 맞췄다.
- 충돌 해결 검증으로 `./gradlew test --tests com.saynow.OpenApiIntegrationTest --tests com.saynow.scenario.ScenarioApiIntegrationTest --tests com.saynow.practice.PracticeSessionApiIntegrationTest`, `git diff --check`, `./gradlew test`를 실행했고 통과했다.
- 시나리오 상세 조회 API는 제거한다. 목록 API가 이미 각 시나리오별 `situationDescription`을 반환하므로, 상세 조회 성공 테스트와 OpenAPI 경로도 제거 대상이다.
- 삭제 범위는 `ScenarioController.getScenarioDetail`, `ScenarioService.getScenarioDetail`, `ScenarioDetailResponse`, `OpenApiResponseCustomizer`의 상세 조회 예시, 관련 통합 테스트로 제한한다.
- 프로덕션 코드 변경 전 `./gradlew test --tests com.saynow.OpenApiIntegrationTest`를 실행했고, `/api/v1/scenarios/{scenarioId}`가 아직 OpenAPI에 있어 두 테스트가 실패했다.
- 상세 조회 전용 컨트롤러 메서드, 서비스 메서드, DTO 파일, 수동 OpenAPI 예시를 제거했다. 상세 조회 관련 참조 검색 결과는 OpenAPI 부재 검증 테스트만 남았다.
- 제거 후 `./gradlew test --tests com.saynow.OpenApiIntegrationTest`가 통과했다.
- 목록 API 회귀 확인으로 `./gradlew test --tests com.saynow.scenario.ScenarioApiIntegrationTest`를 실행했고 통과했다.
- 최종 검증은 `rg`로 상세 조회 참조 검색, `git diff --check`, `./gradlew test` 순서로 실행했다.
- 시나리오 목록 조회 API도 상세 조회의 `situationDescription`과 같은 설명 값을 각 시나리오별로 반환해야 한다.
- 새 DB 컬럼이나 마이그레이션은 필요 없다. 이미 `scenarios.situation_description`과 `Scenario.situationDescription`이 존재하므로 목록 DTO와 매핑만 확장한다.
- 기존 목록 응답의 내부 슬롯 비공개 정책은 유지한다. 추가 필드는 설명 텍스트 하나로 제한한다.
- 프로덕션 코드 변경 전 `./gradlew test --tests com.saynow.scenario.ScenarioApiIntegrationTest`를 실행했고, 목록 응답의 `situationDescription` 경로가 없어 두 테스트가 실패했다.
- `ScenarioSummaryResponse`에 필드를 추가하고 `ScenarioService.toSummaryResponse`에서 기존 상세 응답과 같은 `scenario.getSituationDescription()` 값을 넣도록 했다.
- 수동 OpenAPI 예시도 목록 응답에 같은 `situationDescription` 값을 포함하도록 맞췄다.
- 검증은 `./gradlew test --tests com.saynow.scenario.ScenarioApiIntegrationTest`, `./gradlew test --tests com.saynow.OpenApiIntegrationTest`, `git diff --check`, `./gradlew test` 순서로 실행했다.

## 2026-05-09

- AI 서버 주소는 `http://43.202.146.182:8080`이다.
- AI 서버 OpenAPI 기준 턴 평가는 `POST /api/v1/turn-evaluations`를 사용한다.
- 요청은 `multipart/form-data`이며 `audio` 파일 파트와 `payload` 문자열 파트를 받는다.
- `payload`의 내부 JSON 스키마는 Swagger에 명확히 문서화되어 있지 않다. 그래서 백엔드에서는 현재 세션, 시나리오, 슬롯 정보를 평탄 필드와 `scenario` 중첩 필드로 함께 보낸다.
- AI 응답의 `nextQuestion`에는 `ttsAudio`가 포함될 수 있지만, 현재 MVP 방향은 클라이언트 TTS이므로 백엔드는 이를 저장하거나 URL로 변환하지 않고 무시한다.
- 기본 실행 모드는 `local`로 유지한다. 테스트와 일반 로컬 개발이 외부 AI 서버 상태에 의존하지 않도록 하기 위함이다.
- 실제 AI 서버 호출은 `SAYNOW_AI_CLIENT_MODE=remote`일 때만 활성화한다.
- 백엔드 원격 모드 기동은 현재 로컬 PostgreSQL이 떠 있지 않아 실패했다. 오류는 AI 클라이언트가 아니라 Flyway의 `localhost:5432` 연결 실패다.
- 턴 평가 payload는 검증 에러 기준으로 `currentQuestion.questionText`, `turn.turnIndex`, `turn.inputType`, `turn.speechStartedAfterMs`, `turn.recordingDurationMs`가 필수다.
- 같은 오디오 파일을 `/stt`에 직접 보내면 `{"success":false,"data":null,"error":"'dict' object has no attribute 'avg_logprob'"}`가 반환된다. 따라서 현재 완전한 원격 턴 평가는 AI 서버 STT 처리 오류로 막힌다.
- 검증 명령은 `./gradlew test --tests com.saynow.practice.infrastructure.ai.RemoteAiPracticeClientTest`, `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest`, `./gradlew test --rerun-tasks`, `git diff --check`를 실행했다.
- 운영 프로파일에서는 별도 환경변수가 없어도 `RemoteAiPracticeClient`를 사용하도록 `application-prod.yml`에 `saynow.ai.client-mode=${SAYNOW_AI_CLIENT_MODE:remote}`를 추가했다. 운영에서도 필요하면 환경변수로 `local`을 강제할 수 있다.
- 운영 프로파일 검증 테스트에서 `RestClient.Builder` Bean이 없으면 `RemoteAiPracticeClient` 생성이 실패한다는 점을 확인했다. 그래서 `AiClientConfiguration`에서 `RestClient.Builder` Bean을 명시적으로 제공한다.
- 이어서 `ObjectMapper` Bean도 없다는 점이 드러났다. 원격 클라이언트가 운영 프로파일에서 안정적으로 생성되도록 `AiClientConfiguration`에서 `ObjectMapper` Bean도 제공한다.
- AWS의 AI 서버 라우터와 Pydantic 모델을 확인한 결과, 턴 평가 요청은 `scenario.requiredSlots`를 읽는다. 기존 원격 클라이언트의 `scenario.slots` 필드는 AI 서버가 사용하지 않으므로 `requiredSlots`로 수정했다.
- AWS 계정 `494873119837`에서 실행 중인 인스턴스는 `prod-saynow-backend` (`13.209.216.213`)와 `prod-saynow-ai-backend` (`43.202.146.182`)다.
- AI EC2 내부에서 `/tts`로 만든 MP3를 `/stt`에 넣어도 동일하게 `{"success":false,"data":null,"error":"'dict' object has no attribute 'avg_logprob'"}`가 재현됐다.
- AI 서버 원인은 `/opt/saynow/app/services/stt_service.py`의 `avg_logprob = sum(s.avg_logprob for s in segments) / len(segments)`다. OpenAI `verbose_json` 응답의 `segments` 원소가 dict인데 객체 속성처럼 접근해서 `AttributeError`가 난다.
- AI 서버 턴 평가도 같은 STT 함수 `transcribe_with_confidence`를 호출하므로 `/api/v1/turn-evaluations`의 500도 같은 원인이다.
- 현재 운영 백엔드 JAR에는 `LocalAiPracticeClient.class`만 있고 `RemoteAiPracticeClient`, `AiPracticeClient`, `AiClientProperties` 클래스가 없다. 따라서 이 브랜치가 배포되기 전 운영 백엔드는 AI 서버를 실제 호출하지 않는다.
- AI 서버 OpenAPI 기준 최종 피드백은 `POST /api/v1/session-feedbacks`를 사용한다. 요청은 JSON이며 `scenarioId`, `scenarioGoal`, `turns[]`를 보낸다. `turns[]`에는 `question`, `transcript`, `responseTimeSec`가 필요하다.
- `curl`로 AI 서버 `/api/v1/session-feedbacks`를 직접 호출했을 때 200 응답과 `totalUnderstoodScore`, `summary`, `turns[]`가 반환되는 것을 확인했다.
- 백엔드 remote client에도 최종 피드백 호출을 추가했다. `SAYNOW_REMOTE_AI_SMOKE_TEST=true ./gradlew test --tests com.saynow.practice.RemoteAiServerApiSmokeTest`로 턴 평가와 최종 피드백 AI 서버 호출이 모두 통과했다.
- 최종 피드백 요청 payload는 Obsidian 문서 계약에 맞춰 `sessionId`, `scenario`, `scenarioResult`, `filledSlots`, `turns[]`를 보내도록 변경했다. 턴별 필드는 `turnId`, `turnIndex`, `questionText`, `userTranscript`, `speechStartedAfterMs`, `recordingDurationMs`다.
- Lombok 리팩터링은 기존 동작을 바꾸지 않는 범위에서 진행한다. Spring component 생성자는 `@RequiredArgsConstructor`로 대체하고, JPA 엔티티에는 `@Data`를 쓰지 않는다.
- JPA 엔티티는 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`만 적용했다. `PracticeTurn`, `TurnFeedback`은 기존에 없던 association/internal field getter가 생기지 않도록 필드 단위 `@Getter`로 제한했다.
- AI 서버 레포에서 STT segments dict 처리와 백엔드 시나리오 payload 기반 턴 평가를 수정해 main에 push했고, GitHub Actions 배포 성공 후 직접 curl 호출이 200으로 바뀌었다.
- 백엔드 smoke test에서 Spring `RestClient` multipart 요청은 본문에 `audio`, `payload`가 있어도 AI 서버가 422 missing으로 판단했다.
- 수동 curl multipart는 같은 AI 서버에서 200을 반환했다. Java 표준 `HttpClient`로 직접 multipart를 만들고 HTTP/1.1을 명시하자 백엔드 세션 API smoke test가 실제 AI 서버까지 200으로 통과했다.

## 2026-05-10

- 작업 대상을 `feat/3`에서 `main`으로 전환했다. 전환 직후 `main`은 `origin/main`과 같은 `74e0e3d` 커밋이다.
- Swagger UI의 `POST /api/v1/sessions/{sessionId}/turns` 화면은 `multipart/form-data`에서 `audio` 파일 파트와 `request` 객체 파트를 입력하게 보여준다.
- 현재 `main` 컨트롤러는 `@RequestPart("audio") MultipartFile audio`와 `@Valid @ModelAttribute SubmitTurnRequest request` 조합이다. 이 구현은 `request` JSON 파트가 아니라 `inputType`, `speechStartedAfterMs`, `recordingDurationMs` 최상위 form field를 기대한다.
- 사용자가 Swagger UI대로 `request` 객체에 `{ "inputType": "AUDIO", "speechStartedAfterMs": 1000, "recordingDurationMs": 1000 }`를 입력하면 `inputType`이 DTO에 바인딩되지 않아 `VALIDATION_FAILED` 400이 발생한다.
- 수정 방향은 Swagger 계약에 맞춰 API가 `request` JSON part를 받도록 바꾸는 것이다.
- RED 검증으로 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest.submitsTurnWithSwaggerMultipartRequestPart`를 실행했고, 200을 기대했지만 400 `VALIDATION_FAILED`가 반환되는 것을 확인했다.
- 컨트롤러를 `@Valid @RequestPart("request") SubmitTurnRequest request`로 변경했다. 이후 턴 제출 테스트와 수동 smoke test 요청도 `request` JSON part를 보내도록 맞췄다.
- OpenAPI 테스트에는 턴 제출 requestBody의 `multipart/form-data` 스키마에 `audio`와 `request` property가 존재하는지 확인하는 검증을 추가했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest.submitsTurnWithSwaggerMultipartRequestPart`와 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest --tests com.saynow.OpenApiIntegrationTest`를 실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.
- 현재 오디오 파일 검증은 확장자 자체가 아니라 `MultipartFile.getContentType()` 기준이다.
- 허용 MIME은 `audio/webm`, `audio/wav`, `audio/x-wav`, `audio/mpeg`, `audio/mp4`, `audio/x-m4a`다.
- 사용자가 mp3 업로드 시 415 `UNSUPPORTED_AUDIO_TYPE`을 확인했다. 기존 코드상 mp3 표준 MIME인 `audio/mpeg`은 허용하지만, 브라우저나 Swagger 환경이 `audio/mp3`로 보낼 경우 거절된다.
- m4a도 `audio/mp4`, `audio/x-m4a`는 허용하지만, 환경에 따라 `audio/m4a`로 오면 거절된다.
- 이번 수정은 확장자만으로 파일 타입을 신뢰하지 않고 `audio/mp3`, `audio/m4a` MIME alias를 추가하는 범위로 제한한다.
- RED 검증으로 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest.acceptsMp3AudioContentTypeAlias --tests com.saynow.practice.PracticeSessionApiIntegrationTest.acceptsM4aAudioContentTypeAlias`를 실행했고, 두 케이스 모두 415 `UNSUPPORTED_AUDIO_TYPE`으로 실패하는 것을 확인했다.
- 허용 MIME 목록에 `audio/mp3`, `audio/m4a`를 추가했다.
- `PracticeSessionApiIntegrationTest` 전체 실행 중 기존 테스트가 `turnId` DB 전역 시퀀스 값을 `1`, `2`로 고정해 실패했다. 턴 순서는 `turnIndex`로 검증하고 있으므로 `turnId`는 존재 여부만 확인하도록 조정했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest.acceptsMp3AudioContentTypeAlias --tests com.saynow.practice.PracticeSessionApiIntegrationTest.acceptsM4aAudioContentTypeAlias`와 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest`를 실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.
- 사용자가 WebM 업로드도 415로 실패한다고 보고했다.
- 현재 `main` 기준 WebM 허용 값은 `audio/webm` 정확 일치뿐이다. 실제 브라우저, Swagger, 녹음 API 환경에서는 `.webm`이 `video/webm` 또는 `audio/webm;codecs=opus`처럼 들어올 수 있다.
- 이번 수정은 `video/webm`을 WebM audio container alias로 허용하고, `Content-Type`의 `;codecs=...` 같은 MIME 파라미터를 제거해 검증한다.
- RED 검증으로 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest.acceptsWebmVideoContentTypeAlias --tests com.saynow.practice.PracticeSessionApiIntegrationTest.acceptsWebmAudioContentTypeWithCodecParameter`를 실행했고, 두 케이스 모두 415 `UNSUPPORTED_AUDIO_TYPE`으로 실패하는 것을 확인했다.
- 허용 MIME 목록에 `video/webm`을 추가하고, `Content-Type`에서 `;` 이후 MIME 파라미터를 제거한 뒤 소문자로 정규화해 비교하도록 변경했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest.acceptsWebmVideoContentTypeAlias --tests com.saynow.practice.PracticeSessionApiIntegrationTest.acceptsWebmAudioContentTypeWithCodecParameter`와 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest`를 실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.
- 첨부 파일 `/Users/sangmin8817/Desktop/output.mp3`는 로컬 `file --mime-type` 기준 `audio/mpeg`이다.
- 배포 서버 `http://13.209.216.213:8080`에 실제 세션을 만들고 `curl -F audio=@/Users/sangmin8817/Desktop/output.mp3`로 업로드하자 415 `UNSUPPORTED_AUDIO_TYPE`이 재현됐다.
- `curl --trace-ascii`로 multipart를 확인한 결과 `audio` part가 `Content-Type: application/octet-stream`으로 전송됐다. 따라서 파일 자체가 mp3여도 서버는 MIME 기준으로 generic binary로 판단해 거절한다.
- 사용자가 `audio.contentType()` 기반 검증을 제거하자고 방향을 바꿨다. 따라서 generic MIME 보정, MIME alias 맵, 확장자 fallback 맵 방향은 폐기했다.
- `validateTurnSubmitRequest`에서는 `inputType`, 빈 파일, 최대 크기만 검증하고, `Content-Type`은 더 이상 `UNSUPPORTED_AUDIO_TYPE`으로 거절하지 않는다.
- 기존에 `UNSUPPORTED_AUDIO_TYPE`을 기대하던 테스트는 `application/octet-stream` mp3와 임의 Content-Type 파일이 모두 진행되는 테스트로 바꿨다.
- 검증으로 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest`를 실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.
- 테스트를 실제 이슈 중심으로 단순화한 뒤 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest`와 `./gradlew test`를 다시 실행했고 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.
- 사용자가 같은 mp3를 넣었는데도 415가 난다고 보고했다.
- 운영 서버에서 `request` part에 `type=application/json`을 명시한 curl은 200으로 성공했다.
- 같은 세션 API에 `-F 'request={...}'`처럼 request part Content-Type을 생략하면 415 `UNSUPPORTED_AUDIO_TYPE`이 재현됐다.
- 원인은 audio 검증이 아니라 `@RequestPart("request") SubmitTurnRequest`가 text/plain request part를 DTO로 변환하지 못해 Spring `HttpMediaTypeNotSupportedException`이 발생하고, 현재 전역 핸들러가 이를 `UNSUPPORTED_AUDIO_TYPE`으로 매핑하기 때문이다.
- 수정 방향은 컨트롤러가 request part를 문자열 JSON으로 받고 직접 파싱해, Swagger/curl이 text/plain으로 보내도 처리되게 하는 것이다.
- RED 검증으로 `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest.acceptsTurnRequestPartWithoutJsonContentType`를 실행했고, 415 `UNSUPPORTED_AUDIO_TYPE`으로 실패하는 것을 확인했다.
- `PracticeSessionController`가 `request` part를 `String`으로 받은 뒤 `ObjectMapper`로 `SubmitTurnRequest`를 파싱하도록 변경했다. `inputType` 누락이나 음수 메트릭은 `VALIDATION_FAILED`로 처리한다.
- `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest.acceptsTurnRequestPartWithoutJsonContentType --tests com.saynow.practice.PracticeSessionApiIntegrationTest.rejectsInvalidTurnRequestPartJson`, `./gradlew test --tests com.saynow.practice.PracticeSessionApiIntegrationTest`, `./gradlew test`를 실행했고 모두 통과했다.
- 최종 점검으로 `git diff --check`를 실행했고 통과했다.

---

# Sentry 연결 컨텍스트 노트

## 2026-05-11

- Sentry를 추가하는 목적은 현재 운영 환경에서 에러 로그 확인이 어렵기 때문이다.
- 이번 범위는 운영 에러 이벤트, 에러와 연관된 breadcrumbs, 검색 가능한 Sentry structured logs까지 포함한다.
- 성능 트레이싱, 프로파일링, OpenTelemetry Java Agent 도입은 이번 범위에서 제외한다.
- 로컬 개발과 테스트는 DSN 없이도 동작해야 하므로 Sentry 전송은 운영 환경 변수 기반으로만 활성화한다.
- `GlobalExceptionHandler`가 예외를 `ApiResponse`로 감싸기 때문에 Sentry 자동 캡처만 의존하지 않고 예외를 명시적으로 캡처한다.
- 초기 결정은 4xx 사용자 오류와 validation 오류를 Sentry 이벤트로 보내지 않는 것이었지만, 이후 사용자 요청에 따라 4xx도 전송하도록 변경했다.
- 운영 로그 전체 수집은 비용과 노이즈를 고려해 기본 `INFO` 이상을 대상으로 잡고, `DEBUG`와 `TRACE`는 제외한다.
- Sentry Java SDK는 `8.40.0`을 사용한다. Maven Repository 기준 2026-04-22 공개된 최신 8.40.x 버전이다.
- `SentryAppender`의 `minimumLevel` setter가 실제 8.40.0 JAR에 있는지 `javap`로 확인했다.
- GitHub Actions 배포는 SSM `/saynow/prod`에서 Sentry 파라미터를 선택적으로 읽는다. DB 파라미터만 필수로 유지해 기존 배포를 깨지 않게 한다.
- Sentry Java SDK 8.40.0의 `maxRequestBodySize` enum은 `NONE`, `SMALL`, `MEDIUM`, `ALWAYS`다. Spring 설정에는 문서 표현인 `never` 대신 실제 enum 값인 `NONE`을 사용한다.
- `sentry.dsn`을 빈 기본값으로 두면 DSN이 없어도 Sentry 자동 설정 조건이 매칭된다. 환경 변수 `SENTRY_DSN`이 있을 때만 Spring이 `sentry.dsn`으로 바인딩하도록 yml에서는 dsn 기본값을 제거했다.
- `SentryProdConfigurationTest`에서 fake DSN으로 운영 Sentry 옵션 바인딩을 검증했다. 실제 이벤트 전송은 발생시키지 않는다.
- 검증 명령은 `./gradlew test --tests com.saynow.common.exception.GlobalExceptionHandlerTest`, `./gradlew test --tests com.saynow.ProdAiClientModeTest`, `./gradlew test --tests com.saynow.SentryProdConfigurationTest`, `./gradlew test`, `git diff --check`를 사용했다.

---

# 이슈 템플릿 강제 컨텍스트 노트

## 2026-05-11

- GitHub 이슈 템플릿이 적용되지 않는 원인은 `.github/ISSUE_TEMPLATE.md` 단일 legacy Markdown 템플릿만 있어서 이슈 작성 화면의 입력 필드를 강제할 수 없기 때문이다.
- GitHub Issue Forms는 `.github/ISSUE_TEMPLATE/*.yml`로 관리하고 `config.yml`의 `blank_issues_enabled: false`로 웹 UI의 빈 이슈 버튼을 숨길 수 있다.
- GitHub API, GitHub CLI, 권한 있는 자동화는 템플릿을 우회할 수 있으므로 완전한 저장소 정책 강제가 필요하면 별도 issue validation workflow가 필요하다.
- 당시 Sentry 명시 캡처 정책은 `GlobalExceptionHandler` 기준 5xx `ApiException`과 예상 밖 예외만 전송하는 것이었지만, 이후 사용자 요청에 따라 4xx/validation 예외도 전송하도록 변경했다.
- 운영 Sentry 설정에서 SDK `SentryExceptionResolver`는 Spring 기본 `HandlerExceptionResolverComposite` 뒤에 실행된다. 따라서 `@RestControllerAdvice`가 처리한 4xx는 SDK 자동 리졸버에 도달하지 않고, 애플리케이션 명시 캡처 경로로 전송된다.
- 검증으로 `./gradlew test --tests com.saynow.SentryProdConfigurationTest`, `ruby -e 'require "yaml"; Dir[".github/ISSUE_TEMPLATE/*.yml"].sort.each { |file| YAML.load_file(file); puts "ok #{file}" }'`, `git diff --check`, `./gradlew test`를 실행했고 통과했다.

---

# Sentry 4xx 전송 컨텍스트 노트

## 2026-05-11

- 사용자 판단에 따라 4xx 사용자 오류와 validation 오류도 Sentry 이벤트로 전송한다.
- `GlobalExceptionHandler`가 처리하는 `ApiException`, validation, bad request, unsupported media, upload size 예외를 모두 명시 캡처 대상으로 바꾼다.
- Spring Security의 인증 실패는 `GlobalExceptionHandler`를 거치지 않고 `AuthFailureResponseWriter`에서 직접 응답하므로, 401/403도 writer에서 Sentry 이벤트를 생성해야 한다.
- Sentry SDK MVC 리졸버 순서는 그대로 Spring 예외 핸들러 뒤에 둔다. 4xx 전송은 자동 리졸버가 아니라 애플리케이션의 명시 캡처 정책으로 보장한다.
- RED 검증으로 `./gradlew test --tests com.saynow.common.exception.GlobalExceptionHandlerTest --tests com.saynow.auth.SecurityAuthenticationIntegrationTest`를 실행했고, 4xx `ApiException`, validation 예외, 인증 실패 401 캡처 기대 테스트가 실패하는 것을 확인했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.common.exception.GlobalExceptionHandlerTest --tests com.saynow.auth.SecurityAuthenticationIntegrationTest --tests com.saynow.SentryProdConfigurationTest`와 `./gradlew test`를 실행했고 통과했다.

---

# 2차 MVP 도메인 명칭 정리 컨텍스트 노트

## 2026-05-19

- 사용자 피드백에 따라 2차 MVP ERD와 실제 테이블 이름을 기준으로 코드 명칭을 다시 정렬한다.
- `members` 테이블 개념이 사라지고 `users` 테이블이 기준이므로 엔티티와 저장소는 `Member`가 아니라 `User`로 둔다.
- 2차 MVP ERD의 세션 테이블은 `sessions`, `session_turns`, `session_slot_statuses`이므로 `practice` 패키지와 `PracticeSession`, `PracticeTurn` 이름을 `session` 패키지와 `Session`, `SessionTurn`으로 정리한다.
- 시나리오 카테고리 테이블은 `categories`이므로 `ScenarioCategory`는 `Category`로 정리한다.
- 인증 응답의 신규 가입 여부도 `newMember` 대신 `newUser`로 맞춘다.
- `src/main`, `src/test`에서 `Member`, `Practice`, `ScenarioCategory`, `newMember` 잔여 문자열을 검색했고 남아 있지 않았다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.

---

# 발화 하트 차감 정책 컨텍스트 노트

## 2026-05-19

- 사용자 발화마다 하트를 무조건 차감하는 것이 아니라, AI 서버의 `filledSlots` 응답이 빈 배열일 때만 동문서답으로 보고 하트를 1 차감한다.
- 슬롯이 하나라도 반환되면 백엔드는 해당 슬롯을 `SESSION_SLOT_STATUS`에 반영하고 하트는 유지한다.
- 성공 여부는 여전히 백엔드가 `SESSION_SLOT_STATUS` 전체 충족 여부로 판단한다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest`를 실행했고, 슬롯이 채워진 발화의 하트 유지 기대값에서 실패하는 것을 확인했다.
- 구현은 `SessionService.submitUtterance`에서 `aiResponse.filledSlots().isEmpty()`일 때만 `session.decreaseHeart()`를 호출하도록 제한했다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest`를 실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test --rerun-tasks`를 실행했고 통과했다.

---

# 사용자 발화 제출 API 명세 반영 컨텍스트 노트

## 2026-05-19

- 사용자 발화 제출 API의 요청 필드명과 응답 필드는 이미 새 명세의 `userUtterance`, `originalQuestion`, `translatedQuestion`, `remainingHearts`, `feedbackAvailable`과 맞아 있었다.
- 차이는 오류 코드였다. 빈 발화는 `VALIDATION_FAILED`, 완료 세션 재요청은 `SESSION_ALREADY_ENDED`, 다른 사용자 세션 접근은 `SESSION_ACCESS_DENIED`로 반환되고 있었다.
- 새 명세에 맞춰 발화 제출 API는 빈 발화에 `INVALID_REQUEST`, 완료 세션 재요청에 `SESSION_ALREADY_COMPLETED`, 다른 사용자 세션 접근에 `FORBIDDEN`을 반환한다.
- `AI_RESPONSE_INVALID`의 기본 메시지는 명세 문구인 `AI 응답을 처리할 수 없습니다.`로 맞춘다.
- 더 이상 사용하지 않는 `SESSION_ACCESS_DENIED`, `SESSION_ALREADY_ENDED` 오류 코드는 제거한다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.OpenApiIntegrationTest`를 실행했고, 새 오류 코드 기대값에서 실패하는 것을 확인했다.
- GREEN 검증으로 같은 명령을 재실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test --rerun-tasks`를 실행했고 통과했다.

---

# 발화 성공 시 시나리오 클리어 저장 컨텍스트 노트

## 2026-05-19

- 시나리오 성공 판정은 발화 제출 API에서 백엔드가 수행하므로, 사용자별 클리어 기록도 같은 트랜잭션에서 저장한다.
- 기존 구현은 `sessions.status = SUCCESS`는 발화 제출 시 저장하지만, 사용자별 시나리오 진행 상태의 `cleared = true`는 피드백 생성 API에서 저장했다.
- 새 기준은 `SessionService.submitUtterance`에서 모든 슬롯이 충족되어 성공 처리되는 즉시 `UserScenarioProgress.markCleared()`를 호출하는 것이다.
- 피드백 생성 API는 더 이상 사용자별 클리어 기록을 변경하지 않고 피드백 생성만 담당한다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest`를 실행했고, 피드백 생성 전 홈 목록의 클리어 상태 기대값에서 실패하는 것을 확인했다.
- GREEN 검증으로 같은 명령을 재실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test --rerun-tasks`를 실행했고 통과했다.

---

# MVP2 프론트 CORS origin 교체 컨텍스트 노트

## 2026-05-21

- 요청 CORS origin 목록은 `http://localhost:3000`, `http://10.0.2.2:3000`, `http://172.16.103.142:3000`, `http://192.168.219.107:3000`, `https://saynow-fe-web-git-mvp2-with-ai.vercel.app`이다.
- develop SSM `/saynow/develop/SAYNOW_CORS_ALLOWED_ORIGINS`는 존재하지 않았고, dev 서버는 코드 기본값을 사용 중이었다.
- 런타임 반영을 위해 develop SSM에 요청 목록을 설정하고 dev 배포로 `/opt/saynow/.env`를 갱신한다.
- 운영 SSM은 이번 작업에서 건드리지 않는다.
- CORS 변경 커밋 `e41a4e2`를 `origin/develop`에 push했고, dev 배포 run `26223225452`가 성공했다.
- 배포 후 `https://dev-saynow.p-e.kr/api/v1/scenarios`에 OPTIONS preflight를 보내 요청 origin 5개가 모두 `Access-Control-Allow-Origin`으로 그대로 반환되는 것을 확인했다.

---

# 시나리오 상황 필드 응답 추가 컨텍스트 노트

## 2026-05-25

- `develop` 로컬 브랜치는 `origin/develop`과 1개씩 갈라져 있었다. 기능 기준점은 최신 원격 개발 브랜치로 잡기 위해 `git fetch origin develop` 후 `origin/develop`의 `4ea01ca`에서 `feat/15`를 생성했다.
- 프론트-백엔드 계약 변경 대상은 `GET /api/v1/scenarios` 목록 응답이다.
- 응답 필드명은 기존 `scenarioTitle`, `scenarioGoal`, `scenarioEmoji` prefix 패턴에 맞춰 `scenarioSituation`으로 둔다.
- 현재 `origin/develop` 코드에는 `scenarios` 테이블, `Scenario` 엔티티, 목록 DTO에 상황 필드가 없다. 응답 값을 내려주려면 DB migration, 엔티티 매핑, DTO, 서비스 매핑, OpenAPI 예시를 함께 수정해야 한다.
- 기준선 확인으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.OpenApiIntegrationTest`를 실행했고 통과했다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.OpenApiIntegrationTest --tests com.saynow.scenario.ScenarioSchemaIntegrationTest`를 실행했고, `scenarioSituation` JSON path와 `scenarios.situation` 컬럼이 없어 실패했다.
- 구현은 새 migration `V2__add_scenario_situation.sql`로 `scenarios.situation` 컬럼과 기본 카페 시나리오 3개 상황 값을 추가하고, 응답 DTO에서는 `scenarioSituation`으로 노출한다.
- GREEN 검증으로 RED와 같은 테스트 명령을 재실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.

---

# 공항 시나리오 seed 추가 컨텍스트 노트

## 2026-05-26

- 사용자 확정 문구에 따라 3단계 `translated_question`은 `괜찮으세요? 무슨 일 있으신가요?`로 둔다.
- 현재 `Airport` 카테고리는 V1 seed에 이미 존재하지만, 서비스 정책상 `Cafe`가 아닌 카테고리는 `COMING_SOON`으로 잠겨 목록 API에서 시나리오 배열이 비어 내려간다.
- 이번 작업은 DB seed 추가에 한정한다. Airport 잠금 해제와 세션 시작 허용 정책 변경은 별도 의사결정이 필요하다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest`를 실행했고, Airport 시나리오 seed가 없어 실패했다.
- 구현은 새 migration `V3__seed_airport_scenarios.sql`로 Airport display order 1~3 시나리오와 각 슬롯을 추가했다. 명시 id 4~6을 사용한 뒤 `scenarios` identity 시작값을 7로 맞췄다.
- GREEN 검증으로 같은 테스트 명령을 재실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.

---

# 기본 카테고리 Airport 전환 컨텍스트 노트

## 2026-05-26

- 기존 seed 카테고리는 V1 기준 `Cafe`, `Airport`, `Hotel`, `Restaurant` 네 개다.
- 사용자 요청에 따라 이제 기본 열린 카테고리는 `Airport`다. `Cafe`는 다른 비기본 카테고리와 마찬가지로 `COMING_SOON` 잠금으로 처리한다.
- 잠금 기준은 시나리오 목록 API의 카테고리 노출과 세션 시작 API의 카테고리 허용 로직에 동시에 적용해야 한다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.feedback.FeedbackStreamIntegrationTest --tests com.saynow.OpenApiIntegrationTest`를 실행했고, Airport 시나리오 시작이 아직 잠겨 있고 OpenAPI 예시가 Cafe 기준이라 실패했다.
- `ScenarioService`와 `SessionService`의 열린 카테고리 기준을 `Airport`로 변경했다.
- Airport 시나리오는 슬롯 3개를 채워야 완료되므로 기존 Cafe 기준 통합 테스트의 완료 턴 수 기대값을 3턴으로 조정했다.
- OpenAPI 시나리오 목록 예시는 Cafe 잠금, Airport 열림 기준으로 바꿨고, 세션 시작 성공 예시도 Airport 첫 질문으로 맞췄다.
- GREEN 검증으로 RED와 같은 테스트 명령을 재실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.

---

# 카테고리 seed 복구 컨텍스트 노트

## 2026-05-26

- 사용자가 기존 DB의 카테고리 데이터도 초기화됐다고 알려줬다.
- `V3__seed_airport_scenarios.sql`는 Airport 카테고리 `id=2`가 이미 존재한다고 가정하고 있었기 때문에, 카테고리가 비어 있으면 시나리오 insert가 FK 제약에서 실패할 수 있었다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest.airportSeedMigrationRestoresCategoriesBeforeScenarios`를 실행했고, V3에 카테고리 insert가 없어 실패했다.
- 처음에는 PostgreSQL `ON CONFLICT`로 카테고리 복구를 작성했지만 H2 PostgreSQL 모드가 해당 문법을 지원하지 않아 Flyway 적용 중 실패했다. 실제 에러는 `ON CONFLICT (id) DO UPDATE SET` 구문에서 발생했다.
- PostgreSQL과 H2 모두 통과하도록 `UPDATE` 후 `INSERT ... WHERE NOT EXISTS` 방식으로 기본 카테고리 4개를 복구한다.
- GREEN 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioSchemaIntegrationTest`를 실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.

---

# 세션 중 영어 학습 가이드 모드 컨텍스트 노트

## 2026-05-26

- 사용자는 가이드 모드 대화를 저장하지 않는다고 확정했다.
- 프론트-백엔드 API는 `POST /api/v1/sessions/{sessionId}/guide`로 두고, 프론트는 `question`만 보낸다.
- `sessionId`는 저장 키가 아니라 백엔드가 세션 소유권, 진행 상태, 시나리오 문맥을 신뢰할 수 있게 조회하기 위한 키다.
- 백엔드는 세션의 `scenarioTitle`, `scenarioGoal`, `scenarioSituation`, `aiRole`을 조회해 AI 서버 `POST /api/v1/conversation/guide`에 전달한다.
- 가이드 질문과 답변은 `SessionTurn`에 저장하지 않으므로 최종 피드백 생성 대상에도 포함되지 않아야 한다.
- 명백한 프롬프트 인젝션, 시스템 지시 공개, 역할 변경, 코딩/뉴스/금융/정치 등 영어 학습 목적 밖의 요청은 AI 서버 호출 전에 차단하고 안내 답변을 반환한다.
- 원격에는 `origin/dev`가 없어서 실제 기준 브랜치인 `origin/develop`을 fetch한 뒤 `feat/16`을 생성했다.
- RED 검증으로 `./gradlew test --tests com.saynow.scenario.ScenarioFlowIntegrationTest --tests com.saynow.session.infrastructure.ai.RemoteAiConversationClientTest --tests com.saynow.OpenApiIntegrationTest`를 실행했고, `AiGuideRequest`와 `AiGuideResponse`가 없어 컴파일 실패했다.
- 구현은 `POST /api/v1/sessions/{sessionId}/guide`를 추가하고, 프론트 요청은 `question`만 받으며, 백엔드가 세션 문맥으로 `AiGuideRequest`를 구성한다.
- `AiConversationClient.generateGuide`와 원격 AI 경로 `/api/v1/conversation/guide`를 추가했다. 원격 가이드 응답이 비어 있거나 호출에 실패하면 `AI_GENERATION_FAILED`를 반환한다.
- 차단 대상 질문은 AI 서버를 호출하지 않고 `이 기능은 영어 표현, 문법, 단어, 뉘앙스에 관한 질문만 도와드릴 수 있어요.`를 반환한다.
- GREEN 검증으로 RED와 같은 테스트 명령을 재실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.
- 패치 공백 검증으로 `git diff --check`를 실행했고 통과했다.
- 사용자 요청에 따라 프론트 가이드 응답에서 `sessionId`를 제거했다. `sessionId`는 path variable로만 사용하고 응답 본문에는 `answer`만 내려준다.

---

# 세션 NPS 평가 수집 API 컨텍스트 노트

## 2026-05-30

- 사용자는 특정 유저가 특정 세션에 대해 제출하는 평가를 수집한다고 확정했다.
- API는 기존 세션 하위 리소스 패턴에 맞춰 `POST /api/v1/sessions/{sessionId}/nps`로 둔다.
- `userId`는 인증 토큰에서 얻고, `sessionId`는 path variable로 받아 저장한다. 요청 body에 `sessionId`를 중복으로 받지 않는다.
- 성공 응답은 프론트가 별도 식별자를 사용할 필요가 없으므로 `201 Created`와 `data: null`로 반환한다.
- NPS는 AI 최종 피드백 결과가 아니라 사용자가 제출한 만족도 평가이므로 `session_feedbacks`에 컬럼을 붙이지 않고 `session_nps_responses` 별도 테이블로 저장한다.
- `score`는 1~5 정수만 허용한다.
- `lowScoreReason`은 1~2점에서만 의미 있는 선택 입력이다. 3~5점에서 공백이 아닌 사유가 오면 데이터 의미가 섞이지 않도록 `INVALID_REQUEST`로 거부한다.
- 같은 사용자가 같은 세션에 한 번만 평가하도록 `(user_id, session_id)` unique 제약을 둔다.
- 진행 중인 세션은 평가할 수 없으며 `SESSION_IN_PROGRESS`로 응답한다. 완료, 실패, 중도 종료 세션은 소유자라면 평가 대상이 될 수 있다.
- RED 검증으로 `./gradlew test --tests com.saynow.nps.SessionNpsApiIntegrationTest --tests com.saynow.nps.SessionNpsSchemaIntegrationTest --tests com.saynow.OpenApiIntegrationTest`를 실행했고, NPS 경로와 테이블이 없어 9개 테스트가 실패했다.
- 구현은 `nps` 패키지에 컨트롤러, 요청 DTO, 서비스, 엔티티, 저장소를 추가하고 `V6__create_session_nps_responses.sql`로 별도 저장 테이블을 생성했다.
- GREEN 검증으로 같은 테스트 명령을 재실행했고 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.
- 패치 공백 검증으로 `git diff --check`를 실행했고 통과했다.
