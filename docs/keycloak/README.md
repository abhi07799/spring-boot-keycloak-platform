# Keycloak Realm Configuration

## Imported realm

The repository provisions a realm named `platform` from `infra/keycloak/realm/platform-realm.json`. Keycloak imports it at startup only when a realm with that name does not already exist.

Direct Keycloak self-registration is disabled. The platform's public registration flow will be implemented by the backend and will assign only the `USER` client role.

## Clients

| Client ID | Type | Purpose |
|---|---|---|
| `platform-api` | Bearer-only | API resource identifier and owner of platform client roles |
| `platform-swagger` | Public | Swagger UI Authorization Code flow with PKCE S256 |
| `platform-admin-service` | Confidential service account | Backend-to-Keycloak Admin REST API access |

The Swagger client is configured for Authorization Code flow with PKCE S256. Its exact local redirect URI is `http://localhost:8080/swagger-ui/oauth2-redirect.html`, and its web origin is `http://localhost:8080`. These values must match the URL used to open Swagger UI. Update both values for another host or port; never use wildcard redirect URIs.

The Springdoc configuration uses the public client ID `platform-swagger`, enables PKCE, and does not configure a client secret. Swagger UI obtains the authorization and token endpoints from the OpenAPI OAuth2 authorization-code scheme, built from the configured realm issuer.

The Swagger client has an explicit audience mapper for `platform-api`, so its access tokens can carry the API audience claim. The API client roles are emitted under `resource_access.platform-api.roles` when the corresponding role mappings are included in the token.

Full Scope Allowed is disabled on all three clients. After import, explicitly add the API roles to the Swagger client's dedicated role scope so those roles can be included in Swagger-issued access tokens:

1. Open **Clients → Platform Swagger → Client scopes**.
2. Open the dedicated scope for `platform-swagger` (not the shared built-in `roles` scope).
3. Open its **Scope** tab and add the `ADMIN`, `STAFF`, and `USER` roles from the `platform-api` client.
4. Save and use the client's scope evaluation to confirm that the expected client roles and `platform-api` audience appear in the access token.

This is an explicit least-privilege mapping: do not enable Full Scope Allowed as a shortcut.

## Client roles

The `platform-api` client owns these roles:

- `USER`: standard registered account.
- `STAFF`: staff-level access.
- `ADMIN`: administrative access.

These are client roles, not realm roles. Application authorization must read the client role claim for `platform-api`.

## Configure the admin service account

The confidential client is imported with service accounts enabled and without a committed client secret. After the first realm import:

1. Sign in to the Keycloak Admin Console with the local bootstrap administrator.
2. Select the `platform` realm, then open **Clients → Platform Admin Service → Credentials**.
3. Retrieve the generated client secret and store it only in the local ignored `.env` file or a local secrets manager. Never commit it.
4. Open **Service account roles** for `platform-admin-service`.
5. In the `realm-management` client roles, grant only the permissions required by the backend's user lifecycle operations: start with `query-users`, `view-users`, `manage-users`, `query-clients`, and `view-clients`. Verify the exact operations against the Admin REST API calls used by the application.
6. Do not grant `realm-admin` or broad server-administrator roles to this service account.

Keycloak 26.8.2 applies additional restrictions to protocol-mapper administration. This service account is not intended to create or modify clients, client scopes, or protocol mappers. Those are infrastructure/bootstrap responsibilities.

## Backend Admin REST API integration

The backend uses the platform-admin-service confidential client with the OAuth 2.0 Client Credentials grant. The client belongs to the platform realm, has service accounts enabled, and authenticates with HTTP Basic client authentication. The backend obtains a service-account access token from the realm token endpoint and calls the Admin REST API with that bearer token.

For the current backend operations, grant the service account only the realm-management client roles required to:
- Create users (manage-users).
- Read and update user account status (view-users, manage-users).\n- Query users if/when the registration workflow needs lookup (query-users, view-users).
- Resolve the platform-api client and its roles (query-clients, view-clients).

Keycloak's authorization model can vary with fine-grained admin permissions and realm configuration. Verify each operation in the local Keycloak 26.8.2 instance; do not grant realm-admin as a shortcut. Role-mapping operations may require an additional narrowly scoped permission depending on the Keycloak admin-permission configuration.

The public backend registration endpoint is `POST /api/v1/auth/register`. It accepts full name, email, mobile number, and password; creates the identity through the Admin REST API; assigns only the `USER` client role under `platform-api`; and persists the profile in the application database. Passwords are sent directly to Keycloak and are never stored in the application database or returned by the API.

