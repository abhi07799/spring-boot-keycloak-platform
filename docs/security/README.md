# Spring Security and Keycloak Resource Server

## Token validation

The backend acts as an OAuth 2.0 JWT resource server. It validates:
- The JWT signature using the Keycloak realm's published JWK set.
- The exact issuer configured by `KEYCLOAK_ISSUER_URI`.
- Standard timestamp claims (`exp` and `nbf`).
- The `platform-api` audience.

The default local issuer is `http://localhost:8081/realms/platform`. If the backend runs inside Docker, override `KEYCLOAK_ISSUER_URI` with the issuer URL reachable from that container (for the Compose service, `http://keycloak:8080/realms/platform`). The configured issuer must exactly match the token's `iss` claim. Do not rewrite token issuer values to work around network addressing; configure Keycloak's externally advertised hostname consistently instead.

## Authorization mapping

Keycloak client roles are read only from `resource_access.platform-api.roles` and mapped to Spring authorities:
- `ADMIN` → `ROLE_ADMIN`
- `STAFF` → `ROLE_STAFF`
- `USER` → `ROLE_USER`

Only the three roles defined for the `platform-api` client are accepted. Roles from realm scope or other clients are not promoted to application authorities.

## HTTP access policy

Public:
- `POST /api/v1/auth/register` (registration only; assigns `USER`)
- `GET /api/v1/ping`
- `/actuator/health` and its subpaths
- Swagger UI and OpenAPI document endpoints

All other requests require a valid bearer access token. `GET /api/v1/users/me` resolves the application profile using the validated JWT `sub` claim; callers cannot provide another user's identifier. A subject without a corresponding profile receives HTTP 404. The response excludes the Keycloak subject identifier and credential data. `POST /api/v1/admin/users` additionally requires `ROLE_ADMIN`; authenticated `USER` and `STAFF` principals receive HTTP 403. The endpoint accepts only `STAFF` or `ADMIN` as the requested role. Public registration remains restricted to `USER`. The API is stateless; HTTP Basic, form login, and CSRF are disabled for this bearer-token-only API configuration.

## Swagger OAuth2 login

Swagger UI uses the public Keycloak client `platform-swagger` with OAuth 2.0 Authorization Code and PKCE S256. The OpenAPI document declares the OAuth2 authorization-code flow using the configured Keycloak realm issuer. Swagger UI sends the resulting access token as a bearer token when invoking secured operations.

Local settings:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Redirect URI: `http://localhost:8080/swagger-ui/oauth2-redirect.html`
- Client ID: `platform-swagger`
- PKCE: enabled (S256)
- Client secret: none; this is a public client.

The ping operation is marked public in the OpenAPI description. Secured operations inherit the OAuth2 security requirement. Do not enable authorization persistence in the browser, add a client secret, or use implicit/password grants.

## Backend-to-Keycloak service authentication

The backend uses a separate OAuth2 client registration named keycloak-admin for the confidential platform-admin-service client. It uses the Client Credentials grant and the realm's token endpoint. Spring Security's OAuth2AuthorizedClientManager obtains and reuses the service-account access token; the Keycloak Admin REST client sends it only in the Authorization bearer header.

Required environment variables:
- KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET: generated secret from Keycloak's Clients → Platform Admin Service → Credentials page.
- KEYCLOAK_ADMIN_BASE_URL: Keycloak server origin; defaults to http://localhost:8081. Use http://keycloak:8080 from another container on the Compose network.
- KEYCLOAK_ADMIN_REALM: defaults to platform.
- KEYCLOAK_ADMIN_TOKEN_URI: optional token endpoint override; defaults to the local platform realm token endpoint.

The Admin API client supports creating, deleting, and assigning a client role to Keycloak users. Public registration is implemented at `POST /api/v1/auth/register`; callers cannot select a role. The workflow always assigns only `platform-api/USER` and then persists the profile in the application database. `POST /api/v1/admin/users` is separately restricted to `ROLE_ADMIN` and accepts only `STAFF` or `ADMIN`; it provisions the requested role and persists the profile. Both workflows attempt to delete the newly created Keycloak identity if role assignment or profile persistence fails.

The registration payload is validated (email, full name, mobile number, and a password of 12–128 characters). Duplicate email is returned as HTTP 409, invalid input as HTTP 400, and registration failures as a generic HTTP 500 response. The password is never persisted in the application database or returned in the response.

The local imported realm currently has email verification disabled. Enable Keycloak email verification and configure SMTP before production use.

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
