---
title: "LLM Budget Tracker"
type: "concept"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/LlmBudgetTracker.java"
  - "src/main/java/com/embabel/gekko/util/BudgetExceededException.java"
  - "src/test/java/com/embabel/gekko/util/LlmBudgetTrackerTest.java"
updated_at: "2026-08-13"
---

# LLM Budget Tracker

`LlmBudgetTracker` tracks LLM API calls per ticker and can operate in two modes: **soft limit** (warning only) or **hard limit** (throws exception).

## Purpose

During a research workflow, a single ticker can trigger many LLM calls (analyst reports, debate turns, risk assessment, etc.). The budget tracker helps detect when a workflow is making more calls than expected, which can indicate:

- A debate loop failing to converge
- An agent making redundant calls
- Unexpected retry behavior

## Configuration

```yaml
llm:
  budget:
    max: 30            # Max calls per ticker (default: 30)
    hard-limit: false  # Throw exception on exceed (default: false)
```

| Property | Default | Description |
|----------|---------|-------------|
| `llm.budget.max` | 30 | Max LLM calls allowed per ticker |
| `llm.budget.hard-limit` | `false` | If `true`, throws `BudgetExceededException` when budget is exceeded |

## How It Works

1. Each LLM call records the ticker via `recordCall(ticker)`
2. Call counts are stored in a `ConcurrentHashMap<String, Integer>`
3. When a ticker exceeds the budget:
   - **Soft mode** (default): logs a warning, allows the call to proceed
   - **Hard mode**: throws `BudgetExceededException` with ticker, count, and budget details
4. Counts are reset via `reset(ticker)` or `resetAll()` after a workflow completes

## Usage in DebateLoopAgent

`DebateLoopAgent` records a call after each bull and bear turn:

```java
String bullResponse = cache.getOrCompute(..., () -> bullResearcher.argue(...));
llmBudgetTracker.recordCall(ticker.content());
```

This means a 5-round debate (10 turns) uses 10 budget units. Combined with analyst reports (~4 calls) and other agents, a typical workflow uses around 15-20 calls per ticker.

## BudgetExceededException

When hard limit mode is enabled and the budget is exceeded, `BudgetExceededException` is thrown:

```java
throw new BudgetExceededException(ticker, count, budget);
// Message: "LLM call budget hard limit exceeded for AAPL: 35 calls (budget: 30)"
```

The exception exposes `getTicker()`, `getCallCount()`, and `getBudget()` for programmatic handling.

## API

| Method | Returns | Description |
|--------|---------|-------------|
| `recordCall(ticker)` | `int` | Record a call, returns current count |
| `getCallCount(ticker)` | `int` | Get current count for a ticker |
| `reset(ticker)` | `void` | Reset count for a ticker |
| `resetAll()` | `void` | Reset all counts |

## Design Notes

- **Soft limit by default:** Exceeding the budget logs a warning but does not block. Enable `hard-limit: true` to enforce a circuit breaker.
- **In-memory only:** Counts are not persisted. A restart resets all counts.
- **Thread-safe:** Uses `ConcurrentHashMap` with `merge()` for atomic increments.
