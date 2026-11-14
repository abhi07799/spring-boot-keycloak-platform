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
├── user/                   # Application user profile and registration
└── administration/         # Privileged user provisioning and admin use cases
```

## Rules

- Organize by business capability, not by global technical layers such as `controller/`, `service/`, and `repository/`.
- Keep domain rules inside the owning feature; avoid cross-feature entity access.
- Share code only when it is genuinely generic and stable.
- Keycloak credentials remain in Keycloak. The `user` feature will store application profile data and the Keycloak subject identifier only.
- The skeleton does not add security or persistence configuration yet; those are separate implementation milestones.
