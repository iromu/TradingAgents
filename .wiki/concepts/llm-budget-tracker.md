---
title: "LLM Budget Tracker"
type: "concept"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/LlmBudgetTracker.java"
  - "src/test/java/com/embabel/gekko/util/LlmBudgetTrackerTest.java"
updated_at: "2026-08-02"
---

# LLM Budget Tracker

`LlmBudgetTracker` is a soft limiter that tracks LLM API calls per ticker and logs a warning when a configured budget is exceeded. It does not block calls — it's a diagnostic guard, not a hard circuit breaker.

## Purpose

During a research workflow, a single ticker can trigger many LLM calls (analyst reports, debate turns, risk assessment, etc.). The budget tracker helps detect when a workflow is making more calls than expected, which can indicate:

- A debate loop failing to converge
- An agent making redundant calls
- Unexpected retry behavior

## Configuration

Configured via `llm.budget.max` (default: 30 calls per ticker):

```yaml
llm:
  budget:
    max: 30
```

## How It Works

1. Each LLM call records the ticker via `recordCall(ticker)`
2. Call counts are stored in a `ConcurrentHashMap<String, Integer>`
3. When a ticker exceeds the budget, a warning is logged
4. Counts are reset via `reset(ticker)` or `resetAll()` after a workflow completes

## Usage in DebateLoopAgent

`DebateLoopAgent` records a call after each bull and bear turn:

```java
String bullResponse = cache.getOrCompute(..., () -> bullResearcher.argue(...));
llmBudgetTracker.recordCall(ticker.content());
```

This means a 5-round debate (10 turns) uses 10 budget units. Combined with analyst reports (~4 calls) and other agents, a typical workflow uses around 15-20 calls per ticker.

## API

| Method | Returns | Description |
|--------|---------|-------------|
| `recordCall(ticker)` | `int` | Record a call, returns current count |
| `getCallCount(ticker)` | `int` | Get current count for a ticker |
| `reset(ticker)` | `void` | Reset count for a ticker |
| `resetAll()` | `void` | Reset all counts |

## Design Notes

- **Soft limit only:** Exceeding the budget logs a warning but does not throw or block. This avoids hard failures during workflows while still providing visibility.
- **In-memory only:** Counts are not persisted. A restart resets all counts.
- **Thread-safe:** Uses `ConcurrentHashMap` with `merge()` for atomic increments.
