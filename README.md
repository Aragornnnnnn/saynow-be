# saynow-be

Saynow 백엔드 서비스입니다.

## 기술 스택

- Java 21
- Spring Boot 4.0.6
- Gradle Kotlin DSL
- Spring MVC
- Spring Data JPA
- PostgreSQL
- Flyway

## 로컬 환경 설정

로컬 프로필은 [application-local.yml](src/main/resources/application-local.yml)에 정의된 로컬 PostgreSQL 데이터베이스를 사용합니다.

```text
jdbc:postgresql://localhost:5432/postgres
username: postgres
password: postgres
```

## 운영 환경 설정

운영 프로필은 배포 환경 변수로 전달되는 Supabase Session Pooler 연결 정보를 사용합니다.

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<session-pooler-host>:5432/postgres
DB_USERNAME=postgres.<project-ref>
DB_PASSWORD=<database-password>
SAYNOW_AUTH_TOKEN_SECRET=<long-random-hmac-secret>
SAYNOW_AUTH_OIDC_GOOGLE_AUDIENCES=<google-web-client-id>
SENTRY_ENABLED=true
SENTRY_DSN=https://<public-key>@<org>.ingest.sentry.io/<project-id>
SENTRY_ENVIRONMENT=prod
SENTRY_LOGS_ENABLED=true
SENTRY_RELEASE=saynow-be
```

배포 테스트용 환경 변수 파일이 필요하면 [.env.example](.env.example)을 템플릿으로 사용합니다. 운영 환경 변수 파일이나 인증 정보는 커밋하지 않습니다.

Google Web OAuth Client ID를 사용하는 경우 `SAYNOW_AUTH_OIDC_GOOGLE_AUDIENCES`에 Client ID를 설정합니다. 여러 플랫폼의 Client ID를 허용해야 하면 comma-separated 형식으로 입력합니다.

```bash
SAYNOW_AUTH_OIDC_GOOGLE_AUDIENCES=1062331189445-ov26of8u6pb8iauq0c4n68ni1flipm1q.apps.googleusercontent.com
```

Kakao OIDC를 활성화하면 `SAYNOW_AUTH_OIDC_KAKAO_AUDIENCES`에 Kakao 앱 키를 설정합니다.

## Sentry 운영 로그

운영 프로필은 `SENTRY_DSN`이 설정된 경우 Sentry로 서버 오류와 사용자 오류를 전송합니다. `GlobalExceptionHandler`에서 처리되는 4xx/5xx 예외와 Spring Security 인증 실패 401/403 응답은 명시적으로 캡처합니다.

Logback Sentry Appender는 운영 프로필에서만 붙습니다. `ERROR` 이상 로그는 Sentry 이벤트로 전송하고, `INFO` 이상 로그는 에러 이벤트의 breadcrumb와 Sentry Logs 조회 대상으로 사용합니다.

음성 파일 본문과 요청 body, 기본 PII는 보내지 않습니다. 로그 전체 수집을 끄려면 운영 환경 변수 `SENTRY_LOGS_ENABLED=false`를 사용합니다.

애플리케이션 실행:

```bash
./gradlew bootRun
```

테스트 실행:

```bash
./gradlew test
```

## 운영 배포

GitHub Actions의 `Deploy Prod EC2` workflow를 수동 실행해 선택한 브랜치를 운영 EC2에 배포합니다.

GitHub `prod` Environment에는 아래 값을 설정합니다.

- Secret: `EC2_SSH_KEY`
- Variable: `EC2_HOST`
- Variable: `EC2_USER`

애플리케이션 런타임 환경변수는 EC2 IAM Role로 AWS SSM Parameter Store의 `/saynow/prod` 경로에서 읽어 `/opt/saynow/.env`로 생성합니다.

필수 SSM 파라미터입니다.

- `/saynow/prod/DB_URL`
- `/saynow/prod/DB_USERNAME`
- `/saynow/prod/DB_PASSWORD`
- `/saynow/prod/SAYNOW_AUTH_TOKEN_SECRET`
- `/saynow/prod/SAYNOW_AUTH_OIDC_GOOGLE_AUDIENCES`

선택 SSM 파라미터:

- `/saynow/prod/SAYNOW_AUTH_OIDC_KAKAO_AUDIENCES`

선택 Sentry SSM 파라미터입니다.

- `/saynow/prod/SENTRY_ENABLED`
- `/saynow/prod/SENTRY_DSN`
- `/saynow/prod/SENTRY_ENVIRONMENT`
- `/saynow/prod/SENTRY_LOGS_ENABLED`
- `/saynow/prod/SENTRY_RELEASE`
