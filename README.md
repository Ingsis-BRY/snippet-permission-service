# Snippet Permission Service

Permission service for **Snippet Searcher**. It owns the relationship between users and
snippets and answers a single question for the rest of the platform: *can this user perform
this action on this snippet?*

It does not store snippet content or metadata. That belongs to `snippet-service`.

## Responsibility

| User story | What this service provides |
| --- | --- |
| #1, #3 Create snippet | Registers the creator as `OWNER` |
| #2, #4 Update snippet | Only the owner is allowed to `WRITE` |
| #5 List snippets | Snippet ids by relationship: owned, shared or all |
| #6, #13 View / download | `OWNER` and `READER` are allowed to `READ` |
| #7 Share snippet | Only the owner can share; the target becomes `READER` |
| #8 Create tests | Only the owner (`WRITE`) |
| #9 Run tests | `OWNER` and `READER` (`READ`) |
| Delete snippet | Only the owner; removes every permission of the snippet |

The user search required by #7 ("filter users while typing") depends on Auth0 and is not
implemented yet.

## Roles and actions

| Role | `READ` | `WRITE` | `SHARE` | `DELETE` |
| --- | :---: | :---: | :---: | :---: |
| `OWNER` | ✔ | ✔ | ✔ | ✔ |
| `READER` | ✔ | | | |

The rule lives in the domain (`PermissionRole`), not in controllers.

## API

Base path: `/permissions`. Every request needs an Auth0 access token in
`Authorization: Bearer <token>`. snippet-service forwards the token of the user it is acting
for. **The caller is always the token's `sub`**: nobody can register, share or ask on behalf of
another user.

| Method | Path | Scope | Description |
| --- | --- | --- | --- |
| `POST` | `/owner` | `write:snippets` | Makes the caller the owner of a new snippet (body `{"snippetId"}`) |
| `POST` | `/share` | `write:snippets` | Shares a snippet (the caller must be the owner) |
| `DELETE` | `/share/{snippetId}/{userId}` | `write:snippets` | Revokes a share |
| `GET` | `/check?snippetId&action` | `read:snippets` | Whether the caller can perform the action |
| `GET` | `/snippets?relationship` | `read:snippets` | Snippet ids the caller can access |
| `DELETE` | `/snippets/{snippetId}` | `write:snippets` | Removes every permission of a snippet (owner only) |

`relationship` accepts `OWNER`, `SHARED` or `ALL` (default).
`action` accepts `READ`, `WRITE`, `SHARE` or `DELETE`.

### Examples

```bash
TOKEN=<access token of the caller>

curl -X POST localhost:8080/permissions/owner \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"snippetId":"<uuid>"}'

curl -X POST localhost:8080/permissions/share \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"snippetId":"<uuid>","targetUserId":"auth0|<other user>"}'

curl -H "Authorization: Bearer $TOKEN" \
  "localhost:8080/permissions/check?snippetId=<uuid>&action=READ"
# {"allowed":true}

curl -H "Authorization: Bearer $TOKEN" \
  "localhost:8080/permissions/snippets?relationship=SHARED"
# {"snippetIds":["<uuid>"]}
```

### Errors

Errors are returned as `{"message": "..."}`, except `401` and `403` from token validation,
which have no body and a `WWW-Authenticate` header.

| Status | When |
| --- | --- |
| `400` | Token without `sub`, sharing with yourself, revoking the owner, invalid enum |
| `401` | Missing token, or a token with a bad signature, issuer or audience, or expired |
| `403` | The token lacks the scope, or the caller is not allowed to perform the action |
| `404` | Revoking a share that does not exist |
| `409` | The snippet already has an owner, or it is already shared with that user |

## Architecture

Hexagonal layout, following the same structure as `snippet-service`:

```text
com.ingsisbry.permission
├── domain/          SnippetPermission, PermissionRole, SnippetAction
├── application/
│   ├── command/     grant, share, revoke, delete
│   ├── query/       check, list
│   ├── common/      PermissionAuthorizer, exceptions
│   └── port/out/    PermissionRepository, PermissionQueryRepository
└── adapter/
    ├── web/         controller, DTOs, exception handler
    ├── persistence/ JPA entity, repositories and adapters
    └── configuration/
```

Commands (writes) and queries (reads) use separate ports, the same split used in
`snippet-service`.

### Persistence

PostgreSQL 17, single table:

```text
snippet_permissions
  id          uuid        primary key
  snippet_id  uuid        not null
  user_id     varchar     not null
  role        varchar     not null   (OWNER | READER)
  created_at  timestamptz not null

  unique (snippet_id, user_id)
  index  (user_id)
```

## Build tooling

