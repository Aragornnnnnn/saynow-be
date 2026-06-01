# SayNow 3차 MVP BE 구현 체크리스트

- [x] 기준 문서 `/Users/sangmin8817/기타 자료/Obsidian/SayNow/3차 MVP.md`를 다시 읽고 최우선 목표가 피드백 품질임을 확인한다.
- [x] 현재 BE 스키마, 도메인, 서비스, API, AI 계약을 읽고 3차 MVP 문서와 충돌하는 지점을 확인한다.
- [x] 시나리오 목록 응답이 `briefing`, `conversationGoal`, `firstQuestionPreview`, `completed`를 반환하는 RED 테스트를 작성하고 실패를 확인한다.
- [x] 세션 시작 응답이 `currentTurn`과 `progress`를 반환하는 RED 테스트를 작성하고 실패를 확인한다.
- [x] 발화 제출 응답이 `submittedTurn`, `nextTurn`, `progress`, `turnFeedbackStatus`를 반환하고 4번째 답변 뒤 대화 완료 상태가 되는 RED 테스트를 작성하고 실패를 확인한다.
- [x] 최종 피드백 API가 `nativeScore`, `nativeLevelLabel`, `summary`, 새 턴별 피드백 구조를 저장하고 반환하는 RED 테스트를 작성하고 실패를 확인한다.
- [x] AI `next-question`, `turn-feedback`, `session-feedback` 요청과 응답 계약 RED 테스트를 작성하고 실패를 확인한다.
- [x] Flyway migration으로 3차 MVP 스키마를 추가한다.
- [x] `ScenarioQuestion`, 3차 MVP 시나리오/세션/피드백 도메인 매핑을 추가한다.
- [x] 슬롯, 하트, 성공/실패 기반 세션 진행 코드를 제거하거나 3차 MVP 흐름에서 분리한다.
- [x] `GET /api/v1/scenarios`, `POST /api/v1/scenarios/{scenarioId}/sessions`, `POST /api/v1/sessions/{sessionId}/utterances`, `POST /api/v1/sessions/{sessionId}/feedback`, `PATCH /api/v1/sessions/{sessionId}/abandon`, `POST /api/v1/sessions/{sessionId}/nps`를 문서 기준으로 맞춘다.
- [x] 문서 범위 밖인 guide, result, feedback SSE 경로를 제거한다.
- [x] OpenAPI 예시와 통합 테스트를 3차 MVP 계약으로 갱신한다.
- [x] 관련 테스트를 통과시킨다.
- [x] 전체 `./gradlew test`를 실행한다.
- [x] 변경 사항, 결정 이유, 검증 결과를 Obsidian 3차 MVP 문서에 남긴다.
- [x] `git diff --check`와 `git status --short`로 최종 변경 범위를 확인한다.
- [x] 의미 있는 단위로 커밋한다.

---

# turnClassification 기반 하트/슬롯 정책 체크리스트

- [x] 현재 `SessionService`, AI 응답 DTO, OpenAPI 테스트 구조를 확인한다.
- [x] 구현 전 정책과 기존 코드 차이를 `context-notes.md`에 기록한다.
- [x] `INVALID_RESPONSE`가 `filledSlots`를 반환해도 슬롯을 적용하지 않는 RED 테스트를 추가한다.
- [x] `ASSISTANCE_REQUEST`가 `filledSlots`를 반환해도 슬롯을 적용하지 않고 하트를 차감하지 않는 RED 테스트를 추가한다.
- [x] `ANSWER`일 때만 `filledSlots`를 세션 슬롯 상태에 반영한다.
- [x] AI `turnClassification` 누락/알 수 없는 값 계약 테스트를 확인한다.
- [x] OpenAPI 응답 예시에 `turnClassification`, `heartDeducted`, `remainingHearts`가 포함됨을 확인한다.
- [x] 관련 테스트와 전체 `./gradlew test`를 실행한다.
- [x] Obsidian AI 작업 기록에 BE 변경 사항을 함께 정리한다.
- [x] 변경 범위를 점검하고 커밋한다.

---

# AI SSE 피드백 스트림 중계 체크리스트

# AI 피드백 슬롯 데이터 요청 추가 체크리스트

- [x] AI 요청 계약 변경 범위를 확인한다.
- [x] 피드백 원격 요청 JSON에 `slots`가 포함되는 RED 테스트를 추가한다.
- [x] 동기 피드백 API가 `slots`를 AI 요청에 포함하는 RED 테스트를 추가한다.
- [x] SSE 피드백 API가 `slots`를 AI 요청에 포함하는 RED 테스트를 추가한다.
- [x] `AiFeedbackRequest`에 `slots`를 추가한다.
- [x] `FeedbackService` 공통 요청 생성 경로에서 세션 슬롯 상태와 슬롯 설명을 매핑한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.
- [x] develop으로 push한다.

