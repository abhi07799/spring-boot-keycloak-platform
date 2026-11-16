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


## Step 3.4 — Public USER registration workflow

**What we implemented**
- Added a public `POST /api/v1/auth/register` endpoint with Bean Validation for full name, email, mobile number, and password.
- Normalized email addresses before identity creation and profile persistence; checked for existing profiles and retained the database's unique email constraint as the concurrency backstop.
- Created Keycloak identities using the confidential Admin API client and assigned only the fixed `platform-api/USER` client role. The request cannot select a role.
- Persisted the Keycloak subject ID, full name, normalized email, and mobile number in the application PostgreSQL database.
- Added a compensating Keycloak delete when role assignment or profile persistence fails after identity creation.
- Added safe HTTP error responses, registration controller tests, service workflow tests, and Keycloak deletion tests.
- Kept passwords out of the application database and registration response.

**Why**
Keycloak owns authentication credentials, while the application database owns the user profile. Creating an identity and saving a profile spans two independent systems and cannot be covered by one ACID transaction. The workflow therefore uses a compensating action to remove a newly created Keycloak user if a later step fails.

**Important concepts**
- **Public registration:** an unauthenticated endpoint that can create only a standard USER account; it cannot grant ADMIN or STAFF.
- **Email normalization:** trims and lowercases email before checking and creating the account.
- **Unique constraint:** the existing case-insensitive unique email index handles concurrent duplicate registration attempts even when both requests pass the pre-check.
- **Compensating action:** if role assignment or database persistence fails after Keycloak creation, the service attempts to delete that Keycloak user. This is not a distributed transaction; a failed compensation requires operational cleanup.
- **Credential ownership:** the password is sent to Keycloak as a password credential and is never stored in the application profile table or included in the response.
- **Email verification:** the imported local realm currently has email verification disabled; production deployment must configure SMTP and enable verification.

**Files changed**
- `src/main/java/io/github/abhi07799/platform/security/config/SecurityConfig.java`
- `src/main/java/io/github/abhi07799/platform/user/infrastructure/persistence/UserProfileRepository.java`
- `src/main/java/io/github/abhi07799/platform/identity/api/RegistrationRequest.java`
- `src/main/java/io/github/abhi07799/platform/identity/api/RegistrationResponse.java`
- `src/main/java/io/github/abhi07799/platform/identity/api/RegistrationController.java`
- `src/main/java/io/github/abhi07799/platform/identity/api/RegistrationExceptionHandler.java`
- `src/main/java/io/github/abhi07799/platform/identity/application/RegistrationService.java`
- `src/main/java/io/github/abhi07799/platform/identity/application/EmailAlreadyRegisteredException.java`
- `src/main/java/io/github/abhi07799/platform/identity/application/RegistrationFailedException.java`
- `src/main/java/io/github/abhi07799/platform/identity/application/RegistrationCompensationException.java`
- `src/main/java/io/github/abhi07799/platform/identity/infrastructure/keycloak/KeycloakAdminClient.java`
- `src/test/java/io/github/abhi07799/platform/identity/api/RegistrationControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/identity/application/RegistrationServiceTest.java`
- `src/test/java/io/github/abhi07799/platform/identity/infrastructure/keycloak/KeycloakAdminClientTest.java`
- `README.md`
- `docs/keycloak/README.md`
- `docs/security/README.md`
- `LEARNING.md`

**Verification**
The public registration route is explicitly permitted by the stateless security filter chain; all other non-public API paths still require a valid bearer token. Tests cover public endpoint access, validation, password omission from the response, default-role assignment, duplicate handling, and compensating deletion. Maven and a running Keycloak/PostgreSQL environment are not available here, so compilation, automated test execution, SMTP/email verification, and end-to-end registration remain to be run locally.



## Step 3.5 — Authenticated current-user profile API

**What we implemented**
- Added authenticated `GET /api/v1/users/me`.
- Resolved the caller's application profile by looking up the validated Keycloak JWT `sub` claim in PostgreSQL.
- Added a response DTO containing the application profile fields and timestamps, excluding the Keycloak subject identifier and credential data.
- Added an HTTP 404 Problem Detail response when the authenticated subject has no application profile.
- Added MVC security/response tests and service-level repository lookup tests.
- Documented the endpoint and the identity-resolution boundary.

