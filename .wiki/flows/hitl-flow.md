---
title: "HITL Flow"
type: "flow"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/htmx/ProcessStatusController.java"
  - "src/main/java/com/embabel/gekko/htmx/HitlService.java"
  - "src/main/java/com/embabel/gekko/htmx/HitlAgenticEventListener.java"
  - "src/main/resources/templates/common/waiting.html"
  - "src/main/resources/templates/common/hitl.html"
updated_at: "2026-06-11"
---

# Human-in-the-Loop (HITL) Flow

Gekko uses two HITL patterns: **Embabel's native WaitFor** (for pre-execution checkpoints) and a **custom fallback** (for process failures).

## Pattern 1: WaitFor (Pre-Execution HITL)

This is the primary HITL mechanism. The agent process enters a WAITING state and waits for user input.

### When it happens

1. **After debate completes** — before generating the final investment plan
2. **After research plan is generated** — before executing the full workflow

### How it works

1. Agent calls `WaitFor.formSubmission(title, FeedbackClass)`
2. Embabel creates a `FormBindingRequest` on the blackboard
3. Process status becomes `WAITING`
4. User visits `/status/{processId}` and sees the waiting form
5. User fills in the form (feedback text + approval checkbox)
6. POST to `/status/{processId}/waitfor`
7. `ProcessStatusController.submitWaitForFeedback()` processes the form
8. Process is resumed with the user's feedback bound to the blackboard

### Key files

| File | Role |
|------|------|
| `TraderAgent.waitForReview()` | Returns the WaitFor form |
| `InvestmentReviewFeedback` | Record defining form fields |
| `ProcessStatusController.submitWaitForFeedback()` | POST handler for form submission |
| `ProcessStatusController.renderWaitingForm()` | GET handler that shows the form |
| `waiting.html` | Thymeleaf template for the form |

### Form fields

For `InvestmentReviewFeedback`:
- `feedback` (String) — user's comments on the debate
- `approved` (boolean) — whether to proceed with the plan

For `PlanApproval`:
- `feedback` (String) — user's comments on the plan
- `approved` (boolean) — whether to execute the full workflow

## Pattern 2: Failure Recovery (Post-Execution HITL)

This is a fallback mechanism for when an agent process fails unexpectedly.

### When it happens

When an agent process enters the `FAILED` state (e.g., LLM error, network failure).

### How it works

1. `HitlAgenticEventListener` detects the failure and creates a `HitlSession`
2. User visits `/status/{processId}` and sees the HITL form
3. User provides feedback on what went wrong
4. POST to `/status/{processId}/resubmit`
5. A new agent process is created with feedback injected
6. Old session is migrated to the new processId

### Key files

| File | Role |
|------|------|
| `HitlAgenticEventListener` | Listens for process failures |
| `HitlService` | Manages HITL sessions with TTL-based cleanup |
| `HitlConfig` | Creates the `HitlService` bean (24h TTL) |
| `ProcessStatusController.resubmit()` | POST handler for failure recovery |
| `hitl.html` | Thymeleaf template for the form |

### Session Management

`HitlService` uses a 24-hour TTL:
- Sessions older than 24 hours are automatically cleaned up every 5 minutes
- Uses `ConcurrentHashMap` for thread-safe session storage
- `computeIfAbsent()` prevents duplicate session creation
- `compute()` prevents concurrent update overwrites

## Security

User feedback is sanitized before being injected into LLM prompts:

- `sanitizeForPrompt()` strips Jinja syntax (`{{`, `}}`, `{%`, `%}`)
- Control characters and Unicode formatting characters are removed
- This prevents prompt injection attacks from malicious user input
