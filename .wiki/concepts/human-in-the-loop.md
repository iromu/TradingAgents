---
title: "Human-in-the-Loop"
type: "concept"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/htmx/HitlService.java"
  - "src/main/java/com/embabel/gekko/htmx/HitlAgenticEventListener.java"
  - "src/main/java/com/embabel/gekko/htmx/ProcessStatusController.java"
  - "src/main/java/com/embabel/gekko/util/AgentUtils.java"
updated_at: "2026-08-02"
---

# Human-in-the-Loop (HITL)

HITL is a core design principle in Gekko. The system **never makes a final investment decision without human oversight**.

## Two HITL Checkpoints

Gekko has two deliberate pause points where a human must review and approve:

### 1. Research Plan Approval

After the system generates a research plan but before executing the full workflow:

- **Action:** `waitForPlanApproval()`
- **User sees:** A summary of what data will be collected and how
- **User can:** Provide feedback, approve, or reject
- **If rejected:** The system stops; no data is collected

### 2. Debate Review

After the Bull/Bear debate completes but before generating the final plan:

- **Action:** `waitForReview()`
- **User sees:** The full debate history (bull arguments vs bear arguments)
- **User can:** Provide feedback, approve, or reject
- **If approved:** ResearchManager generates the final investment plan
- **If rejected:** The system stops; no plan is generated

## Why Two Checkpoints?

1. **Research Plan Approval** prevents wasted API calls and LLM costs if the plan is fundamentally flawed
2. **Debate Review** ensures a human reviews the reasoning before a decision is made

## Implementation

Gekko uses two HITL mechanisms:

### Embabel's Native WaitFor

- `WaitFor.formSubmission(title, FeedbackClass)` — creates a form from a Java record
- The process enters `WAITING` state
- A form is auto-generated from the record's fields
- User submits the form → process resumes with feedback bound to the blackboard

### Custom Failure Recovery

- `HitlService` manages HITL sessions with a 24-hour TTL and scheduled cleanup (every 5 minutes)
- `HitlAgenticEventListener` catches process failures and creates sessions
- User provides feedback → a new process is created with feedback injected
- Sessions auto-expire after 24 hours
- Session creation uses `computeIfAbsent()` for atomic check-and-create
- Session updates use `compute()` for atomic read-modify-write
- Session migration uses per-session locking to avoid global serialization
- `AgentUtils` provides shared WaitFor form submission and process lock utilities

## Security

User input is sanitized before being injected into LLM prompts:

- `sanitizeForPrompt()` strips Jinja syntax, control characters, and Unicode formatting
- Prevents prompt injection attacks
- Feedback is only injected when the user has approved