**Why**
The API must derive the current identity from the access token rather than trusting a user ID supplied by the caller. This prevents a caller from changing a path or query parameter to retrieve another user's profile. The JWT subject is the stable link between Keycloak identity and the application-owned profile.

**Important concepts**
- **JWT subject (`sub`):** identifies the authenticated principal in the token. The API uses this value to find the matching application profile.
- **Principal-derived authorization:** the endpoint does not accept a user ID; the request's validated principal determines which profile is returned.
- **Data minimization:** the response includes profile fields needed by the client, not the internal Keycloak subject or authentication credentials.
- **404 profile state:** a valid authenticated identity may still have no corresponding application profile; this is reported as not found rather than exposing another user's data.

**Files changed**
- `src/main/java/io/github/abhi07799/platform/user/api/UserProfileController.java`
- `src/main/java/io/github/abhi07799/platform/user/api/UserProfileResponse.java`
- `src/main/java/io/github/abhi07799/platform/user/api/UserProfileExceptionHandler.java`
- `src/main/java/io/github/abhi07799/platform/user/application/UserProfileService.java`
- `src/main/java/io/github/abhi07799/platform/user/application/UserProfileNotFoundException.java`
- `src/test/java/io/github/abhi07799/platform/user/api/UserProfileControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/user/application/UserProfileServiceTest.java`
- `README.md`
- `docs/security/README.md`
- `docs/architecture/package-structure.md`
- `LEARNING.md`

**Verification**
Tests cover unauthenticated rejection, subject-based profile lookup, missing-profile HTTP 404, and omission of the Keycloak subject and password fields from the response. Maven is not installed in this execution environment, so compilation and automated test execution remain to be run locally with JDK 21 and Maven. Live Keycloak/PostgreSQL verification is also pending.


## Step 3.6 — Admin-controlled privileged user provisioning

**What we implemented**
- Added `POST /api/v1/admin/users`, protected by Spring Security `ROLE_ADMIN`.
- Added a validated request contract with a role enum that permits only `STAFF` or `ADMIN`.
- Added an application service that creates the Keycloak identity, assigns exactly the requested `platform-api` client role, and persists the application profile.
- Reused the existing Keycloak Admin REST client and safe registration response contract.
- Added compensating Keycloak deletion when role assignment or profile persistence fails after identity creation.
- Added controller tests for unauthenticated rejection, non-admin `403`, admin success, and invalid role rejection; added service tests for both supported roles, duplicate email, and compensation.
- Documented the one-time out-of-band first-ADMIN bootstrap procedure.

**Why**
Public registration must never grant privileged access. The administrative provisioning path is separate and checks the caller's `ROLE_ADMIN` authority before it can create either a STAFF or ADMIN account. The first application ADMIN must be established outside the public API so there is no unauthenticated privilege-escalation route.

**Important concepts**
- **Role-based access control (RBAC):** the endpoint requires the `ROLE_ADMIN` authority produced from the Keycloak `platform-api` client role.
- **Allowlisted role input:** the request enum makes `USER` and arbitrary role names invalid for this endpoint.
- **Out-of-band bootstrap:** the first application ADMIN is created through the Keycloak console and linked to an application profile row, not through a public bootstrap endpoint.
- **Compensating action:** if a later step fails after Keycloak user creation, the service attempts to delete that identity. This is best-effort across two independent systems, not a distributed transaction.

**Files changed**
- `src/main/java/io/github/abhi07799/platform/administration/api/AdminUserProvisioningRequest.java`
- `src/main/java/io/github/abhi07799/platform/administration/api/AdminUserController.java`
- `src/main/java/io/github/abhi07799/platform/administration/application/AdminUserProvisioningService.java`
- `src/main/java/io/github/abhi07799/platform/security/config/SecurityConfig.java`
- `src/test/java/io/github/abhi07799/platform/administration/api/AdminUserControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/administration/application/AdminUserProvisioningServiceTest.java`
- `README.md`
- `docs/security/README.md`
- `docs/keycloak/README.md`
- `docs/architecture/package-structure.md`
- `LEARNING.md`