---

# 시나리오 슬롯 설명 필드 추가 체크리스트

- [x] `description` 스키마와 AI 요청 계약 변경 범위를 기록한다.
- [x] `scenario_slots.description` 컬럼과 확정 description seed 값을 검증하는 RED 테스트를 추가한다.
- [x] `next-question` AI 요청의 `slots[].description` 포함 RED 테스트를 추가한다.
- [x] Flyway migration으로 `scenario_slots.description` 컬럼과 기존 슬롯별 값을 추가한다.
- [x] `ScenarioSlot` 엔티티에 `description`을 매핑한다.
- [x] `AiSlotStatus`와 `SessionService` 매핑에 `description`을 추가한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.
- [x] develop으로 push한다.

---

# 시나리오 AI 역할 필드 추가 체크리스트

- [x] `aiRole` 스키마와 AI 요청 계약 변경 범위를 기록한다.
- [x] `scenarios.ai_role` 컬럼과 seed 값을 검증하는 RED 테스트를 추가한다.
- [x] `next-question`, `feedback`, `feedback/stream` AI 요청에 `aiRole`이 포함되는 RED 테스트를 추가한다.
- [x] Flyway migration으로 `scenarios.ai_role` 컬럼과 기존 시나리오별 값을 추가한다.
- [x] `Scenario` 엔티티에 `aiRole`을 매핑한다.
- [x] AI 요청 DTO와 서비스 매핑에 `aiRole`을 추가한다.
- [x] 프론트 시나리오 목록 응답에는 `aiRole`이 노출되지 않는지 확인한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.

---

# 공항 시나리오 dev 실환경 진행 체크리스트

- [x] develop 배포 workflow 완료 상태를 확인한다.
- [x] dev 환경에 임시 사용자를 만들고 Bearer token을 준비한다.
- [x] `GET /api/v1/scenarios`로 Airport 시나리오 1~3 노출과 잠금 상태를 확인한다.
- [x] AI 요청에 `scenarioSituation`이 누락되어 dev 진행이 막히는 현상을 RED 테스트로 고정한다.
- [x] `next-question`, `feedback`, `feedback/stream` AI 요청에 `scenarioSituation`을 포함한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] develop에 재배포한다.
- [x] 공항 시나리오 1단계를 실제 세션으로 진행하고 성공 여부를 기록한다.
- [x] 공항 시나리오 2단계를 실제 세션으로 진행하고 성공 여부를 기록한다.
- [x] 공항 시나리오 3단계를 실제 세션으로 진행하고 성공 여부를 기록한다.
- [x] 세션별 request/response JSON과 품질 판단 포인트를 정리한다.

---

- [x] AI 피드백 요청에 `sessionResult`를 포함하는 계약 변경 범위를 기록한다.
- [x] 기본 피드백 API가 AI 요청에 `sessionResult`를 보내는 테스트를 먼저 작성하고 실패 확인.
- [x] SSE 피드백 API가 AI 요청에 `sessionResult`를 보내는 테스트를 먼저 작성하고 실패 확인.
- [x] 원격 AI 피드백 요청 JSON에 `sessionResult`가 직렬화되는 테스트를 먼저 작성하고 실패 확인.
- [x] `AiFeedbackRequest`에 `sessionResult` 필드 추가.
- [x] 기본 API와 SSE API 공통 요청 생성 경로에서 세션 상태를 `sessionResult`로 매핑한다.
- [x] 관련 테스트 통과.
- [x] 전체 `./gradlew test` 통과.
- [x] diff 최종 점검 후 커밋.

---

- [x] 세션 성공/실패 결과 조회 API 구현 범위를 기록한다.
- [x] 성공 세션 결과 조회 통합 테스트를 먼저 작성하고 실패 확인.
- [x] 실패 세션 결과 조회 통합 테스트를 먼저 작성하고 실패 확인.
- [x] 진행 중 세션 결과 조회 거부 통합 테스트를 먼저 작성하고 실패 확인.
- [x] 세션 결과 응답 DTO와 서비스 메서드 추가.
- [x] `GET /api/v1/sessions/{sessionId}/result` 컨트롤러 추가.
- [x] OpenAPI 응답 예시와 경로 검증 갱신.
- [x] 관련 테스트 통과.
- [x] 전체 `./gradlew test` 통과.
- [x] diff 최종 점검 후 커밋.

---

