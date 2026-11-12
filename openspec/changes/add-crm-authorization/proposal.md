# Proposal

## Why

CRM APIs must answer three questions on every request: who are you (Keycloak `crm-internal`), what may you do (permissions in the token), and which records (team scope, checked on the server). Without a shared, deny-by-default mechanism each endpoint would reinvent it.

## What Changes

- Resource server that trusts only `crm-internal` tokens with audience `crm-api`; `crm-api` client roles become authorities.
- An authorization module (to be extracted later as a shared Spring starter) with a permission annotation per endpoint, a build-time check that every endpoint declares one, and `canView` / `canEdit` team-scope checks.
- Team scope resolved from the UMS and cached, cleared by `TeamMembershipChanged` events.
- Live status check: only ACTIVE users get access; writes re-check live membership.
- Catalog registration of this service's permissions with the UMS at startup.
- `GET /me/capabilities` for the CRM UI.
- Authorization denials and Super Manager cross-team reads are logged and audited.

## Capabilities

### New Capabilities
- `api-authorization`: token trust, permissions and deny-by-default.
- `team-scope-enforcement`: which records a user may read or change.
- `user-capabilities`: the resolved capabilities endpoint for the UI.

## Impact

- New dependency on Spring Kafka and on the UMS endpoints `/internal/users/{sub}/team-scope` and `/internal/catalog/{service}` via a `crm-internal` client-credentials client for this service (scopes `ums:team-scope`, `ums:catalog`).
- Every later CRM endpoint must declare a permission.
