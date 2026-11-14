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
- `src/main/java/io/github/abhi07799/platform/administration/api/AdminUserProvisioningController.java`
- `src/main/java/io/github/abhi07799/platform/administration/application/AdminUserProvisioningService.java`
- `src/main/java/io/github/abhi07799/platform/security/config/SecurityConfig.java`
- `src/test/java/io/github/abhi07799/platform/administration/api/AdminUserProvisioningControllerTest.java`
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
- **Deterministic sorting:** the query sorts newest profiles first rather than relying on unspecified database row order.
- **Bounded page size:** a maximum of 100 limits response size and database work per request.
- **DTO projection at the API boundary:** the endpoint reuses the public-safe profile response instead of serializing persistence entities.

**Files changed**
- `src/main/java/io/github/abhi07799/platform/administration/api/AdminUserProvisioningController.java`
- `src/main/java/io/github/abhi07799/platform/administration/api/AdminUserDirectoryResponse.java`
- `src/main/java/io/github/abhi07799/platform/administration/application/AdminUserDirectoryService.java`
- `src/test/java/io/github/abhi07799/platform/administration/api/AdminUserProvisioningControllerTest.java`
- `src/test/java/io/github/abhi07799/platform/administration/application/AdminUserDirectoryServiceTest.java`
- `README.md`
- `docs/security/README.md`
- `docs/architecture/package-structure.md`
- `LEARNING.md`

**Verification**
Tests were added but not executed because Maven is not installed in this execution environment. Run `mvn test` with JDK 21 and Maven. A live PostgreSQL/Keycloak check is also pending.