**Verification**
Tests cover the intended authorization boundary, supported roles, duplicate handling, and compensation. Maven is not installed in this execution environment, so compilation and automated test execution remain pending local verification with JDK 21 and Maven. A live Keycloak/PostgreSQL test is also required to verify the actual service-account permissions for role mapping and user deletion.


## Step 3.7 — Authenticated current-user profile update

**What we implemented**
- Added `PUT /api/v1/users/me`, requiring a valid bearer access token like the existing GET endpoint.
- The API derives the profile identity only from the JWT `sub` claim; no user ID can be supplied by the caller.
- Added a request contract that validates a nonblank full name (maximum 150 characters) and a mobile number with 10–15 digits and an optional leading `+`.
- Updated only `fullName` and `mobileNumber`; email, Keycloak subject, and credentials are not writable through this endpoint.
- Trimmed the submitted editable values and used a write transaction so JPA dirty checking persists the managed profile. The entity `@PreUpdate` callback maintains `updatedAt`.
- Added controller and service tests for authentication, validation, subject-based lookup, update behavior, unchanged email, and missing-profile handling.

**Why**
A user may edit their application profile but must not be able to change the identity link or privileged attributes. The authenticated token determines the target profile, while Keycloak remains the owner of identity credentials and email identity.

**Important concepts**
- **Principal-derived resource access:** the target profile is always resolved from the validated token subject.
- **Allowlisted writable fields:** the request DTO exposes only fields the user is permitted to modify.
- **JPA dirty checking:** the managed entity is updated inside a transaction; the persistence context flushes the change without accepting a detached entity from the API.
- **Entity lifecycle callback:** `@PreUpdate` refreshes the `updated_at` timestamp when Hibernate writes an entity update.

**Files changed**
- `src/main/java/io/github/abhi07799/platform/user/api/UpdateUserProfileRequest.java`
- `src/main/java/io/github/abhi07799/platform/user/api/UserProfileController.java`
- `src/main/java/io/github/abhi07799/platform/user/application/UserProfileService.java`
- `src/main/java/io/github/abhi07799/platform/user/infrastructure/persistence/UserProfile.java`
- `src/test/java/io/github/abhi07799/platform/user/api/UserProfileControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/user/application/UserProfileServiceTest.java`
- `README.md`
- `docs/security/README.md`
- `docs/architecture/package-structure.md`
- `LEARNING.md`

**Verification**
Tests were added for the update workflow and authorization boundary. Maven is not installed in this execution environment, so compilation and automated test execution remain pending local verification with JDK 21 and Maven. A live Keycloak/PostgreSQL check is also pending.


## Step 3.8 — ADMIN-only paginated user directory

**What we implemented**
- Added `GET /api/v1/admin/users`, protected by the existing `ROLE_ADMIN` access rule for `/api/v1/admin/**`.
- Added a directory application service that reads application profiles using Spring Data JPA pagination.
- Added a deterministic newest-first ordering by `createdAt` with descending profile `id` as a tie-breaker.
- Added query parameters `page` (zero-based, default `0`) and `size` (default `20`, maximum `100`).
- Added an explicit response contract containing the user profile list and pagination metadata: page, size, total elements, total pages, first, and last.
- Reused the safe `UserProfileResponse`; the directory does not expose Keycloak subject IDs or credentials.
- Added controller tests for unauthenticated rejection, USER/STAFF forbidden access, ADMIN access, pagination metadata, and maximum page size; added a service test for pagination and sort order.

**Why**
Administrators need a bounded way to inspect application accounts after registration and provisioning. Pagination prevents an unbounded full-table response, while the existing ADMIN authorization boundary keeps profile data restricted to administrators.

**Important concepts**
- **Spring Data pagination:** `PageRequest` translates page and size into a database-level paginated query.
- **Deterministic sorting:** the query sorts newest profiles first and uses the profile ID as a tie-breaker rather than relying on unspecified database row order.
- **Bounded page size:** a maximum of 100 limits response size and database work per request.
- **DTO projection at the API boundary:** the endpoint reuses the public-safe profile response instead of serializing persistence entities.

