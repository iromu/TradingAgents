# Spec: Form Consistency & Checkbox Bug Fix

## ADDED Requirements

### Requirement: Approval checkboxes SHALL submit `value="true"`

All approval checkboxes (the `approved` field in HITL forms) SHALL include `value="true"` in the HTML so that when checked, the submitted value is `"true"` which `Boolean.parseBoolean("true")` correctly evaluates as `true`.

```html
<input type="checkbox" id="approved" name="approved" value="true" checked />
```

#### Scenario: Approved checkbox submits true
- **WHEN** the user checks the "Approve" checkbox (default checked)
- **THEN** the form submits `approved=true`
- **THEN** `Boolean.parseBoolean("true")` returns `true`
- **THEN** the agent process resumes

#### Scenario: Unchecked approved checkbox submits false
- **WHEN** the user unchecks the "Approve" checkbox
- **THEN** the form submits no `approved` parameter (unchecked checkboxes don't submit)
- **THEN** the controller default `defaultValue = "false"` applies
- **THEN** `Boolean.parseBoolean("false")` returns `false`
- **THEN** the agent process does not resume

#### Scenario: Checkbox value is explicit in HTML
- **WHEN** viewing the source of `waiting.html` or `plan-review.html`
- **THEN** the approved checkbox has `value="true"` attribute

### Requirement: Duplicate form structures SHALL be extracted to shared fragments

The feedback+approval form pattern SHALL be extracted into a shared fragment in `common/fragments/approval-form.html`.

The fragment SHALL accept these model variables:
- `processId` — the agent process ID for the form action
- `feedbackPlaceholder` — optional placeholder text for the feedback textarea
- `feedbackMaxlength` — optional maxlength attribute (default 1000)
- `checkboxLabel` — the label text for the approval checkbox
- `submitButtonText` — the text for the submit button
- `cancelUrl` — the URL for the cancel link (default `/`)

#### Scenario: waiting.html uses the approval form fragment
- **WHEN** `common/waiting.html` is rendered
- **THEN** it includes `~{common/fragments/approval-form :: approval-form}`
- **THEN** the form fields are: feedback (textarea), approved (checkbox), submit button, cancel link

#### Scenario: plan-review.html uses the approval form fragment
- **WHEN** `plan-review.html` is rendered
- **THEN** it includes `~{common/fragments/approval-form :: approval-form}`
- **THEN** the form fields are identical to waiting.html's form

#### Scenario: Fragment accepts custom submit button text
- **WHEN** the fragment is included with `submitButtonText="Approve & Execute"`
- **THEN** the submit button displays "Approve & Execute"
- **THEN** the button type is `submit`

### Requirement: Shared button styles SHALL be in a fragment

Common button classes (`btn`, `btn-approve`, `btn-reject`, `btn-cancel`, `btn-submit`) SHALL be defined in a shared fragment `common/fragments/buttons.html` and referenced via CSS classes in `project.css`.

#### Scenario: Button fragment defines approved style
- **WHEN** `buttons.html` is processed
- **THEN** the `.btn-approve` class uses green background (#4CAF50)
- **THEN** the `.btn-reject` class uses red background (#f44336)
- **THEN** the `.btn-cancel` class uses gray background (#555)

## REMOVED Requirements

### Requirement: Inline CSS in waiting.html and plan-review.html

All CSS styles defined in `<style>` blocks within `waiting.html` and `plan-review.html` SHALL be moved to `project.css` as named CSS classes.

**Reason:** Inline CSS in templates is hard to maintain, duplicates existing styles in `project.css`, and prevents CSS caching/optimization.

**Migration:** Extract all `.waiting-container`, `.waiting-header`, `.waiting-form`, `.debate-preview`, `.plan-review-container`, `.plan-review-header`, `.plan-preview`, `.plan-actions`, and `.flash-message` styles from the template `<style>` blocks into `project.css`.

### Requirement: Duplicate checkbox markup in waiting.html and plan-review.html

Both `waiting.html` and `plan-review.html` currently have the checkbox markup:
```html
<input type="checkbox" id="approved" name="approved" checked />
```
This is missing `value="true"` and causes the approval bug.

**Reason:** The checkbox submits `approved=on` when checked (browser default), but `Boolean.parseBoolean("on")` returns `false`. The HITL approval flow is broken.

**Migration:** Add `value="true"` to both checkboxes and extract the form into a shared fragment.