# Tasks: Fix UI Thymeleaf & htmx Best Practices

## 0. Fix Critical Bug: Checkbox Approval Logic (P0)

- [x] 0.1 In `waiting.html`, add `value="true"` to the approved checkbox: `<input type="checkbox" id="approved" name="approved" value="true" checked />`
- [x] 0.2 In `plan-review.html`, add `value="true"` to the approved checkbox: `<input type="checkbox" id="approved" name="approved" value="true" checked />`
- [x] 0.3 Verify both files compile and the form still submits correctly

## 1. Create Shared Fragments

- [x] 1.1 Create `common/fragments/buttons.html` with button class definitions (`.btn`, `.btn-approve`, `.btn-reject`, `.btn-cancel`, `.btn-submit`, `.submit-btn`, `.scary-delete-btn`)
- [x] 1.2 Create `common/fragments/approval-form.html` fragment with these parameters:
  - `processId` (required) — form action path
  - `feedbackPlaceholder` (optional, default "Provide feedback...")
  - `feedbackMaxlength` (optional, default 1000)
  - `checkboxLabel` (optional, default "Approve and proceed...")
  - `submitButtonText` (optional, default "Submit Feedback")
  - `cancelUrl` (optional, default "/")
  - The fragment includes: form with feedback textarea, approved checkbox (with `value="true"`), submit button, cancel link
- [x] 1.3 Verify fragment renders correctly by checking `waiting.html` preview

## 2. Extract Inline CSS from waiting.html

- [x] 2.1 Move `.waiting-container`, `.waiting-header`, `.waiting-form` styles from `waiting.html` `<style>` to `project.css`
- [x] 2.2 Move `.form-group`, `.form-group label`, `.form-group textarea`, `.form-group input[type="text"]` styles to `project.css`
- [x] 2.3 Move `.checkbox-group`, `.checkbox-group input[type="checkbox"]`, `.checkbox-group label` styles to `project.css`
- [x] 2.4 Move `.form-actions` styles to `project.css`
- [x] 2.5 Move `.btn-*` class styles to `project.css` (if not already in a button fragment)
- [x] 2.6 Move `.debate-preview`, `.debate-preview h4`, `.debate-entry`, `.debate-turn`, `.debate-turn.bull`, `.debate-turn.bear`, `.debate-text` styles to `project.css`
- [x] 2.7 Move `.sse-status` styles to `project.css`
- [x] 2.8 Remove the `<style>` block from `waiting.html`
- [x] 2.9 Verify `waiting.html` renders identically (same visual output)

## 3. Extract Inline CSS from plan-review.html

- [x] 3.1 Move `.plan-review-container`, `.plan-review-header`, `.plan-review-header h2`, `.h2`, `.plan-review-header p` styles to `project.css`
- [x] 3.2 Move `.plan-preview`, `.plan-preview h3`, `.plan-content`, `.plan-actions`, `.plan-actions h3` styles to `project.css`
- [x] 3.3 Move `.flash-message`, `.flash-error`, `.flash-success` styles to `project.css`
- [x] 3.4 Move `.info-text` styles to `project.css`
- [x] 3.5 Remove the `<style>` block from `plan-review.html`
- [x] 3.6 Verify `plan-review.html` renders identically

## 4. Wrap Standalone Pages in Common Layout

### 4.1 processing.html

- [x] 4.1.1 Add `th:replace="~{common/layout :: layout(~{::title}, ~{::section}, ~{::extraHeadContent})}"` to the `<html>` tag
- [x] 4.1.2 Add `<head>` section with `<title>Processing</title>` and `<th:block th:fragment="extraHeadContent">` containing the marked.js and DOMPurify script tags
- [x] 4.1.3 Remove the standalone `<head>`, `<title>`, `<link>` tags (move CSS references to layout)
- [x] 4.1.4 Ensure the `<body>` content is wrapped in `<section>` (already is)
- [x] 4.1.5 Verify the layout's `_htmx` model attribute is set to `true` in `GenericProcessingValues` or the controller so htmx loads (needed for Kill Process button)

### 4.2 waiting.html

- [x] 4.2.1 Add `th:replace="~{common/layout :: layout(~{::title}, ~{::section}, ~{::extraHeadContent})}"` to the `<html>` tag
- [x] 4.2.2 Add `<head>` with `<title>` and `<th:block th:fragment="extraHeadContent">` (empty, no extra scripts needed)
- [x] 4.2.3 Remove standalone `<head>`, `<meta>`, `<link>` tags
- [x] 4.2.4 Remove `<body>` wrapper (layout provides it)
- [x] 4.2.5 Wrap content in `<section>` instead of `<div class="waiting-container">`
- [x] 4.2.6 Update CSS selectors in `project.css` if needed (`.waiting-container` → `section .waiting-container` or remove container wrapper)

### 4.3 plan-review.html

- [x] 4.3.1 Add `th:replace="~{common/layout :: layout(~{::title}, ~{::section}, ~{::extraHeadContent})}"` to the `<html>` tag
- [x] 4.3.2 Add `<head>` with `<title>` and `<th:block th:fragment="extraHeadContent">` containing the `<link rel="stylesheet" th:href="@{/css/project.css}">`
- [x] 4.3.3 Remove standalone `<head>`, `<meta>`, `<link>` tags
- [x] 4.3.4 Remove `<body>` wrapper
- [x] 4.3.5 Wrap content in `<section>` instead of `<div class="plan-review-container">`

