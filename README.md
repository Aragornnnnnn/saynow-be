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
export DB_URL="jdbc:postgresql://<supabase-host>:5432/postgres"
export DB_USERNAME="<supabase-user>"
export DB_PASSWORD="<supabase-password>"
```

Run the application:

```bash
./gradlew bootRun
```

Run tests:

```bash
./gradlew test
```
