# Spec Delta

## Purpose

Lets internal users grant a starter portal user standard access, through the UMS promote API rather than any direct identity change.

## ADDED Requirements

### Requirement: Promote submitter
A user with `portal-users:write` SHALL be able to promote the submitter of a submission in their team, which calls the UMS promote API with a correlation id.

#### Scenario: Promote starter
- **WHEN** a team member promotes the starter submitter of a team submission
- **THEN** the UMS promote API is called as `crm-promoter` and the response shows the user as promoted

#### Scenario: Out of scope
- **WHEN** a user promotes the submitter of another team's submission
- **THEN** the response is 404 and the UMS isn't called

### Requirement: UMS errors translated
UMS 409 and 429 responses SHALL be returned as 409 and 429 problem details; other UMS failures SHALL be 502.

#### Scenario: Not a starter
- **WHEN** the UMS returns 409 for an internal user
- **THEN** the response is a 409 problem detail
