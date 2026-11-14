# Spring Boot Keycloak Platform

A learning-oriented backend platform built with Java 21, Spring Boot 4.1.2, Keycloak 26.8.2, and PostgreSQL 16.

## Goals
- Learn production-oriented authentication and authorization with Keycloak and Spring Security.
- Keep application data and identity data in separate PostgreSQL databases.
- Authenticate through Swagger UI using OAuth 2.0 Authorization Code with PKCE.
- Build incrementally with focused branches, tests, and a learning journal.

## Approved baseline
| Component | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 |
| Keycloak | 26.8.2 |
| PostgreSQL | 16 |
| Springdoc OpenAPI | 3.2.0 |
| Migrations | Flyway |

## Backend skeleton

The backend is a Maven-based Spring MVC application.

### Prerequisites
- JDK 21
- Apache Maven 3.6.3+

### Run locally
```bash
mvn spring-boot:run
```

- Ping endpoint: http://localhost:8080/api/v1/ping
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Actuator health: http://localhost:8080/actuator/health

The current skeleton does not connect to PostgreSQL or Keycloak yet. Persistence, Flyway migrations, and OAuth2 resource-server security are added in subsequent milestones.

## Architecture decisions
- Modular monolith; no frontend application.
- Separate PostgreSQL services and persistent volumes for application data and Keycloak.
- Keycloak owns credentials and authentication; the application database owns profile and business data plus the Keycloak subject identifier.
- Keycloak client roles: `ADMIN`, `STAFF`, and `USER` under the `platform-api` client.
- Public registration assigns only `USER`.
- An authenticated `ADMIN` can create `STAFF` or `ADMIN`; the first administrator is provisioned through a one-time, non-public bootstrap process.
- Swagger UI uses a public OAuth client with PKCE. Administrative Keycloak operations use a separate confidential service account with least-privilege permissions.

## Keycloak clients
- `platform-api`: bearer-only API audience and role owner.
- `platform-swagger`: public Swagger UI client using Authorization Code + PKCE S256.
- `platform-admin-service`: confidential service-account client for backend Admin REST API calls.
- Full Scope Allowed is disabled; Swagger receives only explicitly mapped API client roles.

See [Keycloak configuration](docs/keycloak/README.md), [local infrastructure](docs/docker/README.md), and [backend package structure](docs/architecture/package-structure.md).

## Branch workflow
- `main`: stable baseline.
- `develop`: integration branch.
- `step1/initial-project-setup`: initial repository baseline.
- Later milestones use numbered prefixes such as `step2.1/`, `step2.2/`, and `step2.3/`.

## Learning journal
See [LEARNING.md](LEARNING.md).

## Security
Never commit passwords, tokens, private keys, or real environment files. Use `.env.example` only for placeholders.
