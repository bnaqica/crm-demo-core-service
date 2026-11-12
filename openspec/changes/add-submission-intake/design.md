# Design

## Context

Depends on `add-crm-authorization`. Event contracts are shared with the customer-portal-service. See proposal.md.

## Decisions

- **Event contract** (JSON string values, no Java types on the wire): `SubmissionReceived v1 {eventId, schemaVersion, occurredAt, portalUserId, email, submissionId, version, type, region, documents:[{documentId, storageKey, contentType, size, sha256}]}`; `SubmissionAccepted|Rejected v1 {eventId, schemaVersion, occurredAt, submissionId, version, portalUserId, status, reason?}`; `PortalUserEmailChanged v1 {eventId, schemaVersion, occurredAt, portalUserId, email}`. JSON Schemas kept in `src/main/resources/schemas/` until a schema registry is introduced. The `type` header distinguishes event kinds on `portal-intake`.
- **Idempotency** with `processed_event(event_id primary key)` inserted in the same transaction as the submission.
- **Errors:** `DefaultErrorHandler` with `DeadLetterPublishingRecoverer` to `portal-intake-dlq`, `FixedBackOff(1000, 3)`, no retry for validation and JSON errors; log before DLQ.
- **Documents** stay in the portal's private storage; the CRM stores references and fetches a short-lived download URL from the portal backend if needed (out of scope here).
- **Promotion** uses a separate `crm-promoter` client-credentials registration in `client`, isolated from the `core-service` client so only the promote call can carry `ums:promote`.

## Risks / Trade-offs

- [Schema evolution] → consumer accepts listed versions only; new versions are added before producers emit them.
