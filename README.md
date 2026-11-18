# crm-demo-core-service

TODO: what this service does.

| | |
|---|---|
| Port | 8081 |
| Stack | Java 25, Spring Boot 4.1, Gradle 9.9 (wrapper), PostgreSQL, Flyway, JUnit, Testcontainers, Spring Security (JWT), springdoc, Spotless, JaCoCo, Lombok, SLF4J (Logback), Jib |

## Prerequisites

- A JDK 17+ to run Gradle. The Java 25 toolchain is downloaded automatically by Gradle (foojay resolver)
  if you don't have it.
- In your IDE, the Lombok plugin with annotation processing enabled.
- For the feature workflow in Claude Code: Node.js 22.12+ and OpenSpec (`npm install -g @fission-ai/openspec@latest`).
- Docker running, for the local database and the Testcontainers tests.

## Run locally

```bash
./gradlew bootRun
```

This uses the `local` profile. Requests don't need a token to get past the security filters, so Swagger UI is at
<http://localhost:8081/swagger-ui.html>, but CRM endpoints still check permissions and the user's UMS status, so
calls to them need a token from the local Keycloak (`crm-internal` realm, audience `crm-api`, for example signed in
through the CRM UI). `compose.yaml` only starts PostgreSQL; the local profile also expects, from `local-docker` and the
UMS repo, Kafka on `localhost:9092`, Keycloak on `localhost:8180` and the UMS on `localhost:8083`.

## Environment variables

Needed everywhere except the `local` profile.

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL of the database |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_ISSUER_URI` | Issuer whose JWTs this service accepts: the `crm-internal` realm, e.g. `https://keycloak.example/realms/crm-internal` |
| `KEYCLOAK_BASE_URL` | Keycloak address, for the client-credentials token endpoint |
| `KEYCLOAK_CORE_SERVICE_SECRET` | Secret of the `core-service` client in `crm-internal` |
| `KEYCLOAK_CRM_PROMOTER_SECRET` | Secret of the `crm-promoter` client in `crm-internal` (only for promoting portal users) |
| `UMS_BASE_URL` | Address of the User Management Service |
| `CRM_DEFAULT_TEAM_ID` | UMS team id that gets portal submissions no routing rule matches |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers |
| `CRM_CORS_ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call the API (the CRM UI) |
| `CRM_SCOPE_CACHE_TTL` | Optional; how long team scope is cached for reads (default and maximum `5m`) |
| `CRM_CATALOG_RETRY_INTERVAL` | Optional; wait before retrying a failed catalog registration (default `30s`) |
| `CRM_OUTBOX_RELAY_INTERVAL` | Optional; how often outbox events are published (default `1s`) |

## Test and format

```bash
./gradlew build
```

The build runs the tests, writes a coverage report to `build/reports/jacoco/test/html/index.html`, and fails if
the code isn't formatted. To format it:

```bash
./gradlew spotlessApply
```

## Build a container (Jib)

```bash
./gradlew jibDockerBuild
```

To push to a registry instead:

```bash
IMAGE_REGISTRY=ghcr.io/my-org ./gradlew jib
```

## Contributing

Package layout and conventions are in `CLAUDE.md` and `.claude/rules/`.
