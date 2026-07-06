# Spec: Native HITL

## ADDED Requirements

### Requirement: HITL checkpoints use WaitFor.formSubmission()

All human-in-the-loop checkpoints in agent actions SHALL use `WaitFor.formSubmission()` instead of manual `FormBindingRequest`/`FormSubmission` handling.

The system SHALL define specific record types for each HITL checkpoint:
- `PlanApproval` — for research plan approval (fields: `approved: boolean`, `feedback: String`)
- `InvestmentReviewFeedback` — for debate review (fields: `feedback: String`, `approved: boolean`)

#### Scenario: Plan approval HITL checkpoint
- **WHEN** the orchestrator generates a research plan
- **THEN** the `waitForPlanApproval` action returns `WaitFor.formSubmission("...", PlanApproval.class)`
- **THEN** the process enters WAITING state automatically

#### Scenario: Debate review HITL checkpoint
- **WHEN** the debate agent completes the debate loop and risk assessment
- **THEN** the `waitForReview` action returns `WaitFor.formSubmission("...", InvestmentReviewFeedback.class)`
- **THEN** the process enters WAITING state automatically

#### Scenario: Form auto-generated from record structure
- **WHEN** `WaitFor.formSubmission()` is called with a record type
- **THEN** the framework generates a form based on the record's fields
- **THEN** the form fields match the record's field names and types

### Requirement: Controllers detect WAITING state

The web controllers SHALL detect when a process is in WAITING state and render the appropriate form.

#### Scenario: WAITING state triggers form rendering
- **WHEN** a user navigates to `/status/{processId}`
- **THEN** if the process state is WAITING, the form is rendered
- **THEN** the form includes the relevant context (debate preview, plan preview)

#### Scenario: Form submission resumes process
- **WHEN** a user submits the HITL form
- **THEN** the framework binds form data to the record type
- **THEN** the process resumes with the bound data as the action's return value
- **THEN** subsequent actions receive the HITL result as a parameter

### Requirement: Prompt injection sanitization preserved

User input received through HITL forms SHALL be sanitized before injection into LLM prompts.

#### Scenario: Jinja syntax stripped from user feedback
- **WHEN** user submits feedback containing `{{` or `{%`
- **THEN** the feedback is sanitized to remove Jinja template syntax
- **THEN** the sanitized feedback is wrapped in XML delimiters

#### Scenario: Control characters stripped
- **WHEN** user submits feedback containing control characters (NUL, BEL, ESC)
- **THEN** the control characters are stripped
- **THEN** printable Unicode characters are preserved

#### Scenario: Feedback truncated
- **WHEN** user submits feedback longer than 1000 characters
- **THEN** the feedback is truncated to 1000 characters with a "[truncated]" suffix

## REMOVED Requirements

### Requirement: Manual FormBindingRequest handling in ProcessStatusController

**Reason**: WaitFor.formSubmission() handles form generation and binding automatically

**Migration**: Replace the 50-line `submitWaitForFeedback()` method with a simple form submission handler that calls `onResponse()` on the form binding request

### Requirement: Manual FormSubmission/FormResponse construction

**Reason**: WaitFor.formSubmission() eliminates the need for manual form object construction

**Migration**: Remove all manual `new FormSubmission(...)` and `new FormResponse(...)` calls from controllers
