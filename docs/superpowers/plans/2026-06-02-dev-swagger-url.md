# Develop Swagger 서버 URL 분리 계획

## 목표

develop 배포 Swagger/OpenAPI 문서가 운영 서버 `https://saynow.p-e.kr`를 가리키지 않게 한다.

## 원인

코드에는 dev 프로필 기본값 `https://dev-saynow.p-e.kr`가 있지만, dev 배포 workflow가 `/saynow/develop/SAYNOW_OPENAPI_SERVER_URL` SSM 값을 그대로 `.env`에 쓸 수 있다. SSM 값이 prod 주소로 오염되면 dev 기본값보다 환경변수가 우선되어 Swagger가 운영 주소를 노출한다.

## 구현

1. dev 프로필에 prod Swagger URL이 주입된 상황을 테스트로 재현한다.
2. OpenAPI 서버 URL 결정 로직에서 dev 프로필은 prod URL을 dev URL로 보정한다.
3. README에는 develop SSM의 Swagger URL override를 권장하지 않고 dev 프로필 보정 정책을 정리한다.

## 검증

- `./gradlew test --tests com.saynow.DevOpenApiIntegrationTest`.
- `./gradlew test --tests com.saynow.OpenApiIntegrationTest`.
- `./gradlew test`.
- `git diff --check`.
