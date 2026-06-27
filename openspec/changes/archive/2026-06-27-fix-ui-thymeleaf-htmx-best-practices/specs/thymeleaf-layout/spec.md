# Spec: Thymeleaf Layout Consistency

## MODIFIED Requirements

### Requirement: All page templates MUST use the common layout fragment

Every page template (not just fragments) SHALL wrap its content using the `common/layout` fragment:

```html
<html xmlns:th="https://www.thymeleaf.org" lang="en"
      th:replace="~{common/layout :: layout(~{::title}, ~{::section}, ~{::extraHeadContent})}">
<head>
    <title>Page Title</title>
    <th:block th:fragment="extraHeadContent">
        <!-- page-specific head content -->
    </th:block>
</head>
<body>
<section>
    <!-- page content -->
</section>
</body>
</html>
```

The layout fragment provides: shared CSS (`embabel-common-dark.css`, `project.css`), optional htmx/SSE script loading via `_htmx` and `_sse` model attributes, the site footer, and a consistent `.container` wrapper.

#### Scenario: Processing page uses layout
- **WHEN** the user navigates to `/plan/status/{processId}`
- **THEN** `common/processing.html` renders within the common layout
- **THEN** the page includes the footer and shared CSS

#### Scenario: Waiting page uses layout
- **WHEN** the user navigates to `/status/{processId}` (WAITING state)
- **THEN** `common/waiting.html` renders within the common layout
- **THEN** the page includes the footer and shared CSS

#### Scenario: Plan review page uses layout
- **WHEN** the user navigates to `/plan/review/{processId}`
- **THEN** `plan-review.html` renders within the common layout
- **THEN** the page includes the footer and shared CSS

#### Scenario: HITL error page uses layout
- **WHEN** the user navigates to `/status/{processId}` (FAILED state)
- **THEN** `common/hitl.html` renders within the common layout
- **THEN** the page includes the footer and shared CSS

### Requirement: Layout fragment parameters SHALL be optional for unused slots

The `layout` fragment in `common/layout.html` SHALL accept empty/thunk parameters for `extraHead` when no page-specific head content is needed, instead of requiring a separate `empty.html` fragment.

#### Scenario: Page with no extra head content
- **WHEN** a template has no page-specific CSS or JS
- **THEN** it can use `th:replace="~{common/layout :: layout(~{::title}, ~{::section}, ~{})}"` 
- **THEN** the layout renders without an error for a null/empty extraHead

### Requirement: Thymeleaf namespace SHALL use HTTPS

All templates SHALL use `xmlns:th="https://www.thymeleaf.org"` (not `http://`).

#### Scenario: user-info.html namespace updated
- **WHEN** `user-info.html` is rendered
- **THEN** it uses `https://www.thymeleaf.org` for the Thymeleaf namespace declaration