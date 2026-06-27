# Design: Fix UI Thymeleaf & htmx Best Practices

## Context

The TradingAgents project uses Thymeleaf templates with htmx for the web UI. There are 13 templates across `templates/` and `templates/common/fragments/`, 4 controllers, and 2 CSS files. The UI layer handles:

- **Form input** (`form.html`) — user enters a ticker symbol
- **Processing/status** (`processing.html`) — SSE-driven real-time agent progress
- **Plan review** (`plan-review.html`) — HITL checkpoint for research plan approval
- **Debate review** (`waiting.html`) — HITL checkpoint for investment debate review
- **Error handling** (`hitl.html`, `processing-error.html`) — failure recovery
- **Result display** (`plan.html`) — final investment plan output
- **Platform info** (`platform.html`) — developer links
- **User profile** (`user-info.html`) — legacy profile page

The layout system uses `common/layout.html` with a `layout(title, content, extraHead)` fragment. However, 4 of 13 templates bypass this system entirely, resulting in missing footers, inconsistent styling, and duplicated `<html>/<head>/<body>` structures.

A critical bug exists in the HITL approval flow: checkboxes in `waiting.html` and `plan-review.html` submit `approved=on` when checked, but `Boolean.parseBoolean("on")` returns `false`, meaning approval never works.

## Goals / Non-Goals

**Goals:**
- Fix the critical checkbox approval bug (`approved=on` → `false`)
- Ensure all page templates use the common layout fragment for consistency
- Remove dead htmx script loads from pages that don't use htmx
- Extract inline CSS from `waiting.html` and `plan-review.html` into `project.css`
- Create shared fragments for the duplicate approval form and button styles
- Add `hx-boost` to the main form for progressive enhancement
- Fix the "Kill Process" button targeting behavior
- Add `aria-live` region for SSE updates (accessibility)

**Non-Goals:**
- Adding new htmx features beyond `hx-boost` on the form
- Refactoring controllers or Java code
- Adding client-side form validation
- Migrating SSE from raw `EventSource` to htmx SSE extension
- Adding new CSS framework or changing the dark theme
- Rewriting `processing.html`'s ~200-line inline JavaScript

## Decisions

### D1: Layout wrapping for standalone pages

**Choice:** Wrap `processing.html`, `waiting.html`, `plan-review.html`, and `user-info.html` in the `common/layout` fragment.

Each page already has its own `<head>` with page-specific scripts (htmx, SSE, marked.js, DOMPurify). These will move into the `extraHeadContent` th:block within each page's `<head>`, before the `th:replace` call.

```
Current:                          After:
<html>                             <html th:replace="~{common/layout :: layout(...)}">
  <head>                           <head>
    <script>htmx</script>             <title>...</title>
  </head>                            <th:block th:fragment="extraHeadContent">
  <body>                             <script>htmx</script>
    <section>content</section>       </th:block>
  </body>                          </html>
</html>                            <body>
                                     <div class="container">
                                       <section>content</section>
                                     </div>
                                     <footer>...</footer>
                                   </body>
                                 </html>
```

**Rationale:** The layout fragment already handles CSS loading, footer, and container. Pages that need extra scripts (htmx, SSE, marked.js) use the `extraHeadContent` fragment slot.

**Alternatives considered:**
- Keep standalone pages — results in inconsistent UI (missing footer, different margins)
- Create a second "full" layout variant — adds complexity for minimal benefit
- Use `th:include` instead of `th:replace` — `th:replace` is the standard for full-page layout replacement

### D2: Checkbox fix — explicit `value="true"`

**Choice:** Add `value="true"` attribute to both approved checkboxes:

```html
<!-- waiting.html and plan-review.html -->
<input type="checkbox" id="approved" name="approved" value="true" checked />
```

**Rationale:** When a browser checkbox is checked, it submits `name=value`. Without an explicit `value`, the default is `"on"`. `Boolean.parseBoolean("on")` returns `false`. With `value="true"`, the checkbox submits `approved=true` and `Boolean.parseBoolean("true")` returns `true`.

This is a one-character change in two files. No controller changes needed.

**Alternatives considered:**
- Change controller to check `approved != null` — changes the semantics (unsubmitted checkbox = false, which is correct, but `approved=on` would also be true). Less explicit.
- Use `th:field="*{approved}"` — requires binding to a form object. The current forms use plain `@RequestParam`, not `@ModelAttribute`. More invasive.
- Use JavaScript to set the value on submit — fragile, defeats progressive enhancement.

### D3: Shared approval form fragment

**Choice:** Create `common/fragments/approval-form.html` with a fragment that accepts model variables for customization.

```html
<th:block th:fragment="approval-form(processId, feedbackPlaceholder, feedbackMaxlength, checkboxLabel, submitButtonText, cancelUrl)">
    <form th:action="@{/status/{processId}/waitfor(processId=${processId})}" method="post" class="approval-form">
        <div class="form-group">
            <label for="feedback">Feedback / Instructions (optional)</label>
            <textarea id="feedback" name="feedback" 
                      th:attr="maxlength=${feedbackMaxlength ?: 1000}, placeholder=${feedbackPlaceholder}"
                      th:classappend="${feedbackPlaceholder != null} ? '' : 'has-placeholder'"></textarea>
        </div>
        <div class="form-group">
            <div class="checkbox-group">
                <input type="checkbox" id="approved" name="approved" value="true" checked />
                <label for="approved" th:text="${checkboxLabel ?: 'Approve and proceed'}"></label>
            </div>
        </div>
        <div class="form-actions">
            <a th:href="${cancelUrl ?: '/'}" class="btn btn-cancel">Cancel</a>
            <button type="submit" class="btn btn-submit" th:text="${submitButtonText ?: 'Submit Feedback'}"></button>
        </div>
    </form>
</th:block>
```

