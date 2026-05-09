# 소셜 로그인 OIDC 컨텍스트 노트

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

---

# AI 서버 연동 컨텍스트 노트

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
