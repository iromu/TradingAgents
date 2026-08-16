---
title: "Web API (REST + HTMX)"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/web/TradingApiController.java"
  - "src/main/java/com/embabel/gekko/web/TradingHtmxController.java"
  - "src/main/java/com/embabel/gekko/web/ResearchPlanService.java"
  - "src/main/java/com/embabel/gekko/htmx/ProcessStatusController.java"
updated_at: "2026-08-16"
---

# Web API (REST + HTMX)

Gekko exposes two interfaces for driving the trading research workflow: a **REST API** for programmatic access and an **HTMX-based web UI** for interactive use. Both share `ResearchPlanService` as the common entry point.

## REST API (`/api`)

`TradingApiController` provides JSON endpoints for external clients:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/plan` | POST | Start research with a ticker; returns processId + status |
| `/api/plan/{processId}/status` | GET | Poll process status; includes plan content when WAITING, investment plan when COMPLETED |
| `/api/v1/process/{processId}/status` | GET | Lightweight status-only endpoint for SSE polling |
| `/api/plan/{processId}/approve` | POST | Approve/reject the research plan with optional feedback |

### Request/Response Records

- `TickerRequest(ticker, feedback)` — input to start research
- `ApprovalRequest(approved, feedback)` — plan approval submission

All endpoints validate the processId via `AgentUtils.validateProcessId()` (alphanumeric, underscore, hyphen only). Feedback is capped at 10,000 characters.

## HTMX Web UI

`TradingHtmxController` handles the browser-based workflow:

| Route | Purpose |
|-------|---------|
| `GET /` or `GET /research` | Show the ticker form (defaults to "NVDA") |
| `POST /plan` | Submit ticker → start agent process → redirect to processing or plan review |
| `GET /plan/review/{processId}` | Display the generated research plan for approval |
| `POST /plan/review/{processId}` | Submit plan approval (approved + feedback) |
| `GET /plan/status/{processId}` | Show processing page with SSE updates |

`ProcessStatusController` handles the HITL status page:

| Route | Purpose |
|-------|---------|
| `GET /status/{processId}` | Dispatch on process status: COMPLETED → result view, FAILED → HITL form, WAITING → waiting form, default → processing |
| `POST /status/{processId}/resubmit` | Failure recovery: create new process with user feedback |
| `POST /status/{processId}/waitfor` | WaitFor checkpoint: submit approval/feedback and resume process |

## ResearchPlanService

`ResearchPlanService` is the shared service that both controllers use to interact with the agent platform:

- **`createAndStart(input)`** — Finds `OrchestratorAgent`, creates an `AgentProcess` with verbosity and token budget, starts it
- **`createAndStart(agent, input)`** — Same but with a specific agent (used for HITL retry)
- **`getProcess(processId)`** — Look up a process by ID
- **`isWaiting(processId)`** — Check if a process is in WAITING state
- **`submitWaitForForm(process, values, errorMsg)`** — Submit a WaitFor form and resume the process

The token budget comes from `TraderAgentConfig.researchTokenBudget()`.

## Process Ownership

Both the HTMX controller and `ProcessStatusController` bind the `processId` to the HTTP session (`SESSION_PROCESS_ID = "hitl_processId"`). Subsequent POST requests verify the session-bound processId matches the requested one, preventing cross-session access to another user's process.

See `[[security]]` for details on the security model.

## Templates

| Template | Purpose |
|----------|---------|
| `form.html` | Ticker input form |
| `plan-review.html` | Research plan approval page |
| `common/processing.html` | SSE-driven progress display |
| `common/waiting.html` | WaitFor checkpoint form (debate preview + approval) |
| `common/hitl.html` | Failure recovery form |
| `common/fragments/approval-form.html` | Reusable approval form fragment |
