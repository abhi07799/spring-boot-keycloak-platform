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
5. After the first realm import, copy the generated platform-admin-service client secret into your ignored .env file and export KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET in the shell that starts Spring Boot (see [Keycloak setup](docs/keycloak/README.md)).
6. Run `mvn spring-boot:run`.

The backend uses KEYCLOAK_ADMIN_BASE_URL=http://localhost:8081 and realm platform by default. If running the backend inside Compose, use http://keycloak:8080 for the admin base URL and ensure the configured issuer/token endpoint is reachable from that container.

- Registration endpoint: `POST http://localhost:8080/api/v1/auth/register` (public; assigns only `USER` and sends a Keycloak email-verification link).
- Password reset request: `POST http://localhost:8080/api/v1/auth/password/forgot` (public; generic 202 response; Keycloak emails a one-time password-update link).
- Resend verification email: `POST http://localhost:8080/api/v1/auth/email/verification/resend` (public; generic 202 response).
- Current-user logout: `POST http://localhost:8080/api/v1/auth/logout` (authenticated; terminates all Keycloak sessions for the JWT subject).
- Admin force logout: `POST http://localhost:8080/api/v1/admin/users/{userId}/logout` (ADMIN only; terminates all sessions for the target user).
- Current user profile: `GET http://localhost:8080/api/v1/users/me` (authenticated; resolves the profile from the JWT subject).
- Update current user profile: `PUT http://localhost:8080/api/v1/users/me` (authenticated; updates full name and mobile number only).
- Admin user directory: `GET http://localhost:8080/api/v1/admin/users` (ADMIN only; paginated, default 20 users, maximum 100 per page).
- Admin account status: `PATCH http://localhost:8080/api/v1/admin/users/{userId}/status` (ADMIN only; body `{"enabled":false}` disables, `{"enabled":true}` re-enables).
- Admin role management: `PATCH http://localhost:8080/api/v1/admin/users/{userId}/role` (ADMIN only; body `{"role":"STAFF"}`; allowed values: `USER`, `STAFF`, `ADMIN`).
- Admin provisioning: `POST http://localhost:8080/api/v1/admin/users` (ADMIN only; creates `STAFF` or `ADMIN`).
- Admin audit history: `GET http://localhost:8080/api/v1/admin/audit-events` (ADMIN only; paginated, default 20 events, maximum 100 per page).
- Ping endpoint: http://localhost:8080/api/v1/ping (public)
- Swagger UI: http://localhost:8080/swagger-ui.html (Keycloak OAuth2 + PKCE)
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Actuator health: http://localhost:8080/actuator/health

The imported Keycloak realm requires verified email addresses for login and enables its built-in password-reset flow. Configure SMTP in Keycloak before testing registration, verification, or password reset. Password reset and verification action links expire after 15 minutes by default.

Administrative provisioning, account status changes, role changes, and session revocations are recorded in the application database audit log. Successful logout also advances the profile authorization timestamp, so the resource server rejects previously issued access tokens on their next request. Logout is user-wide; clients must discard their locally held tokens after receiving the successful response. The last enabled ADMIN cannot be demoted or disabled, and ADMIN provisioning is serialized with role and status changes. Events record the acting JWT subject, target application profile ID, action, event time, and assigned role, before/after enabled state, or before/after application role. Passwords, tokens, and profile PII are not stored in audit events.

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
- The API validates JWT signature, issuer, timestamps, and the `platform-api` audience. Every bearer-token authentication also checks the JWT subject against the application profile's enabled status in PostgreSQL; disabled or missing profiles are rejected. Keycloak client roles are mapped to Spring Security role authorities.

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
- Later milestones use numbered prefixes such as `step2.1/`, `step2.2/`, `step2.3/`, `step2.4/`, `step3.1/`, `step3.2/`, `step3.3/`, `step3.4/`, `step3.5/`, `step3.6/`, `step3.7/`, `step3.8/`, `step3.9/`, and `step3.10/`.

## Learning journal
See [LEARNING.md](LEARNING.md).

## Security
Never commit passwords, tokens, private keys, or real environment files. Use `.env.example` only for placeholders. No password or password hash is stored in the application profile database.
