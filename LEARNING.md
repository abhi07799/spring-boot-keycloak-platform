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


## Step 2.4 — PostgreSQL, Spring Data JPA, and Flyway

**What we implemented**
- Added Spring Data JPA, the Spring Boot Flyway starter, the PostgreSQL Flyway database module, and the PostgreSQL JDBC driver.
- Configured the backend to connect to the application database, with environment-variable overrides for local and containerized execution.
- Set Hibernate to validate the schema and disabled Open EntityManager in View.
- Added the first Flyway migration for the application user profile table.
- Added a JPA entity and repository for user profiles.
- Kept the existing ping test as an MVC slice so it does not require a running database.

**Why**
Flyway provides a versioned, reviewable source of truth for database schema changes. Hibernate validation catches mismatches between the entity mapping and the migrated schema without silently changing the schema.

**Important concepts**
- **JPA entity:** maps a Java object to a relational table.
- **Spring Data repository:** provides typed persistence operations without hand-writing basic CRUD SQL.
- **Flyway migration:** an immutable, ordered SQL change recorded in `flyway_schema_history`.
- **Schema validation:** Hibernate checks that the database schema is compatible with entity mappings; it does not create or update tables.
- **Database boundary:** the backend connects only to the application PostgreSQL database. Keycloak's internal database remains separate.

**Files changed**
- `pom.xml`
- `src/main/resources/application.yaml`
- `src/main/resources/db/migration/V1__create_user_profiles.sql`
- `src/main/java/io/github/abhi07799/platform/user/infrastructure/persistence/UserProfile.java`
- `src/main/java/io/github/abhi07799/platform/user/infrastructure/persistence/UserProfileRepository.java`
- `src/test/java/io/github/abhi07799/platform/system/api/SystemControllerTest.java`
- `docs/database/README.md`
- `README.md`
- `LEARNING.md`

**Verification**
The migration SQL, YAML, and POM were structurally inspected. The migration has a unique Keycloak subject index and a case-insensitive unique email index. Docker and Maven runtime verification are not available in this execution environment, so Flyway execution, Hibernate schema validation, and the full test suite still need to be run locally against PostgreSQL.

 
## Step 3.1 — Spring Security and Keycloak JWT resource server

**What we implemented**
- Added the Spring Boot 4.1.2 OAuth2 Resource Server starter and its test starter.
- Configured stateless bearer-token security with public health, documentation, Swagger, and ping endpoints; all other requests require authentication.
- Added JWT signature, issuer, timestamp, and `platform-api` audience validation using Keycloak's JWK endpoint.
- Mapped only the `ADMIN`, `STAFF`, and `USER` roles from `resource_access.platform-api.roles` to Spring Security authorities.
- Added MVC security tests and focused role-converter tests.
- Documented local and container issuer configuration.

**Why**
A valid signature alone is not enough: an API should also reject tokens issued by an unexpected realm or intended for a different audience. Explicit role mapping prevents unrelated Keycloak roles from accidentally becoming application permissions.

**Important concepts**
- **Resource server:** an API that accepts and validates OAuth 2.0 access tokens.
- **JWK set:** Keycloak's published public keys used to verify JWT signatures.
- **Issuer (`iss`):** the identity provider that issued the token; it must match the configured realm issuer exactly.
- **Audience (`aud`):** the intended recipient API; this backend requires `platform-api`.
- **Granted authority:** Spring Security's internal permission name; Keycloak client roles become `ROLE_ADMIN`, `ROLE_STAFF`, and `ROLE_USER`.
- **Stateless security:** the backend does not create an HTTP session to retain authentication state.

**Files changed**
- `pom.xml`
- `src/main/resources/application.yaml`
- `src/main/java/io/github/abhi07799/platform/security/config/SecurityConfig.java`
- `src/main/java/io/github/abhi07799/platform/security/KeycloakRoleConverter.java`
- `src/test/java/io/github/abhi07799/platform/system/api/SystemControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/security/KeycloakRoleConverterTest.java`
- `docs/security/README.md`
- `README.md`
- `LEARNING.md`

**Verification**
The security rules and tests were statically reviewed against the Spring Security resource-server model. Maven, Docker, and a running Keycloak instance are unavailable in this execution environment; compilation, automated test execution, token signature validation, and live issuer/audience checks must be run locally.


## Step 3.2 — Swagger UI OAuth2 Authorization Code with PKCE

**What we implemented**
- Configured Swagger UI with the public Keycloak client ID `platform-swagger`.
- Enabled Authorization Code with PKCE S256 and left the client secret unset.
- Added an OpenAPI OAuth2 authorization-code security scheme whose authorization and token endpoints are derived from the configured Keycloak realm issuer.
- Applied the OAuth2 security requirement to API operations and explicitly marked the public ping operation as unauthenticated in the OpenAPI document.
- Disabled authorization persistence in Swagger UI.
- Documented the exact local redirect URI, Keycloak client settings, and browser-based verification steps.