- [x] 현재 피드백 생성 API와 AI 클라이언트 구조 확인.
- [x] SSE relay 구현 계획 작성.
- [x] SSE 정상 스트림 통합 테스트를 먼저 작성하고 실패 확인.
- [x] AI error 이벤트 relay 통합 테스트를 먼저 작성하고 실패 확인.
- [x] AI SSE 연결 실패 통합 테스트를 먼저 작성하고 실패 확인.
- [x] 기존 동기 피드백 API 회귀 테스트를 먼저 작성하고 실패 확인.
- [x] AI feedback stream 클라이언트 계약 추가.
- [x] 원격 AI SSE endpoint 호출과 timeout 설정 추가.
- [x] 백엔드 SSE relay endpoint 추가.
- [x] done 수신 시 피드백 저장 확정 처리 추가.
- [x] error 또는 연결 실패 시 SSE error 이벤트 반환 처리 추가.
- [x] 관련 테스트 통과.
- [x] 전체 `./gradlew test` 통과.
- [x] diff 최종 점검 후 커밋.

---

# 로컬 네트워크 CORS origin 추가 체크리스트

- [x] main worktree를 최신 `origin/main`으로 fast-forward.
- [x] 기존 CORS 설정과 테스트 확인.
- [x] 신규 CORS origin 통합 테스트를 먼저 작성하고 실패 확인.
- [x] `http://172.30.1.89:3000`, `http://10.0.2.2:3000` 기본 허용 origin 추가.
- [x] 관련 테스트 통과.
- [x] 전체 `./gradlew test` 통과.
- [x] diff 최종 점검 후 커밋.

---

# Swagger 서버 도메인 설정 체크리스트

- [x] 현재 OpenAPI 설정과 테스트 구조 확인.
- [x] Swagger 서버 도메인 설정 계획 작성.
- [x] OpenAPI server URL 통합 테스트를 먼저 작성하고 실패 확인.
- [x] `https://saynow.p-e.kr` 기본 Swagger server URL 설정.
- [x] `SAYNOW_OPENAPI_SERVER_URL` 환경변수 override 추가.
- [x] README, `.env.example`, EC2 배포 workflow 선택 파라미터 반영.
- [x] 관련 테스트 통과.
- [x] 전체 `./gradlew test` 통과.
- [x] diff 최종 점검 후 커밋.
- [ ] PR #11에 push하고 설명 갱신.

---

# 프론트 CORS origin 허용 체크리스트

- [x] `origin/main` 기준 `feat/cors-allowed-origins` 브랜치 생성.
- [x] 현재 CORS 설정 부재와 Spring Security 인증 설정 확인.
- [x] CORS 허용 origin 구현 계획 작성.
- [x] CORS preflight 통합 테스트를 먼저 작성하고 실패 확인.
- [x] CORS allowed origins 설정 프로퍼티 추가.
- [x] Spring Security CORS 필터 활성화.
- [x] `https://saynow-fe-web.vercel.app`, `http://localhost:3000` 기본 허용 origin 등록.
- [x] 관련 테스트 통과.
- [x] 전체 `./gradlew test` 통과.
- [x] diff 최종 점검 후 커밋.

---

# 회원 탈퇴 기능 체크리스트

- [x] `origin/main` 기준 `feat/member-withdrawal` 브랜치 생성.
- [x] 기존 회원, 소셜 계정, refresh token, Spring Security 인증 구조 확인.
- [x] 회원 탈퇴 구현 계획 작성.
- [x] 탈퇴 API 통합 테스트를 먼저 작성하고 실패 확인.
- [x] 회원 탈퇴 상태 migration 추가.
- [x] 회원 탈퇴 도메인 상태와 저장소 쿼리 추가.
- [x] `DELETE /api/v1/auth/me` API 추가.
- [x] 탈퇴 시 refresh token 폐기와 social account 연결 삭제 구현.
- [x] 탈퇴 회원 access token 거부 구현.
- [x] 관련 테스트 통과.
- [x] 전체 `./gradlew test` 통과.
- [x] diff 최종 점검 후 커밋.

---

# 소셜 로그인 OIDC 체크리스트

