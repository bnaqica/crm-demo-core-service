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

## Implementation decisions

- **Tables without a CRUD controller** (decided with the developer's session; `.claude/rules/service-architecture.md`
  only exempts the audit table, so this is a known rule gap):
  - `submission`: created only from `SubmissionReceived` events; its API is list, view, accept and reject.
    Hand-made creates or edits would break the event contract with the portal.
  - `portal_user`: an event-sourced copy (submission events and `PortalUserEmailChanged`) kept for display, with no
    specified consumer, so no API (YAGNI).
  - `processed_event` and `outbox_event`: infrastructure, as in the UMS.
  - `routing_rule` has the full CRUD API the spec asks for.
- **Migration is `V3__submissions.sql`**, not `V2` as tasks.md says: `V2` already holds the outbox from
  add-crm-authorization. Documents are a fifth table, `submission_document` (an element collection of the submission),
  rather than a JSON column.
- **Corrections.** The portal sends a new `SubmissionReceived` (same `submissionId`, higher `version`) for each
  correction, which the specs don't cover. A higher version replaces type, region and documents but keeps the team and
  status (no re-routing of work in progress); equal or lower versions are ignored. Dedup by `eventId` still applies.
- **Validation** uses the JSON Schemas with networknt `json-schema-validator` 3.0.1 (the Jackson 3 line; not in Boot's
  BOM). Emails are checked with a simple `local@domain.tld` pattern, because the library's `email` format rejects
  reserved domains such as `.example`. Outgoing status events are validated against `submission-status-v1.json`, whose
  `additionalProperties: false` keeps CRM data out of them.
- **Status events** are one record (`SubmissionStatusChanged`) for both `SubmissionAccepted` and `SubmissionRejected`,
  told apart by `status`; the outbox carries no headers, so there is no `type` header (the portal doesn't need one).
- **Routing** matches type and region exactly. `GET /routing-rules` declares `routing-rules:read` (not in the proposal's
  permission list; `routing-rules:write` implies it). Rules are global configuration, so they aren't team-scoped.
- **Changes need team membership.** Accept, reject and promote use `TeamAccessService.checkChange` (live scope): a
  submission the caller can't read is 404 (and the UMS isn't called for a promotion); one they can read but whose team
  they aren't in (a Super Manager) is 403.
- **Concurrent decisions** are caught by optimistic locking (`row_version`) and answered with 409.
- **Promotion** returns `{portalUserId, promoted, tier: STANDARD, status}`; the UMS's 409/429 become problem details
  with our own wording (the UMS's message, which names its internal path, isn't passed on); anything else is 502.
- **Decisions name the version** (from code review): accept and reject take the `version` the user looked at
  (`{"version": n}`, and `{"reason", "version"}` for reject). A mismatch, meaning a correction arrived since, is a 409,
  so nobody accepts documents they haven't seen. This adds a required field to the accept/reject API.
- **Open for the developer** (code review, not changed): a correction to an already REJECTED or ACCEPTED submission
  keeps that status, so a submitter who fixes a rejected submission isn't picked up again; reopening it to RECEIVED
  would be a business decision. A late `SubmissionReceived` can overwrite a newer email (no ordering by
  `occurredAt`). Two concurrent rule creations can get the same position.
