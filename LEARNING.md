# Learning Journal

This file records what we implement, why it matters, the key concepts, and the important files changed. It is intentionally concise and will be updated with every completed milestone.

## Step 1 — Initial repository setup

**What we implemented**
- Established the repository overview and approved technology baseline.
- Recorded the modular-monolith direction, database separation, identity ownership, and role model.
- Defined the numbered milestone branch convention.
- Added this learning journal as the running implementation record.

**Why**
A clear baseline keeps implementation decisions consistent and makes each milestone reviewable. Recording security decisions early reduces the chance of introducing unsafe defaults later.

**Important concepts**
- **Modular monolith:** one deployable application organized into well-separated business and technical modules.
- **Identity vs. application profile:** Keycloak owns credentials and authentication; the application database owns application-specific profile and business data.
- **Client role:** a role scoped to a particular Keycloak client.
- **PKCE:** Proof Key for Code Exchange. It protects an OAuth authorization-code exchange, including for public clients such as browser-based Swagger UI.

**How the planned flow works**
Swagger UI will authenticate a user with Keycloak using Authorization Code + PKCE. The backend will validate the access token and map Keycloak client roles to Spring Security authorities. Public registration will create only a USER account. Privileged account creation will require an ADMIN authorization path.

**Files changed**
- `README.md`
- `LEARNING.md`
- `.gitignore`
- `.editorconfig`
- `docs/architecture/decisions.md`

**Verification**
Repository metadata and write permissions were checked before the initial commit. Application build and runtime verification are not applicable yet because the application skeleton has not been introduced.


## Step 2.1 — Docker infrastructure

**What we implemented**
- Added Docker Compose services for two independent PostgreSQL 16 instances and Keycloak 26.8.2.
- Added separate named volumes, private bridge networks, loopback-only host port bindings, restart policies, and health checks.
- Added `.env.example` and local infrastructure instructions.
- Kept the Spring Boot service out of this milestone because the application skeleton is introduced separately.

**Why**
The application database and Keycloak database have different owners and security boundaries. Separate services, credentials, volumes, and networks reduce accidental coupling and make the ownership boundary explicit.

**Important concepts**
- **Compose service DNS:** containers on the same Compose network can reach each other by service name. Keycloak therefore connects to `keycloak-db:5432`, not `localhost`.
- **Named volume:** Docker-managed storage that survives ordinary container recreation and `docker compose down`.
- **Health check:** a command that reports whether a service is ready. Compose can wait for a dependency marked `service_healthy`.
- **Environment file:** `.env` supplies local configuration to Compose. Only the placeholder `.env.example` is committed.

**How it works**
Keycloak starts only after `keycloak-db` is healthy. The application database is isolated on `app-network`; the Keycloak database is isolated on `keycloak-network`. Keycloak joins both networks so the future backend can reach Keycloak and its own database without exposing the databases to each other.

**Files changed**
- `compose.yaml`
- `.env.example`
- `docs/docker/README.md`
- `LEARNING.md`
- `.gitignore` (corrected an initial rule that would have ignored Maven's `pom.xml`)

**Verification**
The Compose YAML was parsed successfully and its three services, two named volumes, and two networks were inspected. Docker Compose is not installed in this execution environment, so `docker compose config`, image pulls, container startup, and live health checks remain to be run on a machine with Docker Compose v2. No live-container success is claimed.


## Step 2.2 — Keycloak realm and clients

**What we implemented**
- Added an importable `platform` realm definition and enabled first-startup import through Docker Compose.
- Defined the bearer-only `platform-api` resource identifier, public `platform-swagger` client with Authorization Code + PKCE S256, and confidential `platform-admin-service` client with service accounts enabled.
- Added the `ADMIN`, `STAFF`, and `USER` client roles under `platform-api`.
- Added an explicit `platform-api` audience mapper to Swagger-issued access tokens.
- Disabled Full Scope Allowed on all clients and documented the explicit Swagger role-scope mapping.
- Documented manual, least-privilege service-account role assignment without committing credentials.

**Why**
Authentication, API audience validation, application authorization roles, and privileged Admin REST API access have distinct responsibilities. Separate clients make those boundaries explicit and keep the confidential service credential out of browser-facing Swagger UI.

**Important concepts**
- **Realm import:** Keycloak can import a realm JSON file at startup. Import is for realm creation; it does not continuously synchronize changes into an already-existing realm.
- **Public client:** cannot safely hold a client secret. Swagger uses Authorization Code with PKCE S256.
- **Confidential service account:** authenticates server-to-server with client credentials and must receive only the Admin REST API permissions it needs.
- **Audience (`aud`):** identifies the API for which an access token is intended. The backend will validate the API audience in addition to issuer and signature.
- **Client roles:** appear under `resource_access` in access tokens when their role mappings are included.
- **Role scope mapping:** limits which assigned roles a client is allowed to receive in its tokens; Full Scope Allowed remains disabled.

**Files changed**
- `compose.yaml`
- `infra/keycloak/realm/platform-realm.json`
- `docs/keycloak/README.md`
- `docs/docker/README.md`
- `LEARNING.md`

**Verification**
The realm definition was generated as JSON and structurally validated. Docker Compose and a live Keycloak container are not available in this execution environment, so the import, generated service-account credentials, role assignments, and actual JWT claims still require local runtime verification.


## Step 2.3 — Spring Boot project skeleton

**What we implemented**
- Created a Java 21 Maven project using Spring Boot 4.1.2.
- Added Spring MVC, Actuator, Bean Validation, Springdoc OpenAPI 3.2.0, and dedicated Spring Boot 4 MVC test dependencies.
- Added the main application class, a minimal versioned ping endpoint, and an MVC test.
- Added application metadata, health endpoint exposure, and package-by-feature architecture guidance.
- Kept database connectivity and Spring Security out of this skeleton; they are separate milestones.

**Why**
A small, testable application baseline gives later persistence and identity features a stable place to integrate without mixing infrastructure concerns into the initial application setup.

**Important concepts**
- **Maven parent/BOM:** Spring Boot's parent manages compatible dependency versions so individual Spring dependencies do not need arbitrary version pins.
- **Starter:** a curated dependency descriptor that brings in the modules needed for a capability.
- **Actuator:** exposes operational endpoints such as application health.
- **Spring Boot 4 test modules:** MVC test support is provided by the dedicated `spring-boot-starter-webmvc-test` dependency and its Boot 4 package.
- **Package by feature:** groups code by business capability rather than creating global controller/service/repository layers.

**Files changed**
- `pom.xml`
- `src/main/java/io/github/abhi07799/platform/PlatformApplication.java`
- `src/main/java/io/github/abhi07799/platform/system/api/SystemController.java`
- `src/main/resources/application.yaml`
- `src/test/java/io/github/abhi07799/platform/system/api/SystemControllerTest.java`
- `docs/architecture/package-structure.md`
- `README.md`
- `LEARNING.md`

**Verification**
The project structure and dependency coordinates were checked against the Spring Boot 4.1.2 and Springdoc OpenAPI 3.2.0 documentation. The Maven build and tests still need to be run in an environment with JDK 21 and Maven installed.
