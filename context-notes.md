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
