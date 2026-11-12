# Spec Delta

## Purpose

Receives portal submissions as events, safely and exactly once, and hands each one to the right internal team.

## ADDED Requirements

### Requirement: Re-validate every event
The consumer SHALL validate each `SubmissionReceived` event against its schema version (event id, schema version, portal user id, email, submission id, type, region, document references) and SHALL send invalid events to `portal-intake-dlq` with the reason.

#### Scenario: Unknown schema version
- **WHEN** an event with schema version 99 arrives
- **THEN** it goes to the DLQ and no submission is created

### Requirement: Deduplicate by event id
The consumer SHALL process each event id at most once.

#### Scenario: Redelivered event
- **WHEN** the same `SubmissionReceived` event arrives twice
- **THEN** exactly one submission exists

### Requirement: Route by rule
Each new submission SHALL be assigned to a team by the first matching routing rule on type and region, or to the configured default team when none match.

#### Scenario: Matching rule
- **GIVEN** a rule `type=CLAIM, region=EU → team Claims EU`
- **WHEN** a CLAIM submission from region EU arrives
- **THEN** the submission belongs to team Claims EU with status RECEIVED

#### Scenario: No rule matches
- **WHEN** a submission matches no rule
- **THEN** it is assigned to the default team

### Requirement: Routing rules managed
Users with `routing-rules:write` SHALL be able to create, reorder and delete routing rules.

#### Scenario: Rule added
- **WHEN** a user with `routing-rules:write` adds a rule
- **THEN** later submissions are routed by it

### Requirement: Portal user copy kept current
The consumer SHALL keep a copy of each portal user's `sub` and email and update it on `PortalUserEmailChanged`.

#### Scenario: Email changed
- **WHEN** a `PortalUserEmailChanged` event arrives for a known user
- **THEN** the stored email is updated and submissions show the new email