**Files changed**
- `src/main/java/io/github/abhi07799/platform/administration/api/AdminUserController.java`
- `src/main/java/io/github/abhi07799/platform/administration/api/AdminUserDirectoryResponse.java`
- `src/main/java/io/github/abhi07799/platform/administration/application/AdminUserDirectoryService.java`
- `src/test/java/io/github/abhi07799/platform/administration/api/AdminUserControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/administration/application/AdminUserDirectoryServiceTest.java`
- `README.md`
- `docs/security/README.md`
- `docs/architecture/package-structure.md`
- `LEARNING.md`

**Verification**
Tests were added but not executed because Maven is not installed in this execution environment. Run `mvn test` with JDK 21 and Maven. A live PostgreSQL/Keycloak check is also pending.


## Step 3.9 — ADMIN account activation and deactivation

**What we implemented**
- Added `PATCH /api/v1/admin/users/{userId}/status`, protected by the existing `ROLE_ADMIN` rule.
- Added a request contract requiring an explicit `enabled` boolean; omitted or null status is rejected.
- Added a Flyway V2 migration that adds `user_profiles.enabled BOOLEAN NOT NULL DEFAULT TRUE`, preserving existing accounts as enabled.
- Added account status to the profile entity and safe profile response, so the paginated ADMIN directory shows each account's enabled state.
- Added a Keycloak Admin API operation that reads the full user representation, changes only its `enabled` field, and writes the representation back.
- Added an application service that updates Keycloak first, then persists the mirrored application status. If database persistence fails, it attempts to restore the previous Keycloak status. Failed compensation is logged with the application profile ID.
- Added API, service, and Keycloak client tests for disable, re-enable, authorization, validation, Keycloak failure, persistence failure, and failed compensation.
- Documented the account lifecycle endpoint, service-account permissions, and the access-token revocation limitation.

**Why**
Keycloak remains the identity provider and controls whether the account can authenticate or refresh tokens. The application database keeps a mirrored enabled flag for administrative visibility. Since Keycloak and PostgreSQL are separate systems, status synchronization uses a compensating action rather than pretending to provide a distributed ACID transaction.

**Important concepts**
- **Account enabled state:** disabling a Keycloak user blocks future authentication and token refresh for that identity.
- **Cross-system consistency:** the service updates Keycloak first and then writes the application mirror; if the database write fails, it attempts to restore the old Keycloak value.
- **Safe representation update:** the Admin API client reads the existing Keycloak user representation before changing `enabled`, preserving the other returned attributes.
- **Access-token lifetime:** the resource server validates JWTs locally. A token already issued before deactivation may remain valid until its expiry; disabling an account is not instantaneous revocation of every outstanding self-contained JWT.

**Files changed**
- `src/main/resources/db/migration/V2__add_user_profile_enabled_status.sql`
- `src/main/java/io/github/abhi07799/platform/user/infrastructure/persistence/UserProfile.java`
- `src/main/java/io/github/abhi07799/platform/user/api/UserProfileResponse.java`
- `src/main/java/io/github/abhi07799/platform/identity/infrastructure/keycloak/KeycloakAdminClient.java`
- `src/main/java/io/github/abhi07799/platform/administration/api/AdminUserController.java`
- `src/main/java/io/github/abhi07799/platform/administration/api/UpdateUserAccountStatusRequest.java`
- `src/main/java/io/github/abhi07799/platform/administration/api/AdminUserExceptionHandler.java`
- `src/main/java/io/github/abhi07799/platform/administration/application/AdminUserNotFoundException.java`
- `src/main/java/io/github/abhi07799/platform/administration/application/AdminUserStatusUpdateException.java`
- `src/main/java/io/github/abhi07799/platform/administration/application/KeycloakAccountStatusUpdateException.java`
- `src/main/java/io/github/abhi07799/platform/administration/application/AdminUserStatusCompensationException.java`
- `src/main/java/io/github/abhi07799/platform/administration/application/AdminUserStatusService.java`
- `src/test/java/io/github/abhi07799/platform/identity/infrastructure/keycloak/KeycloakAdminClientTest.java`
- `src/test/java/io/github/abhi07799/platform/administration/api/AdminUserControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/administration/application/AdminUserStatusServiceTest.java`
- `README.md`
- `docs/security/README.md`
- `docs/keycloak/README.md`
- `docs/architecture/package-structure.md`