- [x] Obsidian 문서 기준을 refreshToken JSON 반환 방식으로 정리.
- [x] 연습 세션/피드백 API를 로그인 필수로 전환하는 요구사항 확인.
- [x] 연습 세션을 회원에게 귀속하는 요구사항 확인.
- [x] 구현 계획 작성.
- [x] auth API 통합 테스트를 먼저 작성하고 실패 확인.
- [x] 인증 없이 세션 API 호출 시 401 테스트를 먼저 작성하고 실패 확인.
- [x] 로그인 후 세션 생성 시 회원 귀속 테스트를 먼저 작성하고 실패 확인.
- [x] 다른 회원 세션 접근 403 테스트를 먼저 작성하고 실패 확인.
- [x] 인증 테이블과 `practice_sessions.member_id` migration 추가.
- [x] 회원, 소셜 계정, refresh token 도메인/저장소 추가.
- [x] OIDC ID Token 검증 인터페이스와 테스트용 fake verifier 추가.
- [x] SayNow access token과 refresh token 발급/검증 구현.
- [x] 로그인, refresh, logout API 구현.
- [x] 세션/피드백 API 인증 필수와 소유자 검증 구현.
- [x] OpenAPI 테스트 보강.
- [x] 관련 테스트 통과.
- [x] 전체 `./gradlew test` 통과.
- [x] diff 최종 점검 후 커밋.
- [x] Google Web Client ID audience를 환경 변수로 연결.

---

# 2차 MVP API 재설계 체크리스트

- [x] 확정 기준과 애매한 결정 사항을 `context-notes.md`에 기록한다.
- [x] 기존 Flyway migration을 2차 MVP 기준 단일 초기 스키마로 정리한다.
- [x] 사용자, 시나리오, 세션, 턴, 슬롯 상태, 피드백 도메인을 ERD 기준으로 재구성한다.
- [x] `GET /api/v1/scenarios` 카테고리별 시나리오 목록 API를 구현한다.
- [x] `POST /api/v1/scenarios/{scenarioId}/sessions` 세션 시작 API를 구현한다.
- [x] `POST /api/v1/sessions/{sessionId}/utterances` 사용자 발화 제출 API를 구현한다.
- [x] `POST /api/v1/sessions/{sessionId}/feedback` 세션 완료 및 피드백 생성 API를 구현한다.
- [x] `DELETE /api/v1/sessions/{sessionId}` 세션 중도 종료 API를 구현한다.
- [x] AI 꼬리 질문 생성 계약을 새 명세로 교체한다.
- [x] AI 대화 피드백 생성 계약을 새 명세로 교체한다.
- [x] 관련 통합 테스트와 OpenAPI 검증을 2차 MVP 기준으로 갱신한다.
- [x] `./gradlew test`로 전체 검증한다.
- [x] 운영 배포 SSM 파라미터 문서화와 workflow 반영.
- [x] 검증 후 PR 브랜치 갱신.
- [x] 인증 구조를 Spring Security `OncePerRequestFilter` 기반으로 전환하는 테스트 추가.
- [x] MVC `AuthInterceptor`와 `ThreadLocal` 인증 컨텍스트 제거.
- [x] `UserDetails` 기반 인증 principal 추가.
- [x] Spring Security 설정과 인증 실패 JSON 응답 연결.
- [x] 인증 구조 변경 후 관련 테스트 통과.
- [x] 전체 `./gradlew test` 통과.
- [x] diff 최종 점검 후 커밋.
- [x] 운영 소셜 로그인 환경 보강 계획 작성.
- [x] AWS Parameter Store Google/Kakao audience 값 보강.
- [x] 배포 워크플로 인증 환경변수 주입 목록 추가.
- [x] 인증 관련 테스트와 워크플로 문법 검증.
- [x] `ApiResponse`에서 성공 응답 HTTP status를 지정할 수 있는 테스트를 추가한다.
- [x] 공통 응답 헬퍼와 컨트롤러 반환 타입을 상태 코드 지정 방식으로 정리한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.
- [x] AI next-question 응답의 `turnClassification` 계약을 develop 기준으로 반영한다.
- [x] 하트 차감 기준을 `INVALID_RESPONSE`로만 변경하는 통합 테스트를 먼저 추가한다.
- [x] `ASSISTANCE_REQUEST`와 `ANSWER`는 `filledSlots=[]`여도 하트가 차감되지 않게 한다.
- [x] 프론트 발화 응답에 `heartDeducted`, `turnClassification`, `remainingHearts` 정책 결과를 포함한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [ ] 변경 범위를 점검하고 커밋한다.
- [x] 사용자별 시나리오 진행 테이블 이름 변경 스키마 테스트를 추가한다.
- [x] `user_scenario_clears.is_cleared`를 `user_scenario_progress.cleared`로 변경한다.
- [x] `UserScenarioClear` 도메인과 저장소명을 `UserScenarioProgress` 기준으로 변경한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.
- [x] dev EC2 배포 workflow 기대 동작을 테스트로 추가한다.
- [x] dev profile이 배포 환경에서 원격 AI 클라이언트를 기본 사용하는지 테스트를 추가한다.
- [x] prod와 동일한 방식의 `Deploy Dev EC2` workflow를 추가한다.
- [x] `application-dev.yml`과 README dev 배포 안내를 추가한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.
- [x] dev 배포 SSM path를 `/saynow/develop` 기준으로 수정한다.
- [x] 관련 workflow 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.
- [x] GitHub Actions 배포 environment를 `develop` 기준으로 수정한다.
- [x] dev 프로필 Swagger server URL을 `https://dev-api.p-e.kr`로 설정한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.
- [x] dev 프로필 Swagger server URL을 `https://dev-saynow.p-e.kr`로 변경한다.
- [x] SSM override 여부를 확인하고 필요하면 배포 결과를 검증한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.
- [x] Swagger UI Bearer token 입력을 위한 OpenAPI security scheme 테스트를 추가한다.
- [x] OpenAPI `bearerAuth` security scheme을 등록한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.
- [x] dev 환경에서 nonce 검증을 끄는 설정 테스트를 추가한다.
- [x] 기본 환경에서는 nonce 검증이 유지되는 테스트를 추가한다.
- [x] nonce 필드를 선택값으로 바꾸고 설정 기반 검증으로 변경한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.

