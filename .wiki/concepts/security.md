---
title: "Security Model"
type: "concept"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/config/SecurityConfig.java"
  - "src/main/java/com/embabel/gekko/util/AgentUtils.java"
  - "src/main/java/com/embabel/gekko/web/TradingHtmxController.java"
  - "src/main/java/com/embabel/gekko/htmx/ProcessStatusController.java"
  - "src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java"
updated_at: "2026-08-16"
---

# Security Model

Gekko applies several layers of input validation and access control to protect against common web vulnerabilities.

## CSRF Protection

`SecurityConfig` enables Spring Security's CSRF protection with a cookie-based token repository:

- **Repository:** `CookieCsrfTokenRepository.withHttpOnlyFalse()` — the token is stored in a non-HttpOnly cookie so HTMX can read it via JavaScript
- **Request handler:** `CsrfTokenRequestAttributeHandler` — reads the token from request attributes
- **Authorization:** All requests are permitted (`anyRequest().permitAll()`) — no authentication layer yet

This protects HTMX POST forms from cross-site request forgery without requiring session-based auth.

## Process ID Validation

`AgentUtils.validateProcessId()` enforces a strict character whitelist on all process IDs before they reach the agent platform:

```java
processId.matches("^[A-Za-z0-9_-]+$")
```

Invalid IDs throw `ResponseStatusException(400)`. This prevents path traversal and injection through the processId parameter.

## Ticker Validation

Two layers validate ticker symbols:

- **`OrchestratorAgent.tickerFromForm()`** — rejects blank input, uppercases, validates against `^[A-Z0-9.]+$`
- **`AlphaVantageService.validateTicker()`** — validates against `^[A-Z]{1,6}(\.[A-Z]{1,2})?$` (e.g., AAPL, BRK.B)

Both throw `IllegalArgumentException` on invalid input.

## Feedback Length Limit

All user feedback fields are capped at **10,000 characters**:

- REST API: `TradingApiController.approvePlan()` returns 400 if exceeded
- HTMX controller: `TradingHtmxController.submitPlanApproval()` shows an error message
- Process status: `ProcessStatusController.resubmit()` and `submitWaitForFeedback()` show an error page

This prevents oversized payloads from being injected into LLM prompts.

## Prompt Injection Sanitization

`DebateAgent.sanitizeValue()` strips dangerous content before template injection:

- Jinja syntax (`{{ }}`, `{% %}`, unclosed variants) → `[BLOCKED_TEMPLATE]`
- Code fences (```) → `[BLOCKED_CODE]`
- Control characters removed (only `\t`, `\n`, `\r` preserved)
- Input truncated to 10,000 chars before regex processing (ReDoS mitigation)
- Output truncated to 1,000 chars

Pre-compiled `Pattern` objects avoid ReDoS from repeated compilation.

## Session-Based Process Ownership

The HTMX controllers bind a `processId` to the HTTP session when first accessed. Subsequent POST requests verify ownership:

```java
String ownedProcessId = (String) session.getAttribute(SESSION_PROCESS_ID);
if (ownedProcessId == null || !ownedProcessId.equals(processId)) {
    // Access denied
}
```

This prevents one user from approving or resubmitting another user's research process.

## Checkpoint Path Traversal Prevention

`CheckpointStore.checkpointPath()` sanitizes ticker names before using them as filenames:

- Validates against `^[A-Za-z0-9._-]+$`
- Calls `normalize()` and verifies the result stays within the checkpoint directory

## Per-Process Locking

`AgentUtils.getProcessLock(processId)` provides a per-process lock object from a `ConcurrentHashMap`. Used by both REST and HTMX approval endpoints to prevent duplicate submissions:

```java
synchronized (AgentUtils.getProcessLock(processId)) {
    if (process.getStatus() != AgentProcessStatusCode.WAITING) { /* reject */ }
    // submit form
}
```

The lock is cleaned up in `AgentUtils.submitWaitForForm()`'s `finally` block.