**Verification**
Tests were added but not executed because Maven is not installed in this execution environment. Run `mvn test` with JDK 21 and Maven. Apply Flyway migration V2 and verify disable/re-enable against the local Keycloak 26.8.2 instance. Existing self-contained access tokens remain valid until expiry unless a separate revocation/introspection strategy is introduced.


## Step 3.10 — Immediate disabled-account token enforcement

**What we implemented**
- Added `AccountStatusJwtValidator` to the JWT validation pipeline.
- Every bearer-token authentication now looks up the JWT `sub` in `user_profiles`.
- Tokens are rejected as invalid when the subject is missing, no application profile exists, or the profile has `enabled = false`.
- Kept existing signature, issuer, expiry/not-before, and `platform-api` audience validation in place.
- Added unit tests for enabled, disabled, missing-profile, missing-subject, and repeated-validation cases.
- Updated MVC security-slice test configurations to provide a mocked profile repository.

**Why**
Keycloak account deactivation prevents future authentication and refresh, but self-contained JWTs may otherwise remain usable until expiration. The resource server now consults the application's enabled-state mirror whenever a bearer token is authenticated. Since no status cache is introduced, a completed deactivation is enforced on the next protected API request, even when the caller presents an older token.

**Important concepts**
- **JWT validation vs. authorization:** cryptographic and claim validation establish token authenticity; the additional account-status validator confirms the identity remains enabled in the application.
- **Fail closed:** if the application database cannot confirm the profile status, the request cannot be authenticated.
- **Per-request lookup:** every bearer-token authentication performs a profile lookup. The unique index on `keycloak_subject_id` supports this query. This adds a database dependency to authenticated request processing; consider a carefully designed revocation/cache strategy only if performance requires it, with an explicit maximum revocation delay.
- **Token lifecycle:** disabling the Keycloak identity blocks new logins/refreshes; the resource server status check also rejects already-issued access tokens on the next request after the application status update commits.

**Files changed**
- `src/main/java/io/github/abhi07799/platform/security/AccountStatusJwtValidator.java`
- `src/main/java/io/github/abhi07799/platform/security/config/SecurityConfig.java`
- `src/test/java/io/github/abhi07799/platform/security/AccountStatusJwtValidatorTest.java`
- `src/test/java/io/github/abhi07799/platform/administration/api/AdminUserControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/identity/api/RegistrationControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/system/api/SystemControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/user/api/UserProfileControllerTest.java`
- `README.md`
- `docs/security/README.md`

**Verification**
Tests were added but not executed because Maven is not installed in this execution environment. Run `mvn test` with JDK 21 and Maven, then verify with a live Keycloak/PostgreSQL setup: authenticate, call a protected endpoint, disable the account through the ADMIN endpoint, and retry the same endpoint using the original access token. The second request should return HTTP 401.


## Step 3.11 — Administrative audit logging

**What we implemented**
- Added Flyway V3 with an append-only `admin_audit_events` table and indexes for newest-first history and actor lookup.
- Recorded `USER_PROVISIONED`, `USER_DISABLED`, and `USER_ENABLED` events with the actor JWT subject, target application profile ID, timestamp, assigned role (for provisioning), and previous/new enabled state (for status changes).
- Derived the actor exclusively from the authenticated JWT `sub`; no actor identity is accepted from request payloads.
- Added `GET /api/v1/admin/audit-events`, protected by `ROLE_ADMIN`, with zero-based pagination (default 20, maximum 100) and deterministic newest-first sorting.
- Persisted profile mutations and their audit events in one application-database transaction. If audit persistence fails, the profile mutation rolls back and the existing Keycloak compensation path is invoked.
- Avoided storing passwords, tokens, email addresses, and mobile numbers in audit events. Repeated status requests that do not change state do not create duplicate events.
- Added persistence and ADMIN authorization/API tests.

