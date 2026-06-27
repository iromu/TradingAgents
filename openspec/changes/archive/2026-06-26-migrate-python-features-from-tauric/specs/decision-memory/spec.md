# Spec: Decision Memory

## ADDED Requirements

### Requirement: Decision memory stores pending decisions

After each `propagate()` call (i.e., after the pipeline completes), the system SHALL append the final trade decision to an append-only markdown log file as a **pending** entry.

The `DecisionMemoryAgent` SHALL have an action `storeDecision` that:
- Takes `Ticker`, `TradingDate`, `String rating`, `String executiveSummary`, `String investmentThesis` as inputs
- Appends a markdown entry to the memory log file with `pending` status
- Uses the format: `[YYYY-MM-DD | TICKER | Rating | pending]`
- Separates entries with `<!-- ENTRY_END -->` HTML comments
- Uses atomic writes (temp file + rename) to prevent corruption on crash

#### Scenario: Store a pending decision
- **WHEN** the pipeline completes with a `Buy` rating for NVDA
- **THEN** a pending entry is appended to the memory log

#### Scenario: Store a pending decision for a different ticker
- **WHEN** the pipeline completes with a `Hold` rating for AAPL
- **THEN** a pending entry is appended to the memory log (separate from NVDA entry)

### Requirement: Decision memory resolves pending decisions

On the next run for the **same ticker**, the system SHALL resolve pending entries by fetching actual returns and generating an LLM reflection.

The `DecisionMemoryAgent` SHALL have an action `resolvePending` that:
- Has a `@Condition(pre = "repository.hasPendingEntriesFor(ticker)")` guard
- Fetches actual returns for the ticker over 5 days (raw return + alpha vs benchmark)
- Calls the LLM (using `BEST_ROLE` model) to generate a one-paragraph reflection
- Atomically updates the log entry with returns and reflection
- Uses the resolved format: `[YYYY-MM-DD | TICKER | Rating | +X.X% | +Y.YY% | 5d]`

The `resolvePending` action SHALL use a `@Tool fetchReturns(Ticker, TradingDate)` to fetch actual returns from the data service layer (YFinService or AlphaVantageService).

#### Scenario: Resolve a pending decision with positive returns
- **WHEN** the pipeline runs for NVDA and a pending entry exists
- **THEN** the system fetches the 5-day return, generates a reflection, and updates the entry

#### Scenario: Resolve a pending decision with negative returns
- **WHEN** the pipeline runs for AAPL and a pending entry exists
- **THEN** the system fetches the 5-day return (negative), generates a reflection, and updates the entry

#### Scenario: No pending entry to resolve
- **WHEN** the pipeline runs for MSFT and no pending entry exists
- **THEN** `resolvePending` does not execute (condition not met)

### Requirement: Decision memory injects past_context

On each run, the system SHALL inject memory log context into the Portfolio Manager's prompt via the `past_context` state key.

The `DecisionMemoryAgent` SHALL have an action `generatePastContext(Ticker ticker)` that:
- Parses the memory log file
- Extracts up to 5 most recent **same-ticker** decisions (with outcomes and reflections)
- Extracts up to 3 most recent **cross-ticker** lessons (reflections only)
- Returns a formatted string containing this context
- Binds the result to the blackboard as a `String` (or a new `PastContext` record)

The `past_context` SHALL be injected into the `ResearchManager.jinja` template via the existing `past_context` model variable slot.

#### Scenario: Generate past_context for a ticker with history
- **WHEN** the pipeline runs for NVDA and 5+ previous decisions exist
- **THEN** the past_context includes 5 same-ticker decisions with outcomes and reflections

#### Scenario: Generate past_context for a new ticker
- **WHEN** the pipeline runs for a ticker with no previous decisions
- **THEN** the past_context is empty (no injection needed)

#### Scenario: Generate past_context with cross-ticker lessons
- **WHEN** the pipeline runs for NVDA and cross-ticker reflections exist
- **THEN** the past_context includes up to 3 cross-ticker lessons

### Requirement: Decision memory file format

The memory log file SHALL use the following format:

```
[2026-01-15 | NVDA | Buy | pending]

DECISION:
**Rating**: Buy

**Executive Summary**: ...

**Investment Thesis**: ...

<!-- ENTRY_END -->

[2026-01-15 | AAPL | Hold | +3.2% | +1.5% | 5d]

DECISION:
**Rating**: Hold

**Executive Summary**: ...

REFLECTION:
The hold decision was correct because...

<!-- ENTRY_END -->
```

The file SHALL use `<!-- ENTRY_END -->` HTML comments as entry separators (chosen because this cannot appear in LLM prose output).

Parsing SHALL use regex patterns matching the Python project's `_DECISION_RE` and `_REFLECTION_RE` patterns for format compatibility.

#### Scenario: Parse a pending entry
- **WHEN** the memory log contains a pending entry
- **THEN** the parser extracts ticker, date, rating, and status as `PendingDecision`

#### Scenario: Parse a resolved entry
- **WHEN** the memory log contains a resolved entry
- **THEN** the parser extracts ticker, date, rating, returns, and reflection as `ResolvedDecision`

### Requirement: Decision memory atomic writes

All writes to the memory log file SHALL use atomic writes to prevent corruption:
- Write to a temporary file first
- Rename the temp file to the target path
- This ensures the log file is never in a partially-written state

If the process crashes during a write, the next run SHALL detect the corruption and recover by truncating to the last complete `<!-- ENTRY_END -->` separator.

#### Scenario: Atomic write on success
- **WHEN** `storeDecision` completes
- **THEN** the memory log file contains the new entry with no corruption

#### Scenario: Recover from partial write
- **WHEN** the memory log file is corrupted (partial entry at end)
- **THEN** the system recovers by truncating to the last complete entry

### Requirement: Decision memory configuration

Memory log behavior SHALL be controlled by configuration:

| Config key | Default | Description |
|---|---|---|
| `app.memory.log-path` | `~/.tradingagents/memory/trading_memory.md` | Path to the memory log file |
| `app.memory.log-max-entries` | `None` | Max resolved entries (None = no limit) |
| `app.memory.resolve-on-next-run` | `true` | Automatically resolve pending on next run |

#### Scenario: Custom memory log path
- **WHEN** `app.memory.log-path` is set to `/tmp/memory.md`
- **THEN** all memory operations use `/tmp/memory.md`

#### Scenario: Memory log entry limit
- **WHEN** `app.memory.log-max-entries` is set to 50
- **THEN** the system prunes oldest resolved entries when the count exceeds 50
- **THEN** pending entries are never pruned

## REMOVED Requirements

None.