---

# PR 4 main 병합 충돌 해결 체크리스트

- [x] `feat/3` worktree와 PR #4 충돌 상태를 확인한다.
- [x] 로컬 `feat/3`을 원격 PR head로 fast-forward한다.
- [x] 최신 `origin/main`을 병합해 충돌 파일을 식별한다.
- [x] `OpenApiIntegrationTest` 충돌을 소셜 인증 검증과 시나리오 상세 API 제거 검증 모두 보존하는 방식으로 해결한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 충돌 해결 커밋 전 변경 범위를 점검한다.

---

# AI 서버 연동 체크리스트

- [x] `origin/main` 기준 작업 브랜치 생성.
- [x] AI 서버 Swagger/OpenAPI 확인.
- [x] `/api/v1/turn-evaluations` 원격 클라이언트 계획 작성.
- [x] 원격 클라이언트 계약 테스트를 먼저 작성하고 실패 확인.
- [x] `AiPracticeClient` 인터페이스 추가.
- [x] 기본 로컬 AI 클라이언트와 선택형 원격 AI 클라이언트 분리.
- [x] `SAYNOW_AI_CLIENT_MODE=remote` 설정 추가.
- [x] 원격 AI 응답을 기존 `AiTurnEvaluationResult`로 매핑.
- [x] 원격 클라이언트 단위 테스트 통과.
- [x] 기존 연습 세션 API 통합 테스트 통과.
- [x] 전체 테스트 통과.
- [x] AI 서버 `/health` 확인.
- [x] AI 서버 `/api/v1/turn-evaluations` 직접 호출로 payload 필수 구조 확인.
- [x] AI 서버 `/stt` 직접 호출로 현재 STT 서버 오류 확인.
- [x] AWS `prod-saynow` 프로필로 백엔드/AI EC2 식별.
- [x] AI EC2 내부에서 `/tts`로 생성한 MP3를 `/stt`에 넣어 동일 오류 재현.
- [x] AI 서버 `stt_service.py`의 실제 예외 라인 확인.
- [x] 운영 백엔드 JAR에 아직 원격 AI 클라이언트 클래스가 배포되지 않았음을 확인.
- [x] 운영 프로파일 기본 AI 클라이언트 모드를 `remote`로 변경.
- [x] 운영 프로파일에서 `RemoteAiPracticeClient`가 선택되는지 테스트.
- [x] AI 서버 `/api/v1/session-feedbacks` 직접 호출로 최종 피드백 API 정상 응답 확인.
- [x] 최종 피드백 원격 클라이언트 테스트를 먼저 작성하고 실패 확인.
- [x] 최종 피드백 원격 클라이언트 구현.
- [x] 세션 종료 시 AI 서버 최종 피드백 결과를 저장하도록 연결.
- [x] 실제 AI 서버 최종 피드백 smoke test 추가 및 실행.
- [x] 최종 피드백 요청 payload를 문서 계약 기준으로 확장.
- [x] Spring component 생성자 주입을 Lombok `@RequiredArgsConstructor`로 전환.
- [x] 도메인/예외 클래스의 단순 getter/no-args boilerplate를 안전한 Lombok 어노테이션으로 전환.
- [x] Lombok 리팩터링 후 전체 테스트 실행.
- [x] 실제 백엔드 API smoke test로 AI 서버까지 연결되는지 확인.
- [ ] 로컬 PostgreSQL 실행 후 백엔드 원격 AI 모드 기동 확인.
- [x] diff 최종 점검 후 커밋.

