## Purpose

Secure all POST endpoints with CSRF protection, validate user input on feedback parameters, and verify process ownership to prevent unauthorized access via known process IDs.

## ADDED Requirements

### Requirement: CSRF protection enabled for all state-changing endpoints
The system SHALL enable CSRF protection on all POST endpoints to prevent cross-site request forgery attacks.

#### Scenario: POST endpoint requires CSRF token
- **WHEN** a client submits a POST request to `/plan` without a valid CSRF token
- **THEN** the request is rejected with a 403 Forbidden response
- **AND** the response indicates CSRF validation failed

#### Scenario: HTMX requests include CSRF token
- **WHEN** an HTMX request is submitted from a Thymeleaf-rendered form
- **THEN** the form includes a CSRF token input field
- **AND** the token is validated on submission

### Requirement: User feedback input validation
All controller endpoints that accept user feedback SHALL validate input length and sanitize content before processing.

#### Scenario: Feedback length limited
- **WHEN** a user submits feedback exceeding 10,000 characters
- **THEN** the request is rejected with a 400 Bad Request response
- **AND** the error message indicates the maximum length

#### Scenario: Feedback content sanitized
- **WHEN** a user submits feedback containing potentially malicious content (e.g., script tags, Jinja syntax)
- **THEN** the content is sanitized before being stored or passed to downstream services
- **AND** the sanitized content is used in flash messages

### Requirement: Process ownership verification
The system SHALL verify that a user is authorized to interact with a given process ID before allowing state changes.

#### Scenario: Unauthorized process access rejected
- **WHEN** a client submits a request for a `processId` they do not own
- **THEN** the request is rejected with a 403 Forbidden response
- **AND** the error does not leak information about valid process IDs

#### Scenario: Authorized process access allowed
- **WHEN** a client submits a request for a `processId` they own
- **THEN** the request is processed normally
- **AND** the process state is updated

### Requirement: Flash message sanitization
All flash messages in controllers SHALL sanitize user-provided content before rendering.

#### Scenario: Process ID in flash message sanitized
- **WHEN** a flash message includes a `processId` parameter
- **THEN** the process ID is HTML-escaped before being stored in the flash attribute

#### Scenario: Feedback in flash message sanitized
- **WHEN** a flash message includes user feedback content
- **THEN** the feedback is HTML-escaped before being stored in the flash attribute
