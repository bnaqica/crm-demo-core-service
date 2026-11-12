# Proposal

## Why

External users never call CRM APIs. Portal submissions reach the CRM as events, must be routed to an internal team, worked on by that team, and answered with a status the portal shows back to the user.

## What Changes

- Intake consumer on `portal-intake`: re-validates, deduplicates by event id, routes each submission to a team by rule (type, region), dead-letters invalid events.
- Team-scoped submissions API for internal users: list, view, accept, reject.
- Status events `SubmissionAccepted` / `SubmissionRejected` published to `submission-status` through the outbox.
- Portal user copies kept current from `PortalUserEmailChanged`.
- Internal users can promote a starter submitter through the UMS promote API (as `crm-promoter`).

## Capabilities

### New Capabilities
- `submission-intake`: consuming and routing portal submissions.
- `submissions`: the internal API for working on submissions and publishing their status.
- `portal-user-promotion`: promoting a submitter from starter to standard.

## Impact

- Tables `submission`, `portal_user`, `routing_rule`, `processed_event`.
- Topics: consumes `portal-intake`; publishes `submission-status`, `audit-events`; DLQ `portal-intake-dlq`.
- Permissions `submissions:read`, `submissions:write`, `portal-users:write`, `routing-rules:write`.
- Depends on `add-crm-authorization`, portal `add-portal-submissions` (event contract) and UMS `add-external-user-promotion`.