**Why**
Administrative account changes need attributable, queryable history. Recording the actor from the validated token prevents callers from spoofing the administrator identity, while committing the audit event with the profile mutation prevents a successful application-side change from silently losing its audit record. Keycloak remains a separate system, so the existing compensating action is still required if the application database transaction fails after the Keycloak update.

**Important concepts**
- **Append-only audit record:** application code only inserts events; there is no update/delete workflow exposed. Database-level permissions and retention controls should further protect production audit data.
- **Atomic application persistence:** the profile row and its audit event share one PostgreSQL transaction. This does not create a distributed transaction with Keycloak.
- **Actor attribution:** the JWT subject identifies the acting Keycloak identity; the API never trusts a caller-supplied actor ID.
- **Data minimization:** audit entries retain stable profile IDs and action metadata rather than duplicating user PII or credentials.

**Verification**
Tests were added but not executed because Maven is unavailable in this environment. Run `mvn test` with JDK 21 and Maven, apply Flyway V3, then provision/disable/re-enable an account and verify the events through the ADMIN audit endpoint.


## Step 3.12 — ADMIN role management

**What we implemented**
- Added `PATCH /api/v1/admin/users/{userId}/role`, restricted by the existing `ROLE_ADMIN` policy.
- Accepts only `USER`, `STAFF`, or `ADMIN` and replaces only the target user's `platform-api` client role. Realm roles and roles belonging to other clients are left untouched.
- Reads the current platform role before changing it, assigns the requested role, then removes the previous role. If removing the previous mapping fails, it attempts to remove the newly added mapping.
- Prevents an administrator from changing their own role and prevents demotion of the last enabled `platform-api/ADMIN` user.
- Serializes role changes, ADMIN provisioning, and account-status updates across application instances with a PostgreSQL session-level advisory lock. The lock remains held through transaction rollback and any compensating Keycloak call, preventing concurrent lifecycle requests from racing with restoration.
- Adds Flyway V4 fields and constraints for previous/new roles and records `USER_ROLE_CHANGED` events with the actor JWT subject and target profile ID.
- Uses `TransactionTemplate` so database commit failures are visible before the session advisory lock is released; if audit persistence or transaction commit fails after Keycloak changes, the service attempts to restore the previous Keycloak role.
- Adds Keycloak client, application service, and controller authorization tests.

**Why**
Application roles are client roles owned by `platform-api`. Updating a role must not accidentally remove mappings from other clients or realm-level permissions. The last enabled administrator must remain available to manage the application, and role changes need the same attributable audit history as provisioning and account-status changes.

**Important concepts**
- **Client role mapping:** Keycloak stores `USER`, `STAFF`, and `ADMIN` as roles on the `platform-api` client. The API replaces only one of these mappings.
- **Last-admin invariant:** Keycloak's client-role users endpoint is queried for enabled ADMIN members. A session-level advisory lock serializes role changes, ADMIN provisioning, and account-status updates across application instances while this check and any compensation are performed.
- **Cross-system consistency:** Keycloak and PostgreSQL do not share a distributed transaction. The application commits the audit record inside the locked database transaction and compensates the Keycloak role if the database transaction fails.
- **Self-protection:** the authenticated administrator's JWT subject is compared with the target profile's Keycloak subject; the actor cannot alter their own application role.
- **Least privilege:** the service account needs narrowly scoped user/client query and role-mapping permissions. Verify exact Keycloak 26.8.2 permissions locally; never grant `realm-admin`.

**Verification**
Tests were added but not executed because Maven is unavailable in this environment. Run `mvn test` with JDK 21 and Maven. Apply Flyway V4, verify the service account can read client role mappings and role members, then test USER↔STAFF, STAFF↔ADMIN, last-admin protection, self-role protection, and audit rollback against Keycloak/PostgreSQL.

## Step 3.13 — Refresh token boundaries and session revocation

