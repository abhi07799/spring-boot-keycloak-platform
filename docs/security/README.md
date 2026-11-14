# Spring Security and Keycloak Resource Server

## Token validation

The backend acts as an OAuth 2.0 JWT resource server. It validates:
- The JWT signature using the Keycloak realm's published JWK set.
- The exact issuer configured by `KEYCLOAK_ISSUER_URI`.
- Standard timestamp claims (`exp` and `nbf`).
- The `platform-api` audience.
- The application profile status for the JWT `sub`: a matching profile must exist and have `enabled = true` in PostgreSQL.
- The JWT `iat` must be later than the profile's `authorization_updated_at` timestamp. Role changes, account-status changes, and successful session revocation advance this timestamp, invalidating access tokens issued before those changes.

The default local issuer is `http://localhost:8081/realms/platform`. If the backend runs inside Docker, override `KEYCLOAK_ISSUER_URI` with the issuer URL reachable from that container (for the Compose service, `http://keycloak:8080/realms/platform`). The configured issuer must exactly match the token's `iss` claim. Do not rewrite token issuer values to work around network addressing; configure Keycloak's externally advertised hostname consistently instead. The account-status/authorization validator queries `user_profiles` for every bearer-token authentication and rejects missing or disabled profiles, or tokens issued before the latest authorization/session change, as invalid tokens. No account-status cache is used, so a successfully completed admin deactivation takes effect on the next protected API request, including requests carrying an access token issued earlier. If the application database is unavailable, token validation cannot confirm account status and authentication fails closed.

## Authorization mapping

Keycloak client roles are read only from `resource_access.platform-api.roles` and mapped to Spring authorities:
- `ADMIN` → `ROLE_ADMIN`
- `STAFF` → `ROLE_STAFF`
- `USER` → `ROLE_USER`

Only the three roles defined for the `platform-api` client are accepted. Roles from realm scope or other clients are not promoted to application authorities.

## HTTP access policy

Public:
- `POST /api/v1/auth/register` (registration only; assigns `USER` and requests a Keycloak email-verification message)
- `POST /api/v1/auth/password/forgot` (generic public password-reset request; returns HTTP 202)
- `POST /api/v1/auth/email/verification/resend` (generic public verification-resend request; returns HTTP 202)
- `GET /api/v1/ping`
- `/actuator/health` and its subpaths
- Swagger UI and OpenAPI document endpoints

All other requests require a valid bearer access token. `POST /api/v1/auth/logout` is authenticated and terminates all Keycloak sessions for the current JWT subject. `POST /api/v1/admin/users/{userId}/logout` is restricted to `ROLE_ADMIN` and terminates every Keycloak session for the selected profile. `GET /api/v1/users/me` resolves the application profile using the validated JWT `sub` claim; callers cannot provide another user's identifier. A subject without a corresponding profile receives HTTP 404. The response excludes the Keycloak subject identifier and credential data. `PUT /api/v1/users/me` updates only `fullName` and `mobileNumber`, using the same JWT subject lookup; it does not accept a user ID, email, or role from the caller. Email and credentials remain managed by Keycloak and cannot be changed through this profile endpoint. Both `GET /api/v1/admin/users` (paginated directory) and `POST /api/v1/admin/users` (provisioning) require `ROLE_ADMIN`; `GET /api/v1/admin/audit-events` is also restricted to `ROLE_ADMIN` and returns a paginated immutable history of admin provisioning, account status changes, and role changes. The actor is taken from the authenticated JWT subject, never from request data. Audit events store the target application profile ID, action, occurrence time, assigned role for provisioning, and previous/new enabled flags for status changes; they do not store passwords, access tokens, or profile email/mobile data. Provisioning and status audit inserts are committed in the same application-database transaction as their profile mutations; role-change audit inserts commit in the serialized role-change transaction. Authenticated `USER` and `STAFF` principals receive HTTP 403. The directory returns application profile fields only and uses a fixed newest-first sort by creation timestamp with profile ID as a tie-breaker. It accepts a zero-based `page` (default `0`) and `size` (default `20`, maximum `100`). The provisioning endpoint accepts only `STAFF` or `ADMIN` as the requested role. `PATCH /api/v1/admin/users/{userId}/status` accepts only a required boolean `enabled` field and synchronizes the Keycloak user with the application profile status. `PATCH /api/v1/admin/users/{userId}/role` accepts only `USER`, `STAFF`, or `ADMIN`, replaces only the corresponding `platform-api` client role, and records the old/new roles in the audit log. Both logout endpoints record `USER_SESSIONS_REVOKED` with the actor JWT subject and target profile ID. The Keycloak Admin API logout operation removes all sessions for that user; the local authorization timestamp additionally causes previously issued bearer tokens to fail validation immediately after the audit transaction commits. If Keycloak logout succeeds but local persistence fails, the API returns HTTP 500 and requires a retry/reconciliation; the external Keycloak operation cannot be rolled back. An administrator cannot change their own role, and the last enabled application administrator cannot be demoted or disabled. Role changes, ADMIN account provisioning, and account-status updates share a PostgreSQL session-level advisory lock. It remains held through transaction rollback and Keycloak compensation, so concurrent lifecycle operations cannot interleave with a failed operation's restoration. If profile persistence fails after Keycloak is updated, the status service attempts to restore the previous Keycloak state; failed compensation is logged for operational reconciliation. Role changes use a PostgreSQL transaction template so commit failures are also detected before returning; the service attempts to restore the previous Keycloak role if audit persistence or transaction commit fails. Account status is also enforced by the resource server against PostgreSQL on every bearer-token authentication, so already-issued tokens are rejected on the next protected API request after the status update is committed. Keycloak remains responsible for blocking future authentication and refresh. Public registration remains restricted to `USER`. The realm requires verified email for login and enables Keycloak's reset-password flow. Registration and ADMIN provisioning create users with `emailVerified=false` and ask Keycloak to send the built-in verify-email action after profile persistence. Password reset asks Keycloak to send the `UPDATE_PASSWORD` action; the backend never receives or stores a replacement password. Unknown, disabled, already verified, and eligible email addresses receive the same generic 202 response from the recovery endpoints. The implementation does not itself provide distributed rate limiting; deploy edge/API-gateway rate limits and mail-provider abuse controls before production. The API is stateless; HTTP Basic, form login, and CSRF are disabled for this bearer-token-only API configuration. It does not accept, issue, proxy, or persist refresh tokens. OAuth clients refresh directly against Keycloak's realm token endpoint using the OAuth 2.0 refresh-token grant; refresh tokens must remain with the client and must never be sent to application API endpoints.

