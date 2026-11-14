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
