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
