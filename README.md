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

This uses the `local` profile, which turns off authentication. Swagger UI is at
<http://localhost:8081/swagger-ui.html>.

## Environment variables

Needed everywhere except the `local` profile.

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL of the database |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_ISSUER_URI` | Issuer whose JWTs this service accepts |

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
