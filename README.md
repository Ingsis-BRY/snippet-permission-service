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

Base path: `/permissions`. Until Auth0 is integrated, the requesting user is sent in the
`X-User-Id` header.

| Method | Path | Caller | Description |
| --- | --- | --- | --- |
| `POST` | `/owner` | snippet-service | Registers the owner of a new snippet |
| `POST` | `/share` | UI | Shares a snippet (requester must be the owner) |
| `DELETE` | `/share/{snippetId}/{userId}` | UI | Revokes a share |
| `GET` | `/check?snippetId&userId&action` | snippet-service | Returns whether the action is allowed |
| `GET` | `/snippets?userId&relationship` | snippet-service | Snippet ids accessible by a user |
| `DELETE` | `/snippets/{snippetId}` | snippet-service | Removes every permission of a snippet |

`relationship` accepts `OWNER`, `SHARED` or `ALL` (default).
`action` accepts `READ`, `WRITE`, `SHARE` or `DELETE`.

### Examples

```bash
curl -X POST localhost:8080/permissions/owner \
  -H "Content-Type: application/json" \
  -d '{"snippetId":"<uuid>","userId":"franco"}'

curl -X POST localhost:8080/permissions/share \
  -H "Content-Type: application/json" -H "X-User-Id: franco" \
  -d '{"snippetId":"<uuid>","targetUserId":"enzo"}'

curl "localhost:8080/permissions/check?snippetId=<uuid>&userId=enzo&action=READ"
# {"allowed":true}

curl "localhost:8080/permissions/snippets?userId=enzo&relationship=SHARED"
# {"snippetIds":["<uuid>"]}
```

### Errors

Errors are returned as `{"message": "..."}`.

| Status | When |
| --- | --- |
| `400` | Blank user, sharing with yourself, revoking the owner, missing header, invalid enum |
| `403` | The requester is not allowed to perform the action |
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

## Tests

| Suite | Scope |
| --- | --- |
| `PermissionRoleTest`, `SnippetPermissionTest` | Domain rules |
| `PermissionUseCasesTest` | Use cases against an in-memory repository |
| `JpaPermissionPersistenceTest` | JPA adapters, unique constraint, relationship queries |
| `PermissionControllerIntegrationTest` | HTTP endpoints and status codes |

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
- **`X-User-Id` header as a temporary identity.** It will be replaced by the `sub` claim of the
  Auth0 JWT without changing the domain or the use cases.
- **Database constraint as the last line of defence.** Use cases check for duplicates, and the
  `unique (snippet_id, user_id)` constraint covers concurrent requests. Its violation is
  translated to `409` instead of `500`.
- **Line length aligned to 100.** The shared Detekt configuration limits lines to 100
  characters while the shared `.editorconfig` sets Ktlint to 140, which makes both tools
  disagree. This repository overrides `.editorconfig` to 100 until the convention is fixed.
- **LF line endings enforced** through `.gitattributes`, so `gradlew` keeps working inside the
  Linux Docker image when the repository is cloned on Windows.

## Pending

- Integration with `snippet-service` (owner registration on create, permission checks, listing).
- Auth0: JWT validation, user identity and user search for #7.
- Deployment in the Docker Swarm stack of `snippet-searcher-infra`.