**Rationale:** The form structure is identical in `waiting.html` and `plan-review.html` except for the checkbox label, submit button text, and form action URL. A fragment with default values keeps both templates DRY.

**Alternatives considered:**
- Use Thymeleaf's `th:replace` with variable fragments — more complex, harder to read
- Create a `@ControllerAdvice` model attribute — overkill for 2 form instances
- Keep duplicates — violates DRY, any future change needs to be applied in two places

### D4: CSS extraction to project.css

**Choice:** Move all inline `<style>` blocks from `waiting.html` and `plan-review.html` into `project.css` as named CSS classes.

Classes to extract (grouped by page):

**From `waiting.html`:**
- `.waiting-container`, `.waiting-header`, `.waiting-form`
- `.debate-preview`, `.debate-entry`, `.debate-turn`, `.debate-turn.bull`, `.debate-turn.bear`, `.debate-text`
- `.sse-status`
- Form styles (`.form-group`, `.checkbox-group`, `.form-actions`, `.btn`, `.btn-approve`, `.btn-submit`, `.btn-cancel`)

**From `plan-review.html`:**
- `.plan-review-container`, `.plan-review-header`, `.plan-preview`, `.plan-content`
- `.plan-actions`, `.flash-message`, `.flash-error`, `.flash-success`
- `.info-text`
- Button styles (`.btn`, `.btn-approve`, `.btn-reject`, `.btn-cancel`) — overlap with waiting.html

**Rationale:** `project.css` already has dark-theme styles (`.error-box`, `.hitl-form`, etc.). Adding these classes keeps the dark theme consistent and enables CSS caching.

**Alternatives considered:**
- Create a new `waiting.css` and `plan-review.css` — adds HTTP requests for small files
- Use `<link th:href="@{/css/waiting.css}">` — still a separate file, no advantage
- Keep inline — already the problem we're solving

### D5: htmx cleanup and adoption

**Choice:**
1. Remove htmx script from `platform.html` and `plan.html` (no htmx attributes used)
2. Add `hx-boost="true"` to the form in `form.html`
3. Fix "Kill Process" button in `processing.html` to target `#agent-status` instead of `this`
4. Add `aria-live="polite"` to the `#sse-updates` container

**Rationale:** `hx-boost` is the lowest-effort htmx adoption — it turns any existing form/link into an AJAX request without changing the controller. The "Kill Process" fix is a one-line change. The `aria-live` addition is a one-attribute change.

**Alternatives considered:**
- Full htmx migration (replace all form POSTs with `hx-post`) — more work, no clear benefit over `hx-boost`
- Keep htmx on all pages "just in case" — dead payload

### D6: Layout fragment handles empty extraHead

**Choice:** Update the `layout` fragment in `common/layout.html` to handle null/empty `extraHead` gracefully:

```html
<th:block th:replace="${extraHead != null ? extraHead : ~{layout :: empty}}"></th:block>
```

And add an empty fragment:
```html
<th:block th:fragment="empty"></th:block>
```

**Rationale:** Currently, pages with no extra head content must use `~{common/fragments/empty :: empty}` which is awkward. A null check in the layout itself is cleaner.

**Alternatives considered:**
- Make the third parameter optional in the fragment signature — Thymeleaf doesn't support optional fragment parameters
- Use `~{}` syntax — cleaner but less explicit for readers

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| Wrapping pages in layout changes DOM structure (adds `.container` div) | CSS selectors targeting body-level elements may break | Review and update CSS selectors; most pages already have their own container class |
| CSS extraction changes class names in templates | Templates reference inline styles via class names; extraction requires matching class names in `project.css` | Keep class names identical during migration; verify each page after extraction |
| `hx-boost` changes form submission from POST to AJAX | Controller receives `HX-Boosted` header; response handling may differ | `hx-boost` is transparent to the controller; the response is swapped into the page automatically |
| Kill Process button targets `#agent-status` which may not exist if the response is an error | If the API returns an error, the target may not be found | Add `hx-target` fallback or use `hx-target="#agent-container"` (parent container) |
| Shared approval form fragment changes form structure | Controllers expect specific field names (`feedback`, `approved`) | Fragment uses exact same field names; no controller changes needed |

## Migration Plan

1. **Fix the checkbox bug** (D2) — one-line change in 2 files, lowest risk
2. **Create shared fragments** (D3) — `approval-form.html` and `buttons.html`
3. **Extract inline CSS** (D4) — move styles to `project.css`
4. **Wrap standalone pages in layout** (D1) — structural change, verify each page
5. **Clean up dead htmx scripts** (D5.1) — remove from `platform.html` and `plan.html`
6. **Add hx-boost to form** (D5.2) — low-risk progressive enhancement
7. **Fix Kill Process button** (D5.3) — one-line target change
8. **Add aria-live** (D5.4) — one-attribute accessibility improvement
9. **Update layout fragment** (D6) — handle null extraHead gracefully
10. **Verify** — compile, run build, manual page-by-page verification

## Open Questions

- **Should `processing.html` also use `hx-boost` or full htmx attributes?** — The processing page is SSE-driven (no forms), so `hx-boost` isn't applicable. But the "Kill Process" button could use `hx-push-url` for URL history. Out of scope for this change.
- **Should `plan.html` (the travel result page) be cleaned up or deprecated?** — It references travel-related data (`travelPlan`, `airbnbUrl`) that doesn't match the trading app. Keeping it but cleaning up its htmx script is sufficient for now.