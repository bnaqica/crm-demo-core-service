# Spec Delta

## Purpose

Lets internal team users work on routed portal submissions and tells the portal the outcome.

## ADDED Requirements

### Requirement: List and view submissions
Users with `submissions:read` SHALL list submissions in their team scope, filtered by status, type and date, and view one with its document references.

#### Scenario: Member lists
- **WHEN** a member of team A lists submissions
- **THEN** only team A submissions are returned

### Requirement: Accept or reject
Users with `submissions:write` in the submission's team SHALL accept or reject a RECEIVED submission, rejection requiring a reason visible to the submitter.

#### Scenario: Accept
- **WHEN** a team member accepts a RECEIVED submission
- **THEN** its status becomes ACCEPTED

#### Scenario: Decide twice
- **WHEN** a user accepts an already REJECTED submission
- **THEN** the response is 409

### Requirement: Status published to the portal
Each accept or reject SHALL publish `SubmissionAccepted` or `SubmissionRejected` (event id, schema version, submission id, portal user id, reason when rejected, time) to `submission-status`, only after the change commits.

#### Scenario: Rejected
- **WHEN** a submission is rejected with reason "Missing invoice"
- **THEN** a `SubmissionRejected` event with that reason is published

### Requirement: Portal sees status only
Status events SHALL carry no CRM data beyond the submission id, status, reason and time.

#### Scenario: Event contents
- **WHEN** a status event is published
- **THEN** it has no team, assignee or internal notes
