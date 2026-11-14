# Architecture Decisions

This document records approved high-level decisions. Detailed design and trade-offs will be added before the corresponding implementation milestone.

## ADR-001: Modular monolith

**Decision:** Build one Spring Boot backend organized into cohesive modules.

**Reason:** The project is intended to teach production-oriented backend architecture without introducing unnecessary distributed-system complexity.

## ADR-002: Separate application and identity databases

**Decision:** Use independent PostgreSQL 16 services/databases for the application and Keycloak.

**Reason:** Keycloak owns identity data and credentials; the backend owns application-specific data. The application must not store passwords or couple itself to Keycloak's internal schema.

## ADR-003: Keycloak client roles

**Decision:** Use the client roles `ADMIN`, `STAFF`, and `USER` for the backend application.

**Reason:** These roles describe authorization within this application.

## ADR-004: Controlled privileged-user provisioning

**Decision:** Public registration assigns only `USER`. An authenticated `ADMIN` can create `STAFF` and `ADMIN` accounts through a separately protected API. The first `ADMIN` is provisioned through a one-time, non-public bootstrap process.

**Reason:** A public request must never be able to grant itself privileged access. Bootstrap credentials and operations must not be exposed through public HTTP endpoints.

## ADR-005: Separate Keycloak clients by trust boundary

**Decision:** Use distinct clients for backend resource-server identity, Swagger UI login, and confidential administrative service access.

**Reason:** Browser-facing OAuth clients must not receive administrative credentials. Administrative access will use a dedicated service account with least-privilege permissions.

## ADR-006: Swagger OAuth flow

**Decision:** Swagger UI uses OAuth 2.0 Authorization Code flow with PKCE (S256), subject to end-to-end verification against the selected Springdoc and Keycloak versions.

**Reason:** This provides interactive login without manually copying access tokens and avoids embedding a client secret in browser code.
