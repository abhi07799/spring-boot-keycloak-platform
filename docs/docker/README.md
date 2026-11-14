# Local Infrastructure

This milestone starts the two independent PostgreSQL 16 instances and Keycloak 26.8.2. Keycloak imports the project realm definition on first startup.

## Prerequisites

- Docker Engine or Docker Desktop
- Docker Compose v2

## Configure local credentials

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` and replace every placeholder with strong, unique local-development passwords. The application database, Keycloak database, and Keycloak bootstrap administrator must not share passwords.

The `.env` file is ignored by Git. Never commit it or paste its contents into issues or pull requests.

## Start the infrastructure

From the repository root:

```bash
docker compose up -d
```

Check container status:

```bash
docker compose ps
```

Follow Keycloak startup logs:

```bash
docker compose logs -f keycloak
```

When Keycloak is healthy, open:

- Keycloak: http://localhost:8081
- Local application PostgreSQL: localhost:5433
- Local Keycloak PostgreSQL: localhost:5434

The PostgreSQL ports are bound to loopback, so they are not exposed on external network interfaces. Containers communicate over private Compose bridge networks using service DNS names. The Keycloak database is only attached to `keycloak-network`; the application database is only attached to `app-network`. The future Spring Boot service will join both networks.

## Realm import

The file `infra/keycloak/realm/platform-realm.json` is mounted read-only into Keycloak's import directory. The `--import-realm` option imports the `platform` realm when it does not already exist.

Realm import is not an ongoing synchronization mechanism: changing the JSON file does not overwrite an existing realm. For a clean local re-import, stop the stack and remove the Keycloak database volume only if you are willing to lose all Keycloak data:

```bash
docker compose down
docker volume ls
```

Identify the project Keycloak volume before removing it. Do not remove the application database volume. Then start the stack again.

## Stop without deleting data

```bash
docker compose down
```

Named volumes persist across container recreation and ordinary `docker compose down`.

**Destructive reset:** `docker compose down -v` deletes both database volumes and all data stored in them. Use it only when you intentionally want a clean reset of both databases.

## Important development-only note

Keycloak currently runs with `start-dev` to support local development over HTTP. This mode is not a production deployment configuration. Production hardening, TLS/hostname configuration, and secrets management must be addressed before deployment.

## Health checks

Both PostgreSQL containers use `pg_isready`. Keycloak exposes its readiness endpoint on its management port, and Compose checks that endpoint before reporting the service healthy. Keycloak waits for its database health check before starting.