**What we implemented**
- Added authenticated POST /api/v1/auth/logout, which derives the current user only from the validated JWT subject.
- Added ADMIN-only POST /api/v1/admin/users/{userId}/logout to terminate all sessions for a selected application profile.
- Both endpoints call Keycloak's Admin REST API user logout operation. This is a user-wide logout: it terminates all active Keycloak sessions for the account, not just the browser/device that sent the request.
- Added Flyway V6 and the USER_SESSIONS_REVOKED audit action. Events record the actor JWT subject, target profile ID, and timestamp without storing tokens or profile PII.
- On successful Keycloak logout, the application advances user_profiles.authorization_updated_at. The JWT validator checks this marker on every request, so previously issued access tokens are rejected immediately after the audit transaction commits.
- Serialized session revocation with role changes, ADMIN provisioning, and account-status changes using the existing PostgreSQL session-level advisory lock.
- Added tests for Keycloak logout invocation, self-logout subject derivation, ADMIN authorization, audit persistence, and token-revocation timestamp updates.
- Documented that refresh-token exchanges belong directly to the OAuth client and are not proxied or persisted by this API.

**Why**
The API validates JWTs locally and does not maintain server-side HTTP sessions. Ending a Keycloak session prevents its refresh token from being used to continue that session, but a self-contained access token can otherwise remain locally valid until expiry. The application authorization timestamp provides an immediate local cutoff for access tokens issued before logout. The ADMIN endpoint supports terminating a compromised or departed user's sessions.

**Important concepts**
- **Refresh-token ownership:** an OAuth client exchanges its own refresh token directly with Keycloak's token endpoint. Refresh tokens are credentials and must not be sent to application API endpoints or written to logs.
- **User-wide logout:** Keycloak's Admin REST user logout endpoint removes all active sessions for the specified user. It is not a single-device logout operation.
- **Immediate access-token cutoff:** authorization_updated_at is compared with JWT iat during every bearer-token validation. A token issued before the cutoff is rejected even if its exp has not elapsed.
- **Cross-system failure:** Keycloak session termination and the PostgreSQL audit/cutoff transaction are not one distributed transaction. If Keycloak succeeds but the local transaction fails, the API returns an error and requires retry/reconciliation; the Keycloak logout cannot be rolled back.
- **No refresh-token proxy:** the backend does not accept, issue, proxy, or persist refresh tokens. The repository has no frontend token store; Swagger UI must re-authorize when its access token expires.

**Verification**
Tests were added but not executed because Maven is unavailable in this environment. Run mvn test with JDK 21 and Maven, apply Flyway V6, then verify self logout, ADMIN force logout, audit history, and rejection of a previously issued access token against live Keycloak 26.8.2/PostgreSQL.

## Step 3.14 — Password reset and email verification

**What we implemented**
- Enabled Keycloak realm settings `verifyEmail=true` and `resetPasswordAllowed=true`. New registrations remain `emailVerified=false`.
- After persisting a newly registered or ADMIN-provisioned profile, the backend asks Keycloak to send its built-in email-verification action link. If SMTP delivery fails, account creation remains successful and the user can request a resend.
- Added public `POST /api/v1/auth/password/forgot`. It requests Keycloak's built-in `UPDATE_PASSWORD` action email; the application never receives or stores a replacement password.
- Added public `POST /api/v1/auth/email/verification/resend`. It sends another verification email only for an enabled account whose Keycloak email is not yet verified.
- Both recovery endpoints return the same generic HTTP 202 message for unknown, disabled, and eligible email addresses. Provider failures are logged against the application profile ID without logging the email address, and are not disclosed to the caller.
- Configured a 900-second (15-minute) action-token lifetime by default, overridable with `KEYCLOAK_ACTION_TOKEN_LIFESPAN_SECONDS`.
- Added Keycloak Admin REST client tests, recovery service tests, controller tests, and registration email-delivery tests.
- Enabled email verification and password reset in the realm export, and documented SMTP setup without committing SMTP credentials.

**Why**
Credentials and email action tokens belong to Keycloak. The backend initiates the built-in flows but never implements password storage, token validation, or password updates itself. Generic recovery responses reduce direct account enumeration through status codes and response bodies.

