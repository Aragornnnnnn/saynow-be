# 소셜 로그인 OIDC 구현 계획

## 목표

Google, Kakao OIDC 기반 소셜 로그인 API를 추가하고, SayNow 자체 access token과 refresh token으로 API 인증을 처리한다. 연습 세션과 피드백은 로그인한 회원의 리소스로 귀속하며, 다른 회원의 세션 접근은 거부한다.

## 범위

1. 인증 도메인 추가.
   - `members`, `social_accounts`, `refresh_tokens` 테이블을 추가한다.
   - `practice_sessions.member_id`를 `NOT NULL` FK로 추가한다.
   - provider 프로필 이미지는 저장하지 않는다.

2. 토큰과 OIDC 검증.
   - provider ID Token 검증 인터페이스를 둔다.
   - 운영 구현은 OIDC issuer, audience, signature, expiry, nonce 검증을 담당한다.
   - 테스트에서는 fake verifier로 성공/실패 경로를 검증한다.
   - SayNow access token과 refresh token은 자체 발급하고 refresh token 원문은 저장하지 않는다.

3. API 추가.
   - `POST /api/v1/auth/social-login`.
   - `POST /api/v1/auth/token/refresh`.
   - `POST /api/v1/auth/logout`.

4. 권한 정책 변경.
   - 시나리오 탐색 API는 공개로 둔다.
   - 세션 시작, 세션 조회, 마이크 준비 기록, 턴 제출, 세션 종료, 피드백 조회는 로그인 필수로 바꾼다.

5. 세션 소유자 검증.
   - 세션 생성 시 현재 회원을 저장한다.
   - 세션 접근 시 `public_id`와 현재 회원을 함께 확인한다.
   - 다른 회원 세션 접근은 `SESSION_ACCESS_DENIED`로 응답한다.

## 검증 계획

1. auth API 통합 테스트를 먼저 작성하고 실패를 확인한다.
2. 인증 없이 세션 API 호출 시 401이 나는 테스트를 먼저 작성하고 실패를 확인한다.
3. 로그인 후 세션 생성 시 member_id가 저장되는 테스트를 먼저 작성하고 실패를 확인한다.
4. 다른 회원의 세션 접근이 403으로 거부되는 테스트를 먼저 작성하고 실패를 확인한다.
5. 최소 구현 후 관련 테스트와 `./gradlew test`를 실행한다.
