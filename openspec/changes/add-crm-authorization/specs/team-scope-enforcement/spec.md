# Spec Delta

## Purpose

Restricts which team-owned records a user may read or change, so list and detail endpoints never leak other teams' data.

## ADDED Requirements

### Requirement: Read within scope
A user SHALL read a team-owned record only if their scope is all teams, or the record's team is one of their teams; otherwise the response SHALL be 404.

#### Scenario: Other team's record
- **GIVEN** a Team Member of team A
- **WHEN** they request a record owned by team B
- **THEN** the response is 404

#### Scenario: Super Manager reads another team
- **WHEN** a Super Manager who is not in team B reads a team B record
- **THEN** the record is returned and a cross-team read audit event is published

### Requirement: Lists filtered by scope
List endpoints SHALL return only records from teams in the caller's scope.

#### Scenario: Member lists records
- **GIVEN** records in teams A and B
- **WHEN** a member of A lists records
- **THEN** only team A records are returned

### Requirement: Cross-team access is read-only
Create, update and delete SHALL be allowed only on records whose team the user belongs to, regardless of an all-teams scope.

#### Scenario: Super Manager edits another team
- **WHEN** a Super Manager who is not in team B updates a team B record
- **THEN** the response is 403

### Requirement: Manager-only views
Endpoints marked manager-only SHALL be allowed only for teams the user manages, or for Super Managers.

#### Scenario: Member opens manager view
- **WHEN** a member of team A who doesn't manage it opens team A's manager view
- **THEN** the response is 403

### Requirement: Fresh scope
Cached team scope SHALL be evicted when a `TeamMembershipChanged` event for the user arrives and SHALL expire after at most 5 minutes; writes SHALL use live scope from the UMS.

#### Scenario: Removed from team
- **GIVEN** user U is removed from team A
- **WHEN** the `TeamMembershipChanged` event arrives and U lists records
- **THEN** no team A records are returned
