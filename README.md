# saynow-be

Saynow backend service built with Java 21 and Spring Boot 4.

## Stack

- Java 21
- Spring Boot 4.0.6
- Gradle Kotlin DSL
- Spring MVC
- Spring Data JPA
- PostgreSQL
- Flyway

## Local Configuration

The local profile reads database settings from environment variables.

```bash
cp .env.example .env
```

Fill `.env` with your Supabase Session pooler values. Then load it before running the app:

```bash
set -a
source .env
set +a
```

`.env` is ignored by Git. Commit only `.env.example`.

Run the application:

```bash
./gradlew bootRun
```

Run tests:

```bash
./gradlew test
```
