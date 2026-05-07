# Saynow Backend Initial Spring Boot Setup Design

## Context

`saynow-be` is an empty Git repository intended to become a Java/Spring backend service. The first milestone is to create only the initial project structure and configuration, then push it to GitHub.

## Decisions

- Language/runtime: Java 21 LTS
- Framework: Spring Boot 4.0.x
- Build tool: Gradle Kotlin DSL
- Base package: `com.saynow`
- Database: Supabase PostgreSQL
- Database access: Spring Data JPA
- Schema migration: Flyway

## Project Structure

The project will start as a single Spring Boot application, not a multi-module build. The initial package structure will stay minimal:

- `com.saynow`: application entry point
- `src/main/resources`: application configuration
- `src/main/resources/db/migration`: Flyway migrations
- `src/test/java`: application context test

This keeps the initial setup small while leaving room to introduce feature packages later when real domains are defined.

## Dependencies

The initial dependencies will be limited to setup-level concerns:

- Spring Web
- Spring Validation
- Spring Actuator
- Spring Data JPA
- PostgreSQL Driver
- Flyway
- Lombok
- Spring Boot Test

No domain-specific dependencies will be added during the initial setup.

## Configuration

The application will use a `local` profile for local development. Supabase connection values will be read from environment variables so secrets are not committed:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

The committed configuration will include placeholders and safe defaults only. Runtime secrets must be supplied outside Git.

## Database Migration

Flyway will manage schema history from the start. The first migration will be `V1__init.sql`.

Because no business domain has been defined yet, the first migration should remain intentionally minimal. It can be empty or contain only harmless setup SQL if needed for Flyway validation. Domain tables will be added later through dedicated migrations.

## Verification

The initial setup is considered complete when:

- Gradle wrapper is present.
- The project builds with Java 21.
- The Spring application context test passes.
- Configuration does not commit real Supabase credentials.
- Flyway migration location is present and recognized.
- Git status is reviewed before commit.

## Out of Scope

- Authentication and authorization
- Domain modeling
- API endpoint implementation
- Supabase Row Level Security policy design
- CI/CD setup
- Docker or deployment configuration
