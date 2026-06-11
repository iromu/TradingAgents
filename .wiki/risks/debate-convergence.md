---
title: "Debate Convergence"
type: "risk"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/TraderAgent.java"
updated_at: "2026-06-11"
---

# Debate Convergence Risk

## The Problem

The `debateInvestment()` method in `TraderAgent` uses a **fixed iteration count** (`maxIterations(2)`) rather than a convergence condition.

```java
return RepeatUntilBuilder
    .returning(InvestmentDebateState.class)
    .withMaxIterations(2)
    .repeating(...)
    .until(context -> context.lastAttempt() != null && context.lastAttempt().count() >= 2)
    .build()
    .asSubProcess(actionContext, InvestmentDebateState.class);
```

## Why It's a Risk

1. **No intelligence:** The debate always runs for exactly 2 rounds (4 turns), regardless of whether the agents have already reached consensus or are still disagreeing
2. **Hardcoded value:** The number `2` is a magic number in the code. Changing it requires code modification, not configuration
3. **Token cost:** Each additional round adds significant LLM token cost. Without a convergence check, you can't optimize for quality vs cost

## What Would Be Better

A convergence condition that checks whether the agents' positions have stabilized:
- Compare the latest response to the previous one (semantic similarity)
- Stop if the bull and bear positions have stopped changing significantly
- Fall back to max iterations as a safety net

## Impact

- **Too few rounds:** May miss important counterarguments
- **Too many rounds:** Wastes tokens on repetitive arguments
- **Fixed at 2:** A compromise that may not work well for all tickers

## Related

- `[[trading-workflow]]` — where the debate fits in the overall flow
- `[[investment-debate]]` — the debate feature page
