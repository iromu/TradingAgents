---
title: "UI Routes"
type: "reference"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/web/TradingHtmxController.java"
  - "src/main/java/com/embabel/gekko/htmx/PlatformController.java"
  - "src/main/java/com/embabel/gekko/htmx/GenericProcessingValues.java"
  - "src/main/resources/templates/"
updated_at: "2026-06-11"
---

# UI Routes

## Web Controllers

| Controller | Base Path | Purpose |
|-----------|-----------|---------|
| `TradingHtmxController` | `/` | Main trading UI |
| `PlatformController` | `/platform` | Agent platform overview |
| `ProcessStatusController` | `/status/{processId}` | Process status polling and HITL |

## Routes

| Route | Method | Purpose | Template |
|-------|--------|---------|----------|
| `/` | GET | Home page — enter ticker | `form.html` |
| `/plan` | GET | View investment plan | `plan.html` |
| `/plan-review` | GET | Review plan | `plan-review.html` |
| `/status/{processId}` | GET | Poll process status | `common/processing.html` |
| `/status/{processId}` | GET (WAITING) | Show HITL form | `common/waiting.html` |
| `/status/{processId}` | GET (FAILED) | Show error HITL form | `common/hitl.html` |
| `/status/{processId}/resubmit` | POST | Retry failed process | `common/processing.html` |
| `/status/{processId}/waitfor` | POST | Submit HITL form | `common/processing.html` |

## Template Hierarchy

```
templates/
├── form.html                    — Home page (ticker input)
├── plan.html                    — Final investment plan
├── plan-review.html             — Plan review
└── common/
    ├── layout.html              — Base layout
    ├── processing.html          — Processing state (polling)
    ├── processing-error.html    — Error state
    ├── waiting.html             — HITL form (debate review)
    ├── hitl.html                — HITL form (error recovery)
    ├── fragments/
    │   ├── empty.html           — Empty fragment
    │   ├── footer.html          — Footer
    │   ├── plan-complete.html   — Plan complete fragment
    └── user-info.html           — User info fragment
```

## HTMX Pattern

The UI uses HTMX for seamless real-time updates:
1. User submits ticker → process starts → returns `processId`
2. Client polls `/status/{processId}` via HTMX
3. Server returns different templates based on process status
4. Final result is shown when process completes

## Layout

All pages use `common/layout.html` as the base template, which provides:
- Common header and navigation
- Gekko branding
- Responsive layout