---

# 턴 제출 Swagger multipart 계약 수정 체크리스트

- [x] `main` 기준 Swagger UI의 `audio` + `request` multipart 계약과 실제 컨트롤러 바인딩 차이 확인.
- [x] `request` JSON part로 턴 제출 시 실패하는 통합 테스트 추가.
- [x] 컨트롤러가 `request` JSON part를 바인딩하도록 수정.
- [x] 기존 턴 제출 통합 테스트와 smoke test 요청 형식 갱신.
- [x] OpenAPI 문서와 관련 테스트 확인.
- [x] 관련 테스트 통과.
- [x] 전체 테스트 통과.
- [x] diff 최종 점검 후 커밋.

---

# mp3/m4a 업로드 MIME 허용 체크리스트

- [x] `main` 기준 현재 오디오 타입 검증이 MIME 기준임을 확인.
- [x] `audio/mp3` 업로드 실패를 재현하는 통합 테스트 추가.
- [x] `audio/m4a` 업로드 실패를 재현하는 통합 테스트 추가.
- [x] mp3/m4a MIME alias를 허용하도록 수정.
- [x] 관련 테스트 통과.
- [x] 전체 테스트 통과.
- [x] diff 최종 점검 후 커밋.

---

# WebM 업로드 MIME 허용 체크리스트

- [x] `main` 기준 현재 WebM 검증이 `audio/webm` 정확 일치임을 확인.
- [x] `video/webm` 업로드 실패를 재현하는 통합 테스트 추가.
- [x] `audio/webm;codecs=opus` 업로드 실패를 재현하는 통합 테스트 추가.
- [x] MIME 파라미터 정규화와 WebM alias 허용 처리.
- [x] 관련 테스트 통과.
- [x] 전체 테스트 통과.
- [x] diff 최종 점검 후 커밋.

---

# 오디오 Content-Type 검증 제거 체크리스트

- [x] 첨부 `output.mp3` 파일의 로컬 MIME 확인.
- [x] 배포 서버에 첨부 `output.mp3` 실제 업로드로 415 재현.
- [x] multipart trace로 `audio` part가 `application/octet-stream`으로 전송됨을 확인.
- [x] 사용자 요청에 따라 MIME/확장자 검증 보정 방향 폐기.
- [x] `audio.contentType()` 기반 `UNSUPPORTED_AUDIO_TYPE` 검증 제거.
- [x] `application/octet-stream` mp3 업로드와 임의 Content-Type 허용 테스트로 수정.
- [x] 관련 테스트 통과.
- [x] 전체 테스트 통과.
- [x] diff 최종 점검 후 커밋.

---

# 턴 request part Content-Type 허용 체크리스트

- [x] 운영 서버에서 `request` part에 `type=application/json`이 없으면 415가 재현됨을 확인.
- [x] `request` part가 text/plain이어도 처리되는 통합 테스트 추가.
- [x] 컨트롤러가 `request` part 문자열 JSON을 파싱하도록 수정.
- [x] 잘못된 request JSON은 400 검증 실패로 반환.
- [x] 관련 테스트 통과.
- [x] 전체 테스트 통과.
- [x] diff 최종 점검 후 커밋.

---

# 시나리오 목록 설명 반환 체크리스트

- [x] 목록 API 응답에서 `situationDescription` 기대값을 검증하는 테스트를 먼저 추가한다.
- [x] 실패한 테스트를 확인한다.
- [x] `ScenarioSummaryResponse`와 서비스 매핑에 `situationDescription`을 추가한다.
- [x] 관련 통합 테스트를 통과시킨다.
- [x] 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.

---

# 시나리오 상세 조회 API 제거 체크리스트

- [x] OpenAPI에서 시나리오 상세 조회 경로가 제거되는 기대값을 먼저 추가한다.
- [x] 실패한 테스트를 확인한다.
- [x] `ScenarioController`에서 상세 조회 엔드포인트를 제거한다.
- [x] `ScenarioService`에서 상세 조회 메서드와 미사용 DTO를 제거한다.
- [x] 관련 통합 테스트를 통과시킨다.
- [x] 전체 테스트를 실행한다.
- [x] 변경 범위를 점검하고 커밋한다.

---

# PR 7 main 병합 충돌 해결 체크리스트

- [x] PR 충돌 상태와 최신 `origin/main`을 확인한다.
- [x] `origin/main`을 `feat/6`에 병합해 충돌 파일을 식별한다.
- [x] `checklist.md` 충돌을 양쪽 작업 기록 보존 방식으로 해결한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 충돌 해결 커밋 전 변경 범위를 점검한다.