### 4.4 user-info.html

- [x] 4.4.1 Add `th:replace="~{common/layout :: layout(~{::title}, ~{::section}, ~{})}"` to the `<html>` tag
- [x] 4.4.2 Update Thymeleaf namespace to `https://www.thymeleaf.org`
- [x] 4.4.3 Remove standalone `<head>`, `<meta>`, `<link>` tags
- [x] 4.4.4 Remove `<body>` wrapper
- [x] 4.4.5 Wrap content in `<section>`

## 5. Clean Up Dead htmx Scripts

- [x] 5.1 In `platform.html`, remove `<script src="https://unpkg.com/htmx.org@2.0.5"></script>` and `<script src="https://unpkg.com/htmx.org@2.0.5/dist/ext/sse.js"></script>` from `extraHeadContent`
- [x] 5.2 In `plan.html`, remove `<script src="https://unpkg.com/htmx.org@2.0.5"></script>` from `extraHeadContent`
- [x] 5.3 Verify `platform.html` and `plan.html` still render correctly without htmx

## 6. Add htmx Progressive Enhancement

### 6.1 form.html

- [x] 6.1.1 Add `th:replace="~{common/layout :: layout(~{::title}, ~{::section}, ~{::extraHeadContent})}"` to the `<html>` tag
- [x] 6.1.2 Add `<th:block th:fragment="extraHeadContent"><script src="https://unpkg.com/htmx.org@2.0.5"></script></th:block>` to load htmx
- [x] 6.1.3 Add `hx-boost="true"` to the `<form>` element
- [x] 6.1.4 Remove standalone `<body>` wrapper (layout provides it)
- [x] 6.1.5 Wrap form content in `<section>`
- [x] 6.1.6 Verify the form still submits correctly (htmx boost is transparent to the controller)

### 6.2 processing.html — Fix Kill Process button

- [x] 6.2.1 Change `hx-target="this"` to `hx-target="#agent-status"` on the Kill Process button
- [x] 6.2.2 The button already has `th:if="${_htmx}"` on the layout's htmx script — verify `_htmx` is set to `true` in the model
- [x] 6.2.3 If `_htmx` is not set, add it in `GenericProcessingValues.addToModel()` or in the controller that renders processing.html

### 6.3 processing.html — Add aria-live

- [x] 6.3.1 Add `role="log"` and `aria-live="polite"` to the `<div id="sse-updates">` element
- [x] 6.3.2 This ensures screen readers announce new SSE updates

## 7. Update Layout Fragment

- [x] 7.1 In `common/layout.html`, update the `layout` fragment to handle null/empty `extraHead`:
  ```html
  <th:block th:replace="${extraHead != null ? extraHead : ~{layout :: empty}}"></th:block>
  ```
- [x] 7.2 Add an empty fragment to `common/layout.html`:
  ```html
  <th:block th:fragment="empty"></th:block>
  ```
- [x] 7.3 Update `form.html` and `plan.html` to use `~{}` or omit the third parameter instead of `~{common/fragments/empty :: empty}`

## 8. Update Common Fragment: plan-complete.html

- [x] 8.1 Change `<div th:fragment="plan-complete">` to `<th:block th:fragment="plan-complete">` to avoid extra div wrapper when used with `th:insert`
- [x] 8.2 Keep `th:replace` usage (in `plan.html`) — it works either way

## 9. Cleanup: Remove Unused Styles from project.css

- [x] 9.1 Review `project.css` for styles that are now duplicated by `embabel-common-dark.css` (e.g., `.form-group`, `.form-actions`)
- [x] 9.2 Styles in `project.css` (.form-group, .btn, .checkbox-group, .form-actions) are project-specific overrides that differ from the common dark theme (different border radii, padding, colors) — kept intentionally
- [x] 9.3 All page-specific styles (.waiting-container, .plan-review-container, .debate-preview, .flash-message, etc.) are unique to this project and not in embabel-common-dark.css

## 10. Verification

- [x] 10.1 Run `./mvnw compile` to verify no compilation errors
- [x] 10.2 Run `./mvnw verify` to confirm build passes
- [x] 10.3 Manually verify each page renders correctly (requires running the app and opening in browser):
  - [x] 10.3.1 `/` — form page with layout, htmx boost, footer
  - [x] 10.3.2 `/platform` — platform page with layout, no htmx, footer
  - [x] 10.3.3 `/plan/status/{id}` — processing page with layout, SSE, footer
  - [x] 10.3.4 `/plan/review/{id}` — plan review page with layout, footer
  - [x] 10.3.5 `/status/{id}` (WAITING) — waiting page with layout, debate preview, footer
  - [x] 10.3.6 `/status/{id}` (FAILED) — HITL error page with layout, footer
  - [x] 10.3.7 `/plan/{id}` (COMPLETED) — plan result page with layout, footer
- [x] 10.4 Verify the approval checkbox bug is fixed: check the box → form submits `approved=true` → process resumes
- [x] 10.5 Verify the Kill Process button targets `#agent-status` and doesn't inject JSON into itself