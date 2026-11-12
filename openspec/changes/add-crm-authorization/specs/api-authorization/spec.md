# Spec Delta

## Purpose

Ensures every CRM API call is made by an active internal user who holds the permission the endpoint declares, and denies everything else.

## ADDED Requirements

### Requirement: Trust one realm
The service SHALL accept only JWTs issued by the `crm-internal` realm with audience `crm-api`, and SHALL reject tokens from `crm-admin` or `portal` with 401.

#### Scenario: Portal token
- **WHEN** a request carries a valid `portal` token
- **THEN** the response is 401

#### Scenario: Admin token
- **WHEN** a request carries a valid `crm-admin` token
- **THEN** the response is 401

### Requirement: Permission per endpoint
Every endpoint except the health check SHALL declare a required permission. A request SHALL succeed only if the token holds it; otherwise the response SHALL be 403.

#### Scenario: Missing permission
- **WHEN** a user without `submissions:write` calls an endpoint declaring `submissions:write`
- **THEN** the response is 403

### Requirement: Undeclared endpoints fail the build
The build SHALL fail if any controller endpoint lacks a permission declaration.

#### Scenario: Endpoint added without permission
- **WHEN** a developer adds a GET endpoint with no permission declaration and runs the build
- **THEN** the build fails naming the endpoint

### Requirement: Write implies read
A user holding `<resource>:write` SHALL be allowed on endpoints declaring `<resource>:read`.

#### Scenario: Write holder reads
- **WHEN** a user with only `submissions:write` calls a `submissions:read` endpoint
- **THEN** the request is allowed

### Requirement: Only active users
The service SHALL deny with 403 any request from a user whose live status in the UMS is not ACTIVE.

#### Scenario: Disabled user with valid token
- **GIVEN** a user disabled after their token was issued
- **WHEN** they call a write endpoint before the token expires
- **THEN** the response is 403

### Requirement: Denials logged and audited
Every authorization denial SHALL be logged with user `sub`, permission and resource, and published as an audit event.

#### Scenario: Denied request
- **WHEN** a request is denied for a missing permission
- **THEN** an audit event with outcome DENIED, the user, the permission and the path is published to `audit-events`

### Requirement: Permissions registered with the catalog
On startup the service SHALL register every declared permission, with description and sensitive flag, with the UMS catalog.

#### Scenario: Startup registration
- **WHEN** the service starts with endpoints declaring `submissions:read` and `submissions:write`
- **THEN** the UMS catalog for core-service lists both