## Swagger OAuth2 login

Swagger UI uses the public Keycloak client `platform-swagger` with OAuth 2.0 Authorization Code and PKCE S256. The OpenAPI document declares the OAuth2 authorization-code flow using the configured Keycloak realm issuer. Swagger UI sends the resulting access token as a bearer token when invoking secured operations.

Local settings:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Redirect URI: `http://localhost:8080/swagger-ui/oauth2-redirect.html`
- Client ID: `platform-swagger`
- PKCE: enabled (S256)
- Client secret: none; this is a public client.

The ping operation is marked public in the OpenAPI description. Secured operations inherit the OAuth2 security requirement. Do not enable authorization persistence in the browser, add a client secret, or use implicit/password grants. The API does not implement a refresh-token proxy; a future browser/mobile client must perform refresh-token exchanges directly with Keycloak and securely protect its refresh token.

## Backend-to-Keycloak service authentication

The backend uses a separate OAuth2 client registration named keycloak-admin for the confidential platform-admin-service client. It uses the Client Credentials grant and the realm's token endpoint. Spring Security's OAuth2AuthorizedClientManager obtains and reuses the service-account access token; the Keycloak Admin REST client sends it only in the Authorization bearer header.

Required environment variables:
- KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET: generated secret from Keycloak's Clients → Platform Admin Service → Credentials page.
- KEYCLOAK_ADMIN_BASE_URL: Keycloak server origin; defaults to http://localhost:8081. Use http://keycloak:8080 from another container on the Compose network.
- KEYCLOAK_ADMIN_REALM: defaults to platform.
- KEYCLOAK_ADMIN_TOKEN_URI: optional token endpoint override; defaults to the local platform realm token endpoint.

The Admin API client supports creating, reading, updating account enabled status, sending Keycloak email-verification and password-update action emails, terminating all sessions for a user, deleting users, assigning client roles, replacing the platform-api application role, and querying enabled ADMIN role members. Public registration is implemented at `POST /api/v1/auth/register`; callers cannot select a role. The workflow always assigns only `platform-api/USER` and then persists the profile in the application database. `POST /api/v1/admin/users` is separately restricted to `ROLE_ADMIN` and accepts only `STAFF` or `ADMIN`; it provisions the requested role and persists the profile. Both workflows attempt to delete the newly created Keycloak identity if role assignment or profile persistence fails.

The registration payload is validated (email, full name, mobile number, and a password of 12–128 characters). Duplicate email is returned as HTTP 409, invalid input as HTTP 400, and registration failures as a generic HTTP 500 response. The profile update payload requires a nonblank full name (maximum 150 characters) and a mobile number containing 10–15 digits with an optional leading `+`. The profile update returns the refreshed profile representation; its `updatedAt` value is maintained by the JPA entity lifecycle callback. The password is never persisted in the application database or returned in the response.

The realm import enables email verification and password reset, but SMTP is intentionally left unconfigured in source control. Configure and test SMTP in Keycloak before local email-flow testing or production use. For a realm that already existed before this change, update Verify email and Reset password manually because startup import does not overwrite existing realms. Action-token emails expire after 900 seconds by default.

## Local configuration

The default issuer is configured in `application.yaml`. Override it when necessary:

```bash
export KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/platform
```

PowerShell:

```powershell
$env:KEYCLOAK_ISSUER_URI = "http://localhost:8081/realms/platform"
```

If Swagger is opened from another host or port, set `SWAGGER_OAUTH2_REDIRECT_URL` to the matching Swagger OAuth redirect URI and update the `platform-swagger` client's redirect URI and web origin in Keycloak. Exact matches are required; do not use wildcards.

Start Keycloak using the repository's Compose setup before making authenticated requests. The Keycloak realm must have been imported, and the access token must include `platform-api` in `aud`. A token missing that audience is rejected even if its signature and issuer are otherwise valid.

## Verification

The automated tests cover the OpenAPI authorization/token endpoint configuration and issuer URL normalization. The ping handler is annotated as public in the OpenAPI description. Run `mvn test` locally with JDK 21 and Maven. End-to-end login, redirect URI matching, PKCE exchange, role scopes, and live API invocation require the Compose Keycloak service and a browser.
