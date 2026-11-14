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
- `GET /api/v1/ping`
- `/actuator/health` and its subpaths
- Swagger UI and OpenAPI document endpoints

All other requests require a valid bearer access token. The API is stateless; HTTP Basic, form login, and CSRF are disabled for this bearer-token-only API configuration. Role-specific endpoint authorization will be added alongside the corresponding business APIs.

## Local configuration

The default issuer is configured in `application.yaml`. Override it when necessary:

```bash
export KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/platform
```

PowerShell:

```powershell
$env:KEYCLOAK_ISSUER_URI = "http://localhost:8081/realms/platform"
```

Start Keycloak using the repository's Compose setup before making authenticated requests. The Keycloak realm must have been imported, and the access token must include `platform-api` in `aud`. A token missing that audience is rejected even if its signature and issuer are otherwise valid.

## Verification

The automated tests cover public ping access, unauthenticated rejection for other API paths, and strict client-role mapping. Run `mvn test` locally with JDK 21 and Maven. Live JWT verification additionally requires the Compose Keycloak service and a token issued by the `platform` realm.
