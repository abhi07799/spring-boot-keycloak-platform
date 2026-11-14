# Backend Package Structure

The application uses a package-by-feature layout inside one Spring Boot deployable. Each business capability owns its API, application, domain, and infrastructure code as it is introduced.

```text
io.github.abhi07799.platform
├── PlatformApplication.java
├── common/                 # Shared technical primitives; keep this small
├── system/
│   └── api/                # Operational endpoints (for example, ping)
├── identity/
│   ├── api/                # Registration endpoint and request/response contracts
│   ├── application/        # Registration use case and orchestration
│   └── infrastructure/     # Keycloak Admin REST API integration
├── user/
│   ├── api/                # Authenticated profile endpoint and response contract
│   ├── application/        # Profile lookup use case
│   └── infrastructure/
│       └── persistence/    # Application profile entity and repository
└── administration/
    ├── api/                # ADMIN-only privileged user provisioning endpoint
    └── application/        # STAFF/ADMIN account provisioning workflow
```

## Rules

- Organize by business capability, not by global technical layers such as `controller/`, `service/`, and `repository/`.
- Keep domain rules inside the owning feature; avoid cross-feature entity access.
- Share code only when it is genuinely generic and stable.
- Keycloak credentials remain in Keycloak. The `user` feature stores application profile data and the Keycloak subject identifier only.
- The current-user endpoint derives identity exclusively from the validated JWT subject; it does not accept a user ID from the request.
- Administrative provisioning is protected by `ROLE_ADMIN`; its request role is an enum restricted to `STAFF` and `ADMIN`. Public registration remains hard-coded to `USER`.
