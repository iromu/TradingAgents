# Security

## Purpose

Secure all POST endpoints with CSRF protection, validate user input on feedback parameters, and verify process ownership to prevent unauthorized access via known process IDs.

## Requirements

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

### Requirement: Shared prompt-input sanitization
The system SHALL sanitize all external and user-derived text before it is injected into any agent prompt — including user feedback, ticker symbols, instrument identity metadata, past-memory context, risk reasoning, and tool/data output — using a single shared sanitizer. Unsanitized external text MUST NOT reach a prompt.

The shared sanitizer SHALL neutralize Jinja template syntax, HTML/script content, and control characters.

#### Scenario: User feedback is sanitized before the final prompt
- **WHEN** user feedback containing Jinja template syntax or script tags is provided
- **THEN** the shared sanitizer removes/neutralizes that content before the text is placed into any agent prompt

#### Scenario: Identity and past-memory text are sanitized before the final prompt
- **WHEN** instrument identity metadata or past-memory context is injected into the final decision prompt
- **THEN** the text has been passed through the same shared sanitizer

#### Scenario: Tool and risk output are sanitized before prompt use
- **WHEN** tool or risk-debate output is placed into a prompt
- **THEN** the text has been passed through the same shared sanitizer

### Requirement: REST ticker path validation
The system SHALL validate the `ticker` parameter on the REST `/api/trading/*` endpoints using the same format rules as the HTMX `/plan` path (uppercase alphanumeric plus a single dot), rejecting invalid tickers with a 400 Bad Request response rather than passing them to the pipeline. The same format rules SHALL apply whenever a user-resubmitted value is re-injected into the pipeline as a ticker (e.g. the HITL resubmit path); raw resubmitted text MUST NOT be passed to the pipeline as a ticker without validation.

#### Scenario: Invalid ticker rejected on REST path
- **WHEN** a client POSTs a ticker such as `../etc` or `NV DA` to `/api/trading/plan`
- **THEN** the request is rejected with a 400 Bad Request response
- **AND** the ticker is not passed to the pipeline

#### Scenario: Valid ticker accepted on REST path
- **WHEN** a client POSTs a ticker such as `NVDA` or `BRK.B` to `/api/trading/plan`
- **THEN** the ticker is accepted and processed

#### Scenario: Invalid ticker rejected on the resubmit path
- **WHEN** a user resubmits a value containing characters outside the ticker format (script, path, or whitespace text)
- **THEN** the resubmitted value is rejected or sanitized before it is injected into the pipeline as a ticker
