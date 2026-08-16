## MODIFIED Requirements

### Requirement: Decision memory stores pending decisions

After each `propagate()` call (i.e., after the pipeline completes), the system SHALL append the final trade decision to an append-only markdown log file as a **pending** entry.

The `DecisionMemoryAgent` SHALL have an action `storeDecision` that:
- Takes `Ticker`, `TradingDate`, `String rating`, `String executiveSummary`, `String investmentThesis` as inputs
- Appends a markdown entry to the memory log file with `pending` status
- Uses the format: `[YYYY-MM-DD | TICKER | Rating | pending]`
- Separates entries with a self-consistent entry separator (see "Decision memory file format")
- Uses atomic writes (temp file + rename) to prevent corruption on crash

The `TradingDate` written into a pending entry's header SHALL be a valid ISO `YYYY-MM-DD` date (never a placeholder such as `auto`), so that the pending-entry match succeeds on a subsequent run.

#### Scenario: Store a pending decision
- **WHEN** the pipeline completes with a `Buy` rating for NVDA on a known trading date
- **THEN** a pending entry is appended to the memory log with that `YYYY-MM-DD` date

#### Scenario: Store a pending decision for a different ticker
- **WHEN** the pipeline completes with a `Hold` rating for AAPL
- **THEN** a pending entry is appended to the memory log (separate from NVDA entry)

