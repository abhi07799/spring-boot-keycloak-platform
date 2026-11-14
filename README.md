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

## Backend

The backend is a Maven-based Spring MVC application secured as a stateless OAuth 2.0 JWT resource server.

### Prerequisites
- JDK 21
- Apache Maven 3.6.3+
- Docker Engine or Docker Desktop with Compose v2

### Run locally
1. Copy `.env.example` to `.env` and replace the placeholder values.
2. Start infrastructure: `docker compose up -d app-db keycloak`.
3. Export `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` in your shell (see [database setup](docs/database/README.md)).
4. Set `KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/platform` (see [security setup](docs/security/README.md)).
5. Run `mvn spring-boot:run`.

- Ping endpoint: http://localhost:8080/api/v1/ping (public)
- Swagger UI: http://localhost:8080/swagger-ui.html (Keycloak OAuth2 + PKCE)
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Actuator health: http://localhost:8080/actuator/health

In Swagger UI, select **Authorize**, sign in through the `platform` Keycloak realm, and authorize using the `platform-swagger` public client. The access token is attached to secured API calls. The Keycloak redirect URI and browser origin must match the address used to open Swagger UI.

Flyway applies versioned migrations at startup. Hibernate validates the resulting schema. The backend connects to the application database only; Keycloak retains its independent database.

## Architecture decisions
- Modular monolith; no frontend application.
- Separate PostgreSQL services and persistent volumes for application data and Keycloak.
- Keycloak owns credentials and authentication; the application database owns application-specific profile and business data plus the Keycloak subject identifier.
- Keycloak client roles: `ADMIN`, `STAFF`, and `USER` under the `platform-api` client.
- Public registration assigns only `USER`.
- An authenticated `ADMIN` can create `STAFF` or `ADMIN`; the first administrator is provisioned through a one-time, non-public bootstrap process.
- Swagger UI uses a public OAuth client with PKCE. Administrative Keycloak operations use a separate confidential service account with least-privilege permissions.
- The API validates JWT signature, issuer, timestamps, and the `platform-api` audience. Keycloak client roles are mapped to Spring Security role authorities.

## Keycloak clients
- `platform-api`: bearer-only API audience and role owner.
- `platform-swagger`: public Swagger UI client using Authorization Code + PKCE S256.
- `platform-admin-service`: confidential service-account client for backend Admin REST API calls.
- Full Scope Allowed is disabled; Swagger receives only explicitly mapped API client roles.

See [Keycloak configuration](docs/keycloak/README.md), [security setup](docs/security/README.md), [local infrastructure](docs/docker/README.md), [database setup](docs/database/README.md), and [backend package structure](docs/architecture/package-structure.md).

## Branch workflow
- `main`: stable baseline.
- `develop`: integration branch.
- `step1/initial-project-setup`: initial repository baseline.
- Later milestones use numbered prefixes such as `step2.1/`, `step2.2/`, `step2.3/`, `step2.4/`, `step3.1/`, and `step3.2/`.

## Learning journal
See [LEARNING.md](LEARNING.md).

## Security
Never commit passwords, tokens, private keys, or real environment files. Use `.env.example` only for placeholders. No password or password hash is stored in the application profile database.
