# Spec Delta

## Purpose

Tells the CRM UI what the current user may see and do, so it can hide what they can't, while the API still enforces everything.

## ADDED Requirements

### Requirement: Capabilities endpoint
`GET /me/capabilities` SHALL return the caller's `sub`, name, effective permissions (with `:write` implying `:read`), UI features (`ui:*`), member teams, managed teams and whether their scope is all teams.

#### Scenario: Team Manager
- **GIVEN** a Team Manager of team A holding `submissions:write` and `ui:team-dashboard`
- **WHEN** they call `/me/capabilities`
- **THEN** the response lists permissions `submissions:read` and `submissions:write`, uiFeatures [`team-dashboard`], managedTeams [A] and allTeams false

### Requirement: Capabilities don't authorize
Holding a capability in the response SHALL NOT grant access; every API call SHALL still be checked.

#### Scenario: Tampered UI
- **WHEN** a client calls a write endpoint the capabilities response didn't include
- **THEN** the response is 403