---

# Sentry 연결 체크리스트

- [x] Sentry 연결 계획 문서 작성.
- [x] Sentry 5xx 예외 캡처 테스트 작성 및 실패 확인.
- [x] Sentry 의존성과 예외 캡처 구현.
- [x] Logback Sentry Appender 설정 추가.
- [x] 운영 환경 SSM 동기화에 Sentry 환경 변수 추가.
- [x] README와 `.env.example`에 Sentry 설정 반영.
- [x] 관련 단위 테스트 통과.
- [x] 전체 테스트 통과.
- [x] diff 최종 점검 후 커밋.

---

# 이슈 템플릿 강제 체크리스트

- [x] 현재 GitHub 이슈 템플릿 구조 확인.
- [x] legacy Markdown 이슈 템플릿을 Issue Forms로 전환.
- [x] blank issue 생성을 비활성화.
- [x] Sentry 4xx 이벤트 전송 여부를 현재 리졸버 순서로 검증.
- [x] 템플릿 YAML 문법과 관련 테스트 실행.
- [x] 변경 범위 점검 후 커밋.

---

# Sentry 4xx 전송 체크리스트

- [x] 현재 4xx 응답 경로를 확인한다.
- [x] 4xx `ApiException` 전송 기대 테스트를 먼저 추가하고 실패를 확인한다.
- [x] validation 계열 4xx 전송 기대 테스트를 먼저 추가하고 실패를 확인한다.
- [x] Spring Security 401 응답 전송 기대 테스트를 먼저 추가하고 실패를 확인한다.
- [x] 전역 예외 핸들러와 인증 실패 writer에서 4xx를 Sentry로 전송한다.
- [x] Wiki의 Sentry 수집 기준을 4xx 포함으로 수정한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 커밋하고 PR 브랜치를 갱신한다.

---

# 2차 MVP 도메인 명칭 정리 체크리스트

- [x] `Member`, `Practice`, `ScenarioCategory` 계열 잔여 명칭을 `src/main`, `src/test`에서 검색한다.
- [x] 실제 테이블 개념에 맞춰 `User`, `Session`, `Category` 계열 클래스와 패키지명으로 정리한다.
- [x] 인증 응답 필드도 `newMember`에서 `newUser`로 정리한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 커밋한다.

---

# 발화 하트 차감 정책 체크리스트

- [x] 슬롯이 채워진 발화에서는 하트가 유지되는 기대 테스트를 추가한다.
- [x] AI 응답의 `filledSlots`가 빈 배열이면 하트가 1 감소하는 기대 테스트를 추가한다.
- [x] 기존 구현에서 슬롯이 채워진 발화도 하트가 감소해 테스트가 실패하는 것을 확인한다.
- [x] `filledSlots`가 비었을 때만 하트를 차감하도록 세션 발화 처리 로직을 수정한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 커밋한다.

---

# 사용자 발화 제출 API 명세 반영 체크리스트

- [x] 명세와 현재 구현의 요청, 응답, 오류 코드를 대조한다.
- [x] 빈 `userUtterance`, 완료 세션 재요청, 다른 사용자 세션 접근 오류 기대 테스트를 추가한다.
- [x] OpenAPI 문서 예시의 발화 제출 오류 코드를 새 명세 기준으로 수정하는 테스트를 추가한다.
- [x] 기존 구현에서 `VALIDATION_FAILED`, `SESSION_ALREADY_ENDED`, `SESSION_ACCESS_DENIED`가 반환되어 테스트가 실패하는 것을 확인한다.
- [x] 발화 제출 API가 `INVALID_REQUEST`, `SESSION_ALREADY_COMPLETED`, `FORBIDDEN`을 반환하도록 수정한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 커밋한다.

---

# 발화 성공 시 시나리오 클리어 저장 체크리스트

- [x] 발화 제출 성공 직후 피드백 생성 전에도 시나리오가 클리어되고 다음 시나리오가 해금되는 기대 테스트를 추가한다.
- [x] 기존 구현에서 피드백 생성 전 홈 목록의 `cleared`가 false라 테스트가 실패하는 것을 확인한다.
- [x] 세션 성공 처리 시점에 `UserScenarioProgress.markCleared()`를 호출하도록 수정한다.
- [x] 피드백 생성 서비스의 중복 클리어 저장 로직을 제거한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 커밋한다.

---

# MVP2 프론트 CORS origin 교체 체크리스트