Build and quality configuration come from
[`gradle-conventions`](https://github.com/Ingsis-BRY/gradle-conventions), so this repository
does not copy Gradle, Detekt, Ktlint or JaCoCo setup:

```kotlin
plugins {
    id("com.ingsisbry.kotlin-quality") version "2.0.0"
    id("com.ingsisbry.spring-boot-application") version "2.0.0"
    id("com.ingsisbry.jpa") version "2.0.0"
}
```

## Running locally

### 1. GitHub Packages credentials

The convention plugins are resolved from GitHub Packages, which requires a **classic** token
with the `read:packages` scope. Add it to `~/.gradle/gradle.properties` (never to this
repository):

```properties
gpr.user=<github-username>
gpr.key=<token>
```

On Windows, prefer `gpr.user` over the `USERNAME` environment variable: Windows already defines
`USERNAME` as the OS user name.

### 2. Build and test

```bash
./gradlew check
```

Runs compilation, Detekt, Ktlint and every test.

### 3. Docker

```bash
cp .env.example .env
export USERNAME=<github-username> TOKEN=<token>
docker compose up -d --build
```

PowerShell:

```powershell
$env:USERNAME="<github-username>"; $env:TOKEN="<token>"
docker compose up -d --build
```

The service listens on `http://localhost:8080` and uses its own database container,
`snippet-permission-db`.

| Variable | Description |
| --- | --- |
| `PERMISSION_SERVICE_PORT` | Host port mapped to the container port `8080` |
| `POSTGRES_DB` | Database name |
| `POSTGRES_USER` | Database user |
| `POSTGRES_PASSWORD` | Database password |
| `DB_HOST` | Database host (set by compose, defaults to `localhost`) |
| `DB_PORT` | Database port (defaults to `5432`) |
| `AUTH0_DOMAIN` | Auth0 tenant domain, e.g. `snippet-searcher-bry.us.auth0.com` |
| `AUTH0_AUDIENCE` | Identifier of the Auth0 API, `https://snippet-searcher-api` |

The service does not start without `AUTH0_DOMAIN` and `AUTH0_AUDIENCE`. The token's signing
keys are downloaded from the tenant on the first request, not at startup.

## Continuous integration

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `CI.yml` | Pull request or push to `main` | Runs `./gradlew check` |
| `publish-image.yml` | Push to `main` | Runs `./gradlew check`; only if it passes, builds and pushes `ghcr.io/ingsis-bry/snippet-permission-service` tagged `latest` and with the commit SHA |

Both workflows need these repository (or organization) secrets to resolve the convention
plugins:

| Secret | Value |
| --- | --- |
| `GH_PACKAGES_USERNAME` | GitHub user that owns the token |
| `GH_PACKAGES_TOKEN` | Classic token with `read:packages` |

The image is pushed with the workflow's own `GITHUB_TOKEN`, so the package must allow write
access from this repository.

## Deployment

The service is deployed in the Docker Swarm stack defined in
[`snippet-searcher-infra`](https://github.com/Ingsis-BRY/snippet-searcher-infra), next to
`snippet-service`:

| Service | Image | Notes |
| --- | --- | --- |
| `snippet-permission-service` | `$PERMISSION_SERVICE_IMAGE` | Internal only, no published port |
| `snippet-permission-db` | `postgres:17` | Own volume, pinned to the manager node |

Inside the `snippet-network` overlay network other services reach it at
`http://snippet-permission-service:8080`. The stack provides the database through
`PERMISSION_POSTGRES_DB`, `PERMISSION_POSTGRES_USER` and `PERMISSION_POSTGRES_PASSWORD`,
mapped to the `POSTGRES_*` variables the service reads.

The package in GHCR is public, so Swarm nodes pull the image without logging in.

### Operations

| Concern | How |
| --- | --- |
| Health | `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, without a token and without details. No other actuator endpoint is exposed. The runtime image has `curl` for the Swarm healthcheck. |
| Logs | Set `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` for ECS JSON logs on stdout (Spring Boot structured logging). |
| Tracing | Micrometer Tracing with Brave, no exporter, sampling `1.0`. A W3C `traceparent` from snippet-service is continued, and `traceId`/`spanId` go to the MDC and the ECS logs. |
| New Relic | The image ships the agent at `/app/newrelic/newrelic.jar` and starts with it. |

## Tests

| Suite | Scope |
| --- | --- |
| `PermissionRoleTest`, `SnippetPermissionTest` | Domain rules |
| `PermissionUseCasesTest` | Use cases against an in-memory repository |
| `JpaPermissionPersistenceTest` | JPA adapters, unique constraint, relationship queries |
| `PermissionControllerIntegrationTest` | HTTP endpoints, status codes, `401` and scopes, with mock JWTs |

Persistence and integration tests run against H2 in PostgreSQL mode, so `./gradlew check`
does not need Docker.

## Design decisions

- **Separate service with its own database.** Authorization data changes for different
  reasons than snippet content, and the reference architecture of the course defines
  `permission-service` and `permission-db` as independent containers.
- **Two roles instead of per-action grants.** The user stories only describe owners and users
  a snippet was shared with, who get read access to the snippet and its tests. Actions are
  modelled explicitly so a new role can be added without touching callers.
- **The service stores relations, not snippets.** Listing (#5) returns snippet ids; filtering,
  sorting and pagination stay in `snippet-service`.
- **Unknown permission means denied.** A user with no relation to a snippet receives `403`
  instead of `404`, so the service does not reveal which snippets exist.
- **Every service validates the token.** snippet-service forwards the user's access token
  instead of passing a user id, and this service validates its signature, issuer, audience and
  expiry again. A request that reached it without going through snippet-service still needs a
  valid token.
- **The caller comes from the token, never from the request.** `sub` is the owner on `/owner`,
  the requester on share, revoke and delete, and the user on `/check` and `/snippets`. Earlier
  versions took the user from an `X-User-Id` header or a `userId` parameter, which anyone could
  forge.
- **Scopes for the API, permissions for the snippet.** `read:snippets` and `write:snippets`
  (granted through the Auth0 role `snippet-user`) say what the caller can do with the API;
  the stored relations say which snippets. It is the "RBAC for the app, ACL for the resources"
  split from the course.
- **Database constraint as the last line of defence.** Use cases check for duplicates, and the
  `unique (snippet_id, user_id)` constraint covers concurrent requests. Its violation is
  translated to `409` instead of `500`.
- **Line length aligned to 100.** The shared Detekt configuration limits lines to 100
  characters while the shared `.editorconfig` sets Ktlint to 140, which makes both tools
  disagree. This repository overrides `.editorconfig` to 100 until the convention is fixed.
- **LF line endings enforced** through `.gitattributes`, so `gradlew` keeps working inside the
  Linux Docker image when the repository is cloned on Windows.

## Pending

- The user search for #7 lives in snippet-service (Auth0 Management API), not here.