#### Scenario: Pending entry is matchable on the next run
- **WHEN** a pending entry was stored for NVDA with a real trading date
- **AND** the pipeline runs again for NVDA
- **THEN** the pending-entry match for NVDA succeeds (the entry's date field is a valid `YYYY-MM-DD` date, not a placeholder)

### Requirement: Decision memory resolves pending decisions

On the next run for the **same ticker**, the system SHALL resolve pending entries by fetching actual returns and generating an LLM reflection.

The `DecisionMemoryAgent` SHALL have an action `resolvePending` that:
- Has a `@Condition(pre = "repository.hasPendingEntriesFor(ticker)")` guard
- Is actually invoked by the pipeline when a pending entry exists for the ticker (the guard alone does not run an action that has no caller)
- Fetches actual returns for the ticker over 5 days (raw return + alpha vs a real benchmark)
- Calls the LLM (using `BEST_ROLE` model) to generate a one-paragraph reflection
- Atomically updates the log entry with returns and reflection
- Uses the resolved format: `[YYYY-MM-DD | TICKER | Rating | +X.X% | +Y.YY% | 5d]`

The `resolvePending` action SHALL use a `@Tool fetchReturns(Ticker, TradingDate)` to fetch actual returns from the data service layer (YFinService or AlphaVantageService). `fetchReturns` SHALL compute alpha as the stock's excess return over a real benchmark's return over the same window; it SHALL NOT stub alpha to the raw return or to zero when benchmark data is available.

#### Scenario: Resolve a pending decision with positive returns
- **WHEN** the pipeline runs for NVDA and a pending entry exists
- **THEN** the system invokes resolution, fetches the 5-day return and benchmark return, generates a reflection, and updates the entry

#### Scenario: Alpha is computed against a real benchmark
- **GIVEN** a pending entry with an entry date and a benchmark with returns over the same window
- **WHEN** `fetchReturns` computes the outcome
- **THEN** `alpha` equals the stock's 5-day return minus the benchmark's 5-day return (not the raw stock return)

#### Scenario: Resolve a pending decision with negative returns
- **WHEN** the pipeline runs for AAPL and a pending entry exists
- **THEN** the system fetches the 5-day return (negative), generates a reflection, and updates the entry

#### Scenario: No pending entry to resolve
- **WHEN** the pipeline runs for MSFT and no pending entry exists
- **THEN** `resolvePending` does not execute (condition not met)

### Requirement: Decision memory injects past_context

On each run, the system SHALL inject memory log context into the decision prompts via the `past_context` state key.

The `DecisionMemoryAgent` SHALL have an action `generatePastContext(Ticker ticker)` that:
- Parses the memory log file
- Extracts up to 5 most recent **same-ticker** decisions (with outcomes and reflections)
- Extracts up to 3 most recent **cross-ticker** lessons (reflections only)
- Returns a formatted string containing this context
- Binds the result to the blackboard as a `String` (or a new `PastContext` record)

The `past_context` SHALL be injected into BOTH the research-plan prompt and the final decision (`researchManager`) prompt via the `past_context` / `past_memory_str` model variable slot — the final decision prompt SHALL receive the real generated context, not a hard-coded "no past memories" placeholder.

#### Scenario: Generate past_context for a ticker with history
- **WHEN** the pipeline runs for NVDA and 5+ previous decisions exist
- **THEN** the past_context includes 5 same-ticker decisions with outcomes and reflections

#### Scenario: Generate past_context for a new ticker
- **WHEN** the pipeline runs for a ticker with no previous decisions
- **THEN** the past_context is empty (no injection needed)

#### Scenario: Generate past_context with cross-ticker lessons
- **WHEN** the pipeline runs for NVDA and cross-ticker reflections exist
- **THEN** the past_context includes up to 3 cross-ticker lessons

#### Scenario: Final decision prompt receives real past context
- **WHEN** the final decision step builds its prompt for a ticker that has prior resolved decisions
- **THEN** the prompt's `past_memory_str` reflects the generated context (same-ticker outcomes and reflections)
- **AND** it is not the hard-coded "no past memories" placeholder

### Requirement: Decision memory file format

The memory log file SHALL use the following format:

```
[2026-01-15 | NVDA | Buy | pending]

DECISION:
**Rating**: Buy

**Executive Summary**: ...

**Investment Thesis**: ...

<ENTRY_SEPARATOR>

[2026-01-15 | AAPL | Hold | +3.2% | +1.5% | 5d]

DECISION:
**Rating**: Hold

**Executive Summary**: ...

REFLECTION:
The hold decision was correct because...

<ENTRY_SEPARATOR>
```

The file SHALL use an entry separator that cannot appear in LLM prose output. The separator used when WRITING an entry SHALL be the same token recognized when PARSING, so that every entry the system writes is parseable by the system's own reader (the write and read formats SHALL round-trip).

Parsing SHALL use regex patterns matching the Python project's `_DECISION_RE` and `_REFLECTION_RE` patterns for format compatibility.

#### Scenario: Parse a pending entry
- **WHEN** the memory log contains a pending entry
- **THEN** the parser extracts ticker, date, rating, and status as `PendingDecision`

#### Scenario: Parse a resolved entry
- **WHEN** the memory log contains a resolved entry
- **THEN** the parser extracts ticker, date, rating, returns, and reflection as `ResolvedDecision`

#### Scenario: Written entry round-trips through the parser
- **WHEN** an entry is written by the system and then re-read
- **THEN** the entry is parsed back with the same ticker, date, rating, and status it was written with

## ADDED Requirements

### Requirement: Decision memory resolution is triggered on the next run

The system SHALL have a concrete trigger that invokes pending-decision resolution when the pipeline runs for a ticker that has pending entries; the `resolvePending` guard alone is insufficient because an action with no caller never runs.

#### Scenario: Resolution runs when a pending entry exists
- **GIVEN** a pending entry exists for a ticker
- **WHEN** the pipeline next runs for that ticker
- **THEN** resolution is invoked (the pending entry is resolved with returns and a reflection)

#### Scenario: Resolution is skipped when no pending entry exists
- **GIVEN** no pending entry exists for a ticker
- **WHEN** the pipeline next runs for that ticker
- **THEN** resolution is not invoked

### Requirement: Decision memory reflection is validated before persistence

The system SHALL sanitize and structurally validate the LLM-generated reflection before writing it to the memory log, so that reflection text cannot inject prompt-injection payloads into future runs that consume the memory log.

#### Scenario: Reflection containing template syntax is sanitized
- **GIVEN** an LLM reflection that includes Jinja template syntax or control characters
- **WHEN** the reflection is about to be persisted
- **THEN** the reflection is sanitized (template syntax and control characters removed) before it is written to the memory log

#### Scenario: Structurally invalid reflection is rejected or normalized
- **GIVEN** an LLM reflection that does not match the expected reflection structure
- **WHEN** the reflection is about to be persisted
- **THEN** the reflection is rejected or normalized rather than persisted raw

### Requirement: Incomplete return windows are not silently resolved

When resolving a pending decision, the system SHALL validate that the fetched price window contains a real end-date close before recording a return. If the window is incomplete (the end-date has no close — e.g. a holiday or weekend entry — or the data source returns an error string) the entry SHALL NOT be marked resolved with a fabricated 0% return / 0 alpha; it SHALL remain pending or be marked unresolvable with a reason.

#### Scenario: Complete window resolves normally
- **GIVEN** a pending entry and a price window with a valid end-date close
- **WHEN** the return is computed
- **THEN** the entry is resolved with the computed return and alpha

#### Scenario: Incomplete window is not silently resolved
- **GIVEN** a pending entry whose end-date has no close, or a data source returns an error string
- **WHEN** the return is computed
- **THEN** the entry is not marked resolved with a 0% return / 0 alpha
- **AND** it remains pending or is marked unresolvable with a reason
