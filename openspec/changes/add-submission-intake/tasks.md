# Tasks

## 1. Data and contracts

- [ ] 1.1 Add `V2__submissions.sql` (submission, portal_user, routing_rule, processed_event) and verify the migration test
- [ ] 1.2 Add JSON Schemas and event records for the three events; verify schema validation unit tests

## 2. Intake

- [ ] 2.1 Implement the `portal-intake` listener with validation, dedup and DLQ; verify every `submission-intake` validation and dedup scenario with Kafka Testcontainers
- [ ] 2.2 Implement routing rules CRUD and routing; verify matching and default-team scenarios
- [ ] 2.3 Handle `PortalUserEmailChanged`; verify the email scenario

## 3. Submissions API

- [ ] 3.1 Implement list and view with team scope; verify the member-lists scenario
- [ ] 3.2 Implement accept and reject with outbox status events; verify the `submissions` scenarios and event contents

## 4. Promotion

- [ ] 4.1 Implement the `crm-promoter` UMS client and error translation; verify the `portal-user-promotion` scenarios
- [ ] 4.2 Document the event contracts and submissions API in `docs/`

## Workflow follow-up

- Archive the change with `/opsx:archive` after review.
