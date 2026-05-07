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
```

배포 테스트용 환경 변수 파일이 필요하면 [.env.example](.env.example)을 템플릿으로 사용합니다. 운영 환경 변수 파일이나 인증 정보는 커밋하지 않습니다.

애플리케이션 실행:

```bash
./gradlew bootRun
```

테스트 실행:

```bash
./gradlew test
```
