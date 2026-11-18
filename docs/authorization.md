# Authorization

Every CRM API call answers three questions: who is calling, what they may do, and which records they may see.

## 1. Who: the token

- Only access tokens from the Keycloak realm **`crm-internal`** (`JWT_ISSUER_URI`) are accepted, and only if their
  audience includes **`crm-api`**. Tokens from `crm-admin` or `portal`, expired tokens and tokens for another
  audience get **401**. The decoder fetches the realm's keys on first use.
- The token's `crm-api` client roles (`resource_access.crm-api.roles`) become the caller's authorities, unprefixed:
  `submissions:read`, `ui:team-dashboard`. Roles in the UMS are composite client roles, so the token already holds
  the effective permissions.
- The caller's UMS account must be **ACTIVE**. Every request to an endpoint is checked against the user's team
  scope from the UMS: reads use a copy cached for at most 5 minutes, writes (anything but GET, HEAD and OPTIONS) ask
  the UMS live. A disabled user, or one the UMS doesn't know, gets **403** even with a valid token. If the UMS can't
  answer, writes get **503** (fail closed); reads use the cached copy if there is one.

## 2. What: permissions

- Every endpoint declares `@RequiresPermission("<resource>:read|write", description, sensitive)`. A caller without it
  gets **403**. A `<resource>:write` permission also grants `<resource>:read`.
- The build fails when an endpoint declares nothing (`EndpointPermissionArchitectureTests`).
- The one exception is `GET /me/capabilities`, marked `@AnyActiveInternalUser`: any ACTIVE internal user may call it.
  Only allowlisted handlers (`DeclaredPermissions.ANY_ACTIVE_USER_ALLOWLIST`) may use the marker; anywhere else the
  build fails.
- At startup the declared permissions are registered with the UMS catalog (`PUT /internal/catalog/core-service`),
  with their description and sensitive flag; a failed registration is logged and retried every 30 seconds.
- `PermissionMatrixTests` calls every endpoint as callers with no permissions, the exact permission, the resource's
  `:write`, another resource's permissions, and a disabled user.

## 3. Which records: team scope

Team-owned records are checked through `TeamAccessService`, on top of the `AccessPolicy` rules:

| | Member of the record's team | Super Manager not in the team | Other users |
|---|---|---|---|
| Read one record | allowed | allowed, audited as `CROSS_TEAM_READ` | **404** (existence isn't revealed) |
| List | only their teams' records | all teams | only their teams' records |
| Create, change, delete | allowed (live scope) | **403** | **403** |
| Manager-only views | team managers only | allowed | **403** |

List queries take the team ids from `TeamAccessService.readableTeams()` (`WHERE team_id IN (:teams)`, or no filter
for all teams).

The cached scope of a user is evicted when any `ums-user-events` message for them arrives (for example
`TeamMembershipChanged` or `UserDisabled`); each instance consumes that topic in a consumer group of its own.

## 4. Capabilities for the UI

`GET /me/capabilities` returns the caller's `sub`, name, effective permissions (`:write` implying `:read`), UI
features (`ui:*` without the prefix), member and managed teams, and `allTeams`. It is built from the live team scope.
It is a hint for hiding UI; every API call is still checked.

```json
{"sub":"…","name":"Ana Test","permissions":["submissions:read","submissions:write"],
 "uiFeatures":["team-dashboard"],"memberTeams":["…"],"managedTeams":["…"],"allTeams":false}
```

## 5. Audit

Denials (missing permission, inactive user, team edit or manage refused) and Super Manager cross-team reads are
logged and published to **`audit-events`** through the transactional outbox, in the common audit schema plus
`permission`, `path` and `teamId`:

```json
{"eventId":"…","schemaVersion":1,"occurredAt":"…","source":"CORE","realm":"crm-internal","actorId":"<sub>",
 "actorType":"USER","action":"AUTHORIZATION_DENIED","targetType":"ENDPOINT","targetId":"POST /…","outcome":"DENIED",
 "permission":"submissions:write","path":"POST /…","ipAddress":"…","correlationId":"…","reason":"missing permission"}
```

Requests without a valid token are 401s, not authorization denials, and are only recorded in `audit.api_audit`.

## UMS calls

core-service calls the UMS as the `core-service` client of `crm-internal` (client credentials, scopes
`ums:team-scope` and `ums:catalog`): `GET /internal/users/{sub}/team-scope` and `PUT /internal/catalog/core-service`.
