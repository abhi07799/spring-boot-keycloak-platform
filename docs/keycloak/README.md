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

Swagger's local redirect URI is `http://localhost:8080/swagger-ui/oauth2-redirect.html`. Update the redirect URI and web origin when the API is hosted at a different origin. Do not use wildcard redirect URIs.

The Swagger client has an explicit audience mapper for `platform-api`, so its access tokens can carry the API audience claim. The API client roles are emitted under `resource_access.platform-api.roles` when the corresponding role mappings are included in the token.

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

## Security notes

- Never expose the confidential service-account secret to Swagger UI or a browser.
- Do not enable Direct Access Grants (password grant) for these clients.
- The realm import contains no user passwords, bootstrap admin credentials, or client secrets.
- The bootstrap administrator is a local Keycloak server administrator, separate from the platform's application-level `ADMIN` client role.
- The initial client definitions retain Keycloak's default full-scope role behavior for the initial local integration. Before production use, disable Full Scope Allowed and explicitly configure role scope mappings, then verify the effective token with Keycloak's client-scope evaluation.
