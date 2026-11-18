# Design

## Context

Fresh scaffold with JWT security. The architecture calls for a shared authorization library; for now it lives in this repo's `config`/`service`/`processors` packages behind interfaces, to be extracted into a starter once a second CRM service exists. See proposal.md.

## Decisions

- **`@RequiresPermission("submissions:read")`** on controller methods, enforced by a `PermissionEvaluator`-backed `@PreAuthorize` meta-annotation. A JUnit architecture test (scans `RequestMappingHandlerMapping` in a Spring context) fails the build for any handler without it. The HTTP method is only a default hint in the failure message.
- **`JwtAuthenticationConverter`** maps `resource_access.crm-api.roles` to authorities; issuer `crm-internal`, audience validator `crm-api`.
- **`AccessPolicy` interface** with `canView(user, teamId)`, `canEdit(user, teamId)`, `canManage(user, teamId)` and `scopeFilter(user)`. Repositories take the team-id set from `scopeFilter` in every list query (`WHERE team_id IN (:teams)`, or no filter for all-teams reads). Alternative: Hibernate filters — implicit and easy to bypass. Internals can later move to OpenFGA behind the same interface.
- **Not found over forbidden** for out-of-scope reads, so record existence isn't leaked.
- **UMS client** (`client` package, `RestClient`) with client-credentials token for `core-service` client in `crm-internal`; team scope cached with Caffeine (TTL 5 min), evicted by a `TeamMembershipChanged` listener on `ums-user-events`. Status comes from the same team-scope response.
- **Audit and denial logging** via a Spring Security `AuthorizationDeniedEvent` listener → outbox → `audit-events`.
- **Catalog registration** on `ApplicationReadyEvent`, built from the annotations; failure is logged and retried, not fatal.

## Risks / Trade-offs

- [UMS down → no scope] → reads use cache; writes fail closed with 503.
- [Library kept in-repo] → keep it in its own packages with no CRM domain imports so extraction is mechanical.

## Implementation decisions

Made while applying the change; the specs are unchanged unless stated.

- **Own `JwtDecoder` bean** in `SecurityConfig` instead of Boot's `issuer-uri` auto-configuration: it looks the
  issuer's keys up lazily and validates expiry, issuer (`JWT_ISSUER_URI`, the `crm-internal` realm) and audience
  `crm-api`. The validator is a static method so tests can decode test-signed tokens with exactly the production
  rules. Spring Security 7 also adds a `FACTOR_BEARER` authority to every token; it is harmless.
- **Permission annotation** `@RequiresPermission(value, description, sensitive)`. Endpoints sharing a permission must
  agree on description and sensitivity; the catalog build and the architecture test fail otherwise.
- **UMS contracts** (JSON shapes aren't fixed by the UMS specs): team scope
  `{sub, status, memberTeams[uuid], managedTeams[uuid], allTeams}`; catalog `PUT {permissions:[{key, realm,
  description, sensitive}]}`. A 404 from team scope means "not an internal user" and is treated as not ACTIVE.
- **Cache eviction** happens for any `ums-user-events` message that carries a `sub` (not only
  `TeamMembershipChanged`), so a `UserDisabled` also takes effect on reads at once. Writes always use live scope.
- **Real-token tests** use Keycloak Testcontainers with the generated test realms plus a test-only setup step
  (`IntegrationTestContainers`) that creates `crm-api` client roles, gives them to `test-employee@corp.example` and adds
  a `crm-api` audience mapper to `test-client`.
- **ACTIVE check** is a Spring MVC `HandlerInterceptor` (`ActiveUserInterceptor`) for every endpoint: cached status
  for GET/HEAD/OPTIONS, live status for other methods. It runs inside the dispatcher, so "UMS down" becomes a 503
  problem detail. Inactive users get 403 and the denial is audited.
- **Team checks for record-owning services** go through `TeamAccessService` (`readableTeams`, `checkView` → 404 and
  cross-team-read audit, `checkEdit` with live scope → 403, `checkManage` → 403), built on the pure `AccessPolicy`.
- **Audit events** follow the UMS's common audit schema (`source` CORE, `realm` crm-internal, `actorType` USER) plus
  `permission`, `path` (`METHOD /uri`) and `teamId`. Actions `AUTHORIZATION_DENIED` (outcome DENIED) and
  `CROSS_TEAM_READ` (outcome SUCCESS). Each is written to the outbox in a transaction of its own; a failed audit write is
  logged and never changes the response. Method-security denials arrive as `AuthorizationDeniedEvent`s; requests
  without a token are 401s and aren't audited as denials.
- **Outbox** is the UMS's (relay with per-event transactions, parking after 5 failures), except that a parked event is
  logged with the `ALERT` marker instead of going through an alert service.
- **CORS** (added at the developer's request, beyond the specs): browsers may call the API from
  `CRM_CORS_ALLOWED_ORIGINS` (local: `http://localhost:5173`, the CRM UI), with the `Authorization`, `Content-Type`
  and `X-Correlation-Id` headers.
- **`GET /me/capabilities` has no permission** (decided with the developer's session; the developer may overrule):
  it carries `@AnyActiveInternalUser`, a named marker the architecture test accepts only on allowlisted handlers
  (just this one), so it can't become a way round `@RequiresPermission`. The issuer, audience and ACTIVE checks still
  apply, and the response is built from the live team scope, so a disabled user gets 403 even while their cached
  scope says ACTIVE. It isn't registered in the catalog. *Rejected alternative:* a catalog permission such as
  `profile:read` that every CRM role would have to include, which needs every UMS role to grant it and adds a way to
  lock a user out of the UI by mistake. The api-authorization spec's "Permission per endpoint" requirement names the
  exception.
- **Code review follow-ups.** Scope fetches run inside the cache's per-key compute, so an eviction during a fetch
  can't be overwritten by the stale result. Only ACTIVE scopes are cached, because the UMS publishes no event when a
  user becomes ACTIVE or is re-enabled. The outbox relay also counts send errors thrown before Kafka answers.
- **`local` profile**: requests pass the filter chain without a token, but a token is still validated and used, and
  CRM endpoints answer 401 without one. Making `local` work entirely without Keycloak would need a fake user that
  bypasses permissions and the UMS; that's left for the developer to decide.
- **`outbox_event` has no CRUD controller**: it is infrastructure, as in the UMS (the rule in
  `service-architecture.md` only exempts the audit table).
- **Not done, for the developer:** nothing registers `ui:*` features in the UMS catalog (the specs don't say which
  service owns them); the scope listener's per-instance consumer group (`${random.uuid}`) leaves an idle group on the
  broker after each restart until Kafka's offset retention removes it.
- **Permission check before binding** (found by the permission matrix while applying add-submission-intake): Spring
  MVC validates `@Valid` request bodies before the `@PreAuthorize` proxy runs, so a caller without the permission got
  a 400 describing the body instead of 403. `PermissionInterceptor` now checks `@RequiresPermission` first (and audits
  the denial); `@PreAuthorize` stays as a second check.
