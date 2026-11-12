# Tasks

## 1. Token trust

- [ ] 1.1 Add Spring Kafka and Caffeine; configure the `crm-internal` issuer, `crm-api` audience and client-role converter; verify portal and admin tokens get 401 in a `@WebMvcTest`

## 2. Permissions

- [ ] 2.1 Add `@RequiresPermission` with write-implies-read evaluation; verify the missing-permission and write-holder scenarios
- [ ] 2.2 Add the architecture test that fails on any endpoint without a permission; verify it fails for a deliberately unannotated test controller
- [ ] 2.3 Register permissions with the UMS catalog on startup; verify with a mocked UMS client

## 3. Team scope

- [ ] 3.1 Implement the UMS client with client credentials and error translation; verify with a WireMock or MockRestServiceServer test
- [ ] 3.2 Implement `AccessPolicy` and the cached scope with event eviction; verify every `team-scope-enforcement` scenario in unit tests and the eviction with a Kafka Testcontainers test
- [ ] 3.3 Implement the live ACTIVE-status check for writes; verify the disabled-user scenario

## 4. Capabilities and audit

- [ ] 4.1 Implement `GET /me/capabilities`; verify the Team Manager scenario
- [ ] 4.2 Add the outbox and publish denial and cross-team-read audit events; verify payloads
- [ ] 4.3 Add a permission-matrix test of every role against every endpoint, and document the authorization model in `docs/`

## Workflow follow-up

- Archive the change with `/opsx:archive` after review.
