# Medy

Medical ERP & CRM for clinics — appointments, patient management, treatment plans, billing, and more, built as a modular Spring Boot backend.

See [ARCHITECTURE.md](./ARCHITECTURE.md) for the full design and roadmap.

## Stack

- Java 26, Spring Boot 4
- Spring Modulith (modular monolith)
- PostgreSQL 16 + Flyway
- React / Next.js frontend (planned, not started yet)

## Prerequisites

- JDK 26
- Docker Desktop (running, for Postgres)

## Running locally

```bash
./mvnw spring-boot:run
```

This starts Postgres automatically via `compose.yaml` (Spring Boot's Docker Compose support), applies Flyway migrations, and starts the app on `http://localhost:8080`.

## Database access

- Host: `localhost`, Port: `5432`, DB: `medy`, User: `medy`, Password: `secret`
- Connect via IntelliJ's built-in Database tool, or `docker exec -it medy-postgres-1 psql -U medy -d medy`
- To run Postgres standalone (without the app): `docker compose up -d`
- If the app/tests fail with `role "medy" does not exist`, something else (e.g. Postgres.app) is already bound to port 5432 and is intercepting the connection instead of our container — quit it and retry.
