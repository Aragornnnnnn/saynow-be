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

The local profile uses a local PostgreSQL database. Copy the example env file:

```bash
cp .env.example .env
```

Then load it before running the app:

```bash
set -a
source .env
set +a
```

`.env` is ignored by Git. Commit only `.env.example`.

## Production Configuration

Production uses Supabase Session pooler values through deployment environment variables:

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<session-pooler-host>:5432/postgres
DB_USERNAME=postgres.<project-ref>
DB_PASSWORD=<database-password>
```

Do not commit production env files or credentials.

Run the application:

```bash
./gradlew bootRun
```

Run tests:

```bash
./gradlew test
```
