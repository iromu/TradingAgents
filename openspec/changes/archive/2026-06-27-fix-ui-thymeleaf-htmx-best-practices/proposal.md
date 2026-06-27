# Proposal: Fix UI Thymeleaf & htmx Best Practices

## Why

The UI layer (13 Thymeleaf templates, 4 controllers, 2 CSS files) has accumulated inconsistencies and bugs from incremental development:

1. **Critical bug:** The HITL approval checkbox in `waiting.html` and `plan-review.html` submits `approved=on` when checked, but the controllers use `Boolean.parseBoolean("on")` which returns `false`. **The approval flow is broken — users checking the box are actually rejecting.**
2. **Inconsistent layout wrapping:** 4 of 13 templates bypass the shared `common/layout` fragment, resulting in missing footers, inconsistent styling, and duplicated `<html>/<head>/<body>` structures.
3. **Dead JS payload:** htmx is loaded on 3 pages (`platform.html`, `plan.html`, `form.html`) but never used — wasted bandwidth and potential confusion.
4. **Inline CSS bloat:** `waiting.html` and `plan-review.html` each contain 200+ lines of CSS embedded in `<style>` tags, duplicating the dark-theme already in `project.css`.
5. **No htmx adoption:** Despite htmx being loaded on several pages, no page uses htmx attributes for progressive enhancement (forms, navigation, confirmations).

## What Changes

- **Fix the checkbox approval bug** — add `value="true"` to approved checkboxes so `Boolean.parseBoolean("true")` works correctly
- **Wrap standalone pages in common layout** — `processing.html`, `waiting.html`, `plan-review.html`, and `user-info.html` will use the `common/layout` fragment
- **Remove dead htmx script tags** from pages that don't use htmx attributes
- **Move inline CSS to `project.css`** — extract styles from `waiting.html` and `plan-review.html` into the shared stylesheet
- **Extract shared form fragment** — the feedback+approval form pattern appears in both `waiting.html` and `plan-review.html`
- **Add `hx-boost` to the main form** in `form.html` for progressive enhancement
- **Fix the "Kill Process" button** in `processing.html` — it targets a JSON API but uses `hx-swap="outerHTML"`
- **Add `aria-live="polite"`** to SSE update container for accessibility

## Capabilities

### Modified Capabilities

<!-- No existing specs in openspec/specs/ — nothing to modify -->

## Impact

- **Modified:** `form.html`, `plan.html`, `processing.html`, `plan-review.html`, `waiting.html`, `hitl.html`, `user-info.html`, `common/layout.html`, `common/fragments/footer.html`, `common/fragments/plan-complete.html`, `project.css`
- **Created:** `common/fragments/approval-form.html` (shared HITL form fragment), `common/fragments/buttons.html` (shared button fragment)
- **Deleted:** No files deleted (legacy `plan.html` kept but cleaned up)
- **Unchanged:** All Java controllers, services, agents, prompts, CSS framework (embabel-common-dark.css), JS libraries (marked, DOMPurify, htmx)
- **NOT BREAKING:** All existing HTTP endpoints and form field names preserved