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