- [x] 현재 CORS 기본값과 develop 런타임 SSM override 여부를 확인한다.
- [x] `application.yml`, `.env.example`, README의 CORS origin 목록을 요청 목록으로 교체한다.
- [x] CORS preflight 통합 테스트의 허용 origin 기대값을 요청 목록으로 수정한다.
- [x] develop SSM `SAYNOW_CORS_ALLOWED_ORIGINS`를 요청 목록으로 설정한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 커밋하고 push한다.
- [x] dev 배포 후 실제 preflight 응답을 확인한다.

---

# 시나리오 상황 필드 응답 추가 체크리스트

- [x] `origin/develop` 기준으로 `feat/15` 브랜치를 생성한다.
- [x] 현재 시나리오 목록 API, DTO, DB 스키마, OpenAPI 예시를 확인한다.
- [x] `GET /api/v1/scenarios` 응답의 `scenarioSituation` 기대 테스트를 먼저 추가하고 실패를 확인한다.
- [x] `scenarios` 테이블, `Scenario` 엔티티, 목록 응답 DTO와 서비스 매핑에 상황 필드를 추가한다.
- [x] OpenAPI 성공 예시에 `scenarioSituation`을 추가한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 의미 단위 커밋을 만든다.

---

# 공항 시나리오 seed 추가 체크리스트

- [x] 현재 Airport 카테고리와 시나리오 seed 구조를 확인한다.
- [x] 공항 시나리오 3개와 슬롯 seed 기대 테스트를 먼저 추가하고 실패를 확인한다.
- [x] 새 Flyway migration으로 Airport 시나리오 3개를 추가한다.
- [x] 각 공항 시나리오의 슬롯 seed를 추가한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 의미 단위 커밋을 만든다.

---

# 기본 카테고리 Airport 전환 체크리스트

- [x] 현재 Cafe 전용 잠금 정책과 기존 카테고리 seed를 확인한다.
- [x] Airport가 열린 카테고리라는 기대 테스트를 먼저 추가하고 실패를 확인한다.
- [x] 시나리오 목록 잠금 정책의 열린 카테고리를 Airport로 변경한다.
- [x] 세션 시작 허용 정책의 열린 카테고리를 Airport로 변경한다.
- [x] OpenAPI 목록 예시를 Airport 기본 카테고리 기준으로 수정한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 의미 단위 커밋을 만든다.

---

# 카테고리 seed 복구 체크리스트

- [x] 카테고리가 초기화된 DB에서도 V3 migration이 동작해야 하는 조건을 확인한다.
- [x] V3 migration이 카테고리를 시나리오보다 먼저 복구한다는 테스트를 추가하고 실패를 확인한다.
- [x] V3 migration에 기본 카테고리 4개 복구 SQL을 추가한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 의미 단위 커밋을 만든다.

---

# 세션 중 영어 학습 가이드 모드 체크리스트

- [x] `origin/develop` 기준으로 `feat/16` 브랜치를 생성한다.
- [x] 기존 세션, AI 클라이언트, 피드백 생성 흐름을 확인한다.
- [x] 구현 계획 문서를 작성한다.
- [x] 가이드 API 통합 테스트를 먼저 추가하고 실패를 확인한다.
- [x] 원격 AI 가이드 요청/응답 계약 테스트를 먼저 추가하고 실패를 확인한다.
- [x] OpenAPI 문서 테스트를 먼저 추가하고 실패를 확인한다.
- [x] 세션 소유권과 진행 상태를 검증하는 가이드 API를 추가한다.
- [x] 백엔드에서 명백한 남용 요청을 AI 서버 호출 전에 차단한다.
- [x] 가이드 질문과 답변이 저장되지 않고 최종 피드백에 포함되지 않도록 유지한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [x] 변경 범위 점검 후 의미 단위 커밋을 만든다.

---

# 세션 NPS 평가 수집 API 체크리스트

- [x] 최신 `origin/develop` 위로 로컬 develop을 재배치하고 기존 작업 기록을 보존한다.
- [x] 현재 세션, 피드백, OpenAPI, Flyway 구조를 확인한다.
- [x] 구현 계획과 저장 정책을 기록한다.
- [x] NPS API 통합 테스트를 먼저 작성하고 실패를 확인한다.
- [x] NPS 테이블 스키마 테스트를 먼저 작성하고 실패를 확인한다.
- [x] OpenAPI 문서 테스트를 먼저 작성하고 실패를 확인한다.
- [x] `session_nps_responses` Flyway migration을 추가한다.
- [x] NPS 엔티티, 저장소, 서비스, 컨트롤러를 추가한다.
- [x] 오류 코드와 OpenAPI 예시를 추가한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [ ] 변경 범위 점검 후 커밋한다.
- [ ] develop으로 push한다.
- [ ] dev 배포 workflow를 실행하고 health를 확인한다.