The privileged endpoint `POST /api/v1/admin/users` requires an access token containing `ROLE_ADMIN`. It accepts the same profile and password fields plus a `role` value restricted to `STAFF` or `ADMIN`. The backend creates the Keycloak identity, assigns exactly the requested `platform-api` client role, and persists the application profile. A caller cannot request `USER` through this administrative endpoint; standard accounts use public registration. Role authorization is enforced by Spring Security before the controller is invoked.

### One-time first application ADMIN bootstrap

The admin provisioning API cannot create the first application administrator because no application principal has `ROLE_ADMIN` yet. Bootstrap that first administrator out of band, before exposing the admin endpoint:

1. In the Keycloak Admin Console, create a user in the `platform` realm, set the required profile fields, and configure a password using the console.
2. From that user's details, copy the Keycloak user ID (the subject identifier).
3. Assign the `ADMIN` client role from the `platform-api` client to that Keycloak user. Do not assign a realm role.
4. Insert the matching profile row into the application database using the same Keycloak user ID. Example (replace every placeholder with the actual user data; execute only once against the application database):

```sql
INSERT INTO user_profiles (id, keycloak_subject_id, full_name, email, mobile_number)
VALUES (gen_random_uuid(), '<KEYCLOAK_USER_ID>', '<FULL_NAME>', '<EMAIL>', '<MOBILE_NUMBER>');
```

5. Sign in through the `platform-swagger` client and confirm the access token contains `resource_access.platform-api.roles` with `ADMIN` and the `platform-api` audience.
6. Call `GET /api/v1/users/me` to confirm the profile is linked, then use the admin provisioning endpoint.

Keep this as a controlled, audited bootstrap operation. Do not add a public bootstrap HTTP endpoint, commit bootstrap credentials, or grant `realm-admin` to the backend service account.

If role assignment or profile persistence fails after Keycloak user creation, the backend attempts to delete the newly created Keycloak user as a compensating action. This is a cross-system workflow, not a distributed ACID transaction. If the compensating delete also fails, the application logs the failure for operational follow-up; the registration request still returns a generic server error.

The admin account-status endpoint `PATCH /api/v1/admin/users/{userId}/status` uses the Keycloak Admin API to enable or disable an identity and mirrors the result in the application profile table. The client first reads the full Keycloak user representation and then updates its `enabled` field, preserving other user attributes. The service updates Keycloak first and persists the application status second; if persistence fails, it attempts a compensating Keycloak update to the previous status. A failed compensation requires operational reconciliation. Because the resource server validates self-contained JWTs locally, a previously issued access token can remain usable until its expiry even after the Keycloak account is disabled.\n\nThe current imported realm has `verifyEmail: false`. Email verification is therefore not enforced in this learning environment. Before production, configure SMTP and enable Keycloak email verification; retain `emailVerified=false` for newly registered accounts.

### Local backend environment

The Spring Boot process does not automatically load the Compose .env file. After copying the generated service-account secret into the ignored .env, export it in the shell that starts the backend:

Bash:
```bash
export KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET='your-generated-client-secret'
export KEYCLOAK_ADMIN_BASE_URL=http://localhost:8081
export KEYCLOAK_ADMIN_REALM=platform
```

PowerShell:
```powershell
$env:KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET = "your-generated-client-secret"
$env:KEYCLOAK_ADMIN_BASE_URL = "http://localhost:8081"
$env:KEYCLOAK_ADMIN_REALM = "platform"
```

When the backend itself runs inside the Compose network, set KEYCLOAK_ADMIN_BASE_URL=http://keycloak:8080. The issuer/token URL must also be reachable from the backend and must match the realm issuer used by the Keycloak instance.

Never log the client secret or access token, and never send them to Swagger UI or a browser.

## Security notes

- Never expose the confidential service-account secret to Swagger UI or a browser.
- The Swagger client is public: do not add a client secret to application configuration or browser-facing Swagger settings.
- Do not enable Direct Access Grants (password grant) for these clients.
- The realm import contains no user passwords, bootstrap admin credentials, or client secrets.
- The bootstrap administrator is a local Keycloak server administrator, separate from the platform's application-level `ADMIN` client role.
- Full Scope Allowed is disabled on all three clients. The Swagger client's dedicated scope must explicitly include only the `platform-api` client roles required by the API.
