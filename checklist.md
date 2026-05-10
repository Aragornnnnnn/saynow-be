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
