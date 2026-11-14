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
    ├── api/                # ADMIN-only user directory and privileged provisioning endpoints
    └── application/        # User listing and STAFF/ADMIN account provisioning workflows
```

## Rules

- Organize by business capability, not by global technical layers such as `controller/`, `service/`, and `repository/`.
- Keep domain rules inside the owning feature; avoid cross-feature entity access.
- Share code only when it is genuinely generic and stable.
- Keycloak credentials remain in Keycloak. The `user` feature stores application profile data and the Keycloak subject identifier only.
- The current-user read and update endpoints derive identity exclusively from the validated JWT subject; they do not accept a user ID from the request.
- Self-service profile updates are limited to full name and mobile number. Email and credentials remain managed by Keycloak.
- Administrative user listing, provisioning, and account status management are protected by `ROLE_ADMIN`. The directory is paginated with a bounded page size; provisioning accepts only `STAFF` and `ADMIN`. Public registration remains hard-coded to `USER`.
