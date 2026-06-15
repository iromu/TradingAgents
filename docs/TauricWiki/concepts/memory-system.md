---
title: "Memory System"
type: "concept"
status: "active"
language: "default"
source_paths: [tradingagents/agents/utils/memory.py, tradingagents/graph/reflection.py]
updated_at: "2026-06-14"
---

# Memory System

TradingAgents has an append-only markdown decision log that enables the system to learn from past mistakes over time.

## Two-phase design

### Phase A: Store

After each `propagate()` call, the final trade decision is appended to `~/.tradingagents/memory/trading_memory.md` as a **pending** entry:

```
[2026-01-15 | NVDA | Buy | pending]

DECISION:
**Rating**: Buy

**Executive Summary**: ...
```

No LLM call is made at this stage — just a raw append.

### Phase B: Resolve

On the next run for the **same ticker**, the system:

1. Fetches actual returns for each pending entry (raw return over 5 days, alpha vs benchmark)
2. Calls the LLM to generate a **reflection** — a one-paragraph analysis of what went right/wrong
3. Atomically updates the log entry with returns and reflection

```
[2026-01-15 | NVDA | Buy | +3.2% | +1.5% | 5d]

DECISION:
**Rating**: Buy

**Executive Summary**: ...

REFLECTION:
The buy signal was driven by strong technical indicators...
```

## Prompt injection

On each run, the memory log context is injected into the Portfolio Manager's prompt via the `past_context` state key. It includes:

- Up to 5 most recent **same-ticker** decisions (with outcomes and reflections)
- Up to 3 most recent **cross-ticker** lessons (reflections only)

This lets the Portfolio Manager learn from what worked and what didn't.

## Implementation details

- **Path**: Default `~/.tradingagents/memory/trading_memory.md`. Override with `TRADINGAGENTS_MEMORY_LOG_PATH`.
- **Atomic writes**: Updates use temp-file + `os.replace()` to prevent corruption on crash.
- **Idempotency**: Storing a decision checks for existing pending entries to avoid duplicates.
- **Rotation**: Optional `memory_log_max_entries` cap prunes oldest resolved entries. Pending entries are never pruned.
- **Format**: Entries are separated by `<!-- ENTRY_END -->` HTML comments (safe — can't appear in LLM output).

## Reflection

The `Reflector` class (`graph/reflection.py`) generates reflections via the LLM. It receives:

- The final decision text
- Raw return (e.g., `+0.032`)
- Alpha return vs benchmark (e.g., `+0.015`)
- Benchmark name (e.g., "SPY")
