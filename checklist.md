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
