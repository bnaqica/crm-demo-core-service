# Event contracts

Events are JSON text with no Java type headers. Their JSON Schemas live in `src/main/resources/schemas/` (until a
schema registry is introduced) and are enforced: incoming events are validated before anything is stored, and
outgoing status events are validated before they are written to the outbox. A new schema version must be added here
before any producer emits it; unknown versions are dead-lettered.

## Consumed

### `portal-intake` (from customer-portal-service)

Key: the portal user id. Header `type` (UTF-8 string) says which event it is. Consumer group
`crm-demo-core-service`.

**`SubmissionReceived` v1** (`submission-received-v1.json`), for each new submission and each correction:

```json
{"eventId":"uuid","schemaVersion":1,"occurredAt":"2026-11-12T09:00:00Z","portalUserId":"uuid",
 "email":"ana@acme.example","submissionId":"uuid","version":1,"type":"CLAIM","region":"EU",
 "documents":[{"documentId":"uuid","storageKey":"…","contentType":"application/pdf","size":1234,"sha256":"64 hex"}]}
```

- New `submissionId`: routed by the first routing rule matching `type` and `region` (exact match), or to
  `CRM_DEFAULT_TEAM_ID`, with status `RECEIVED`.
- Known `submissionId` with a higher `version` (a correction): type, region and documents are replaced; the
  submission keeps its team and status. Equal or lower versions are ignored.
- The submitter's email is stored as core-service's copy of the portal user.

**`PortalUserEmailChanged` v1** (`portal-user-email-changed-v1.json`):

```json
{"eventId":"uuid","schemaVersion":1,"occurredAt":"…","portalUserId":"uuid","email":"new@acme.example"}
```

Updates the stored email; submissions show the new one.

**Delivery rules.** Each `eventId` is applied at most once (`processed_event`, written in the same transaction as
the change). Invalid events (schema, unknown `schemaVersion`, missing or unknown `type` header) and unreadable JSON go
straight to **`portal-intake-dlq`**; other failures are retried 3 times a second apart first. The reason is in the
dead letter's `kafka_dlt-exception-message` header, and every dead letter is logged.

### `ums-user-events` (from the UMS)

Any message carrying a `sub` evicts that user's cached team scope (for example `TeamMembershipChanged`,
`UserDisabled`). Each instance reads it in a consumer group of its own, from the latest offset. Unreadable messages go
to `ums-user-events-dlq`.

## Published (through the transactional outbox)

Events are written in the transaction that makes the change and published after it commits, at least once; consumers
drop duplicates by `eventId`.

### `submission-status` (to customer-portal-service)

Key: the portal user id. **`SubmissionAccepted` / `SubmissionRejected` v1** (`submission-status-v1.json`):

```json
{"eventId":"uuid","schemaVersion":1,"occurredAt":"…","submissionId":"uuid","version":1,"portalUserId":"uuid",
 "status":"REJECTED","reason":"Missing invoice"}
```

`reason` only when `REJECTED`. No other fields are allowed (the schema has `additionalProperties: false`), so no team,
assignee or internal notes reach the portal.

### `audit-events` (to the UMS central audit)

Authorization denials and Super Manager cross-team reads; see [authorization.md](authorization.md#5-audit).
