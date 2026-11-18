# Submissions API

All endpoints need a `crm-internal` token for audience `crm-api` from an ACTIVE user, and the permission shown.
Errors are RFC 7807 problem details. Records of teams outside the caller's scope are reported as **404**, so their
existence doesn't leak. See [authorization.md](authorization.md) for the rules.

| Method and path | Permission | What it does |
|---|---|---|
| `GET /submissions` | `submissions:read` | Submissions of the caller's teams (all teams for a Super Manager), newest first |
| `GET /submissions/{id}` | `submissions:read` | One submission with its document references; a Super Manager's read of another team is audited |
| `POST /submissions/{id}/accept` | `submissions:write` | `{"version": n}`: RECEIVED → ACCEPTED; publishes `SubmissionAccepted` |
| `POST /submissions/{id}/reject` | `submissions:write` | `{"reason": "…", "version": n}`: RECEIVED → REJECTED (reason 1–500 characters, shown to the submitter); publishes `SubmissionRejected` |
| `POST /submissions/{id}/promote-submitter` | `portal-users:write` | Promotes the submitter from starter to standard through the UMS |
| `GET /routing-rules` | `routing-rules:read` | Routing rules, first one first |
| `POST /routing-rules` | `routing-rules:write` | Adds `{"type","region","teamId"}` as the last rule (201) |
| `PUT /routing-rules/order` | `routing-rules:write` | `{"ruleIds": [...]}` listing every rule once, in the new order |
| `DELETE /routing-rules/{id}` | `routing-rules:write` | Deletes a rule (204) |

`version` is the submission version the user looked at: the portal may send a correction (a higher version) at any
time, and deciding an older version is a 409, so nobody accepts documents they haven't seen. `:write` includes
`:read`. Accept, reject and promote need membership of the submission's team (checked live with
the UMS): a Super Manager may read other teams' submissions but not change them (403).

## List filters

`GET /submissions?status=RECEIVED&type=CLAIM&receivedFrom=2026-11-01T00:00:00Z&receivedTo=2026-12-01T00:00:00Z&page=0&size=50`

All filters are optional. `receivedFrom` is inclusive and `receivedTo` exclusive (ISO-8601 instants). `size` is at
most 200.

```json
{"content":[ /* submissions */ ],"page":0,"size":50,"totalElements":1,"totalPages":1}
```

## Submission

```json
{"id":"uuid","version":1,"type":"CLAIM","region":"EU","status":"REJECTED","teamId":"uuid",
 "portalUserId":"uuid","submitterEmail":"ana@acme.example","receivedAt":"…","decidedAt":"…",
 "rejectionReason":"Missing invoice",
 "documents":[{"documentId":"uuid","contentType":"application/pdf","size":1234,"sha256":"…"}]}
```

Documents are references only; the files stay in the portal's private storage.

## Errors

| Status | When |
|---|---|
| 400 | Invalid body or parameters (blank reason, bad date) |
| 403 | Missing permission, inactive user, or changing another team's submission |
| 404 | Unknown submission or rule, or one outside the caller's team scope |
| 409 | Deciding a submission that isn't RECEIVED or was corrected since (version mismatch), a concurrent decision, or the UMS refusing a promotion (for example the user isn't a starter) |
| 422 | A reorder that doesn't list every rule exactly once |
| 429 | The UMS's promotion rate limit |
| 502 | The UMS failed or couldn't be reached during a promotion |
| 503 | The UMS couldn't confirm the caller's status for a change |

## Promotion

`POST /submissions/{id}/promote-submitter` calls the UMS's `POST /internal/users/{portalUserId}/promote` as the
`crm-promoter` client (the only one holding `ums:promote`), passing the request's `X-Correlation-Id` (or a new one).
Response: `{"portalUserId":"uuid","promoted":true,"tier":"STANDARD","status":"ACTIVE"}`. Promoting an already
promoted user succeeds again (the UMS makes it a no-op).
