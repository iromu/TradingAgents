## ADDED Requirements

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