**Important concepts**
- **Email verification:** Keycloak sends a one-time action link. The realm requires verification before normal login; the application does not treat a newly created email as verified.
- **Password reset:** the backend asks Keycloak to send the `UPDATE_PASSWORD` required action. The user chooses a new password on Keycloak's trusted action page.
- **Enumeration resistance:** both endpoints return HTTP 202 with the same message regardless of whether an eligible profile was found. This does not eliminate timing side channels; production deployments must add API-gateway rate limits, abuse monitoring, and mail-provider limits.
- **SMTP ownership:** SMTP configuration is performed in Keycloak Realm settings and is intentionally not stored in this repository. Existing realms are not overwritten by startup import; apply the realm setting changes manually if the realm already existed.
- **Action-token lifetime:** 900 seconds by default. Expired links require the user to request a new message.

**Verification**
Tests were added but not executed because Maven is unavailable in this environment. Run `mvn test` with JDK 21 and Maven. Configure and test SMTP in Keycloak 26.8.2, then verify registration email delivery, verification-required login, resend behavior, password-reset email, action-link expiry, and successful login with the updated password.


## Step 3.16 — Distributed rate limiting for public authentication endpoints

**What we implemented**
- Added a servlet filter that applies endpoint-specific limits before registration and account-recovery controller logic executes.
- Registration is limited to **10 requests per source IP per 60-second fixed window**.
- Password-reset and verification-resend endpoints are each limited to **5 requests per source IP per 900-second fixed window**.
- Added Flyway V7 with a PostgreSQL bucket table. An atomic `INSERT ... ON CONFLICT DO UPDATE ... RETURNING` increments counters safely across concurrent requests and multiple application instances.
- Bucket identifiers are HMAC-SHA256 digests of the endpoint path and source IP. Raw IP addresses are not stored in the rate-limit table.
- Added a scheduled cleanup job that deletes expired buckets. The cleanup interval is configurable.
- Requests over the limit receive HTTP 429 with a `Retry-After` header and a generic response. If the rate-limit database operation fails, the filter fails closed with HTTP 503 rather than silently bypassing protection.
- Added filter tests and updated MVC tests to mock the rate-limit persistence dependency.

**Why**
Public registration and recovery endpoints can be abused for account creation, email flooding, and provider-cost exhaustion. A process-local in-memory counter is insufficient when the application runs on multiple replicas: each replica would maintain a separate counter. PostgreSQL provides one shared counter store and atomic increments, so all instances enforce the same limit.

**Important concepts**
- **Fixed-window counter:** requests are grouped into aligned time windows. It is simple and efficient, but a caller can make requests near the end of one window and again at the start of the next; the short-term burst can therefore approach twice the configured limit.
- **Atomic upsert:** PostgreSQL increments the existing counter as one database statement. Concurrent application instances cannot both read the same old value and overwrite one another with a lost update.
- **HMAC pseudonymization:** the bucket key is a keyed digest rather than a raw IP address. Keep `RATE_LIMIT_HMAC_SECRET` private and stable across replicas in one environment; rotate it deliberately because rotation starts new buckets.
- **Fail closed:** if PostgreSQL cannot enforce the limit, these public endpoints return 503. This avoids turning a storage outage into an unrestricted public endpoint.
- **Proxy boundary:** the filter uses `HttpServletRequest.getRemoteAddr()`; it does not trust arbitrary `X-Forwarded-For` values. Configure the trusted reverse proxy/container to resolve the real client address safely. Do not accept client-supplied forwarding headers directly.
- **Defense in depth:** IP limits reduce automated abuse but do not replace bot detection, gateway/WAF controls, email-provider quotas, or per-account controls. Shared networks can also cause legitimate users to share an IP bucket.

**Configuration**
- `RATE_LIMIT_HMAC_SECRET`: private random secret, minimum 32 characters. The repository default is for local development only.
- `RATE_LIMIT_CLEANUP_INTERVAL_MS`: cleanup interval in milliseconds; defaults to 3,600,000 (one hour).
- Change the thresholds in `PublicAuthRateLimitFilter` only with an explicit capacity and abuse-risk decision.

**Verification**
Run `mvn test` with JDK 21 and Maven. Apply Flyway V7 and exercise the three public endpoints repeatedly from one source IP. Confirm the first configured number of requests reach the controller, the next request returns 429 with `Retry-After`, another source IP receives an independent bucket, and expired rows are removed by the scheduled cleanup. Also verify behavior through the actual reverse proxy before production rollout.
