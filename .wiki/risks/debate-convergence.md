---
title: "Debate Convergence"
type: "risk"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/TraderAgent.java"
  - "src/main/java/com/embabel/gekko/config/TraderAgentConfig.java"
updated_at: "2026-06-13"
---

# Debate Convergence Risk

## The Problem

The `debateInvestment()` method in `TraderAgent` uses a **fixed iteration count** rather than a semantic convergence condition.

## Current State

The fixed count is now configurable:

| Property | Default | Purpose |
|----------|---------|---------|
| `app.llm-options.max-debate-iterations` | 5 | Max rounds for investment debate |
| `app.llm-options.similarity-threshold` | 0.8 | (planned) Threshold for convergence detection |

## Why It's Still a Risk

1. **No intelligence:** The debate always runs for the configured number of rounds regardless of whether agents have reached consensus
2. **Token cost:** Each additional round adds significant LLM token cost
3. **similarityThreshold not yet used:** The `similarityThreshold` config exists but has not been wired into a convergence check

## What Would Be Better

A convergence condition that checks whether the agents' positions have stabilized:
- Compare the latest response to the previous one (semantic similarity)
- Stop if the bull and bear positions have stopped changing significantly
- Fall back to `maxDebateIterations` as a safety net

## Impact

- **Too few rounds:** May miss important counterarguments
- **Too many rounds:** Wastes tokens on repetitive arguments
- **Fixed count:** A compromise that may not work well for all tickers

## Related

- `[[trading-workflow]]` — where the debate fits in the overall flow
- `[[investment-debate]]` — the debate feature page
- `[[agent-configuration]]` — where to adjust iteration count