**Why**
Swagger UI is a browser-based public OAuth client. PKCE protects the authorization-code exchange without placing a client secret in the browser. The API still independently validates the access token; Swagger UI configuration does not replace backend security.

**Important concepts**
- **Authorization Code flow:** the browser is redirected to Keycloak, receives a short-lived authorization code, and exchanges it for tokens.
- **PKCE S256:** Swagger UI creates a verifier and sends its SHA-256 challenge; the verifier is required at token exchange.
- **Redirect URI:** the exact callback URL registered on the Keycloak client; mismatches are rejected.
- **OpenAPI security scheme:** tells Swagger UI which OAuth flow and endpoints to use.
- **Public client:** a browser cannot keep a client secret confidential, so the Swagger client has no secret.

**Files changed**
- `src/main/resources/application.yaml`
- `src/main/java/io/github/abhi07799/platform/system/config/OpenApiConfig.java`
- `src/main/java/io/github/abhi07799/platform/system/api/SystemController.java`
- `src/test/java/io/github/abhi07799/platform/system/config/OpenApiConfigTest.java`
- `docs/keycloak/README.md`
- `docs/security/README.md`
- `README.md`
- `LEARNING.md`

**Verification**
Springdoc's documented Swagger UI OAuth properties and Keycloak's OIDC authorization/token endpoint paths were checked. The OpenAPI authorization/token endpoint configuration has focused unit tests. The ping handler is annotated as public in the OpenAPI description. Maven, Docker, and a running Keycloak/browser environment are unavailable here; compilation, tests, redirect URI matching, PKCE exchange, and end-to-end API invocation must be verified locally.


## Step 3.3 — Keycloak Admin REST API client

**What we implemented**
- Added Spring Security OAuth2 Client support alongside the existing resource-server support, plus Spring Boot's RestClient starter for imperative Admin REST calls.
- Configured a separate confidential platform-admin-service client registration using the Client Credentials grant and HTTP Basic client authentication.
- Added an OAuth2AuthorizedClientManager backed by the authorized-client service to obtain and reuse service-account access tokens.
- Added a typed Keycloak Admin REST API client for creating users and assigning client roles.
- Role assignment resolves the Keycloak internal client UUID and role representation before posting the user role mapping.
- Added unit tests for token acquisition failure/success, user creation response handling, and the client-role assignment request sequence.
- Documented secret provisioning, local/container base URLs, and least-privilege service-account permissions.

**Why**
The backend needs a server-to-server identity to perform privileged identity lifecycle operations. A confidential service account separates these operations from end-user tokens and from the public Swagger client. The access token is obtained through the OAuth2 Client Credentials grant and is never exposed to a browser.

**Important concepts**
- **Client Credentials grant:** the backend authenticates as its own service account; there is no end-user session.
- **Authorized client manager:** Spring Security obtains and reuses the client access token, handling token expiry and re-authorization.
- **Admin REST API:** Keycloak's administrative endpoints require a bearer token with sufficient service-account permissions.
- **Client UUID vs. client ID:** Admin REST role-mapping endpoints use Keycloak's internal client UUID, not the human-readable client ID. The client resolves this before assigning a role.
- **Least privilege:** grant only the realm-management permissions needed for the specific user and role operations; do not grant realm-admin.

**Files changed**
- pom.xml
- src/main/resources/application.yaml
- src/main/java/io/github/abhi07799/platform/identity/infrastructure/keycloak/KeycloakAdminOAuth2Configuration.java
- src/main/java/io/github/abhi07799/platform/identity/infrastructure/keycloak/KeycloakAdminTokenProvider.java
- src/main/java/io/github/abhi07799/platform/identity/infrastructure/keycloak/KeycloakAdminClient.java
- src/main/java/io/github/abhi07799/platform/identity/infrastructure/keycloak/KeycloakUserRepresentation.java
- src/test/java/io/github/abhi07799/platform/identity/infrastructure/keycloak/KeycloakAdminTokenProviderTest.java
- src/test/java/io/github/abhi07799/platform/identity/infrastructure/keycloak/KeycloakAdminClientTest.java
- docs/keycloak/README.md
- docs/security/README.md
- README.md
- LEARNING.md

**Verification**
Dependency coordinates were checked against the Spring Boot 4.1.2 managed dependency list. Keycloak Client Credentials and Admin REST API endpoint behavior were checked against Keycloak 26.8 documentation. Unit tests were added but cannot be executed here because Maven is not installed. A live Keycloak test is also pending; confirm the actual role-mapping permissions with the local Keycloak 26.8.2 realm and its service-account configuration.
