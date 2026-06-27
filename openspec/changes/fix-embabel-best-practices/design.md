# Design: Fix Embabel Best Practices

## Decisions

### D1: Jinja Bug Fix — Simple Syntax Correction

**Decision:** Change `{trader_decision}` to `{{ trader_decision }}` in `RiskManager.jinja` line 11.

**Rationale:** This is a straightforward syntax fix. The variable is correctly passed in the model map (`RiskDebateAgent.java:150`). No code changes needed.

### D2: Remove Dead Fields from InvestmentDebateState

**Decision:** Remove 5 unused fields from `InvestmentDebateState` record:
- `riskAssessment` (always null)
- `latestSpeaker` (always "")
- `currentAggressiveResponse` (always "")
- `currentConservativeResponse` (always "")
- `currentNeutralResponse` (always "")
- `traderProposal` (always "")

**Rationale:** These fields are from the Python `RiskDebateState` port and were never wired up. They add cognitive load and memory waste. The `InvestmentDebateState` is used only by the bull/bear debate loop (`DebateLoopAgent`), not by the risk debate system.

**Migration concern:** Cached `InvestmentDebateState` objects on disk (in `data/llm/cache/`) will fail to deserialize with fewer fields. Mitigation: the cache key includes the iteration count, so stale cache entries from before this change will simply be recomputed. Jackson's `FAIL_ON_UNKNOWN_PROPERTIES` is already disabled.

### D3: Alpha Vantage — Conditional Loading

**Decision:** Use `@ConditionalOnProperty` to make `AlphaVantageService` optional.

```java
@ConditionalOnProperty(prefix = "app.alphavantage", name = "enabled", havingValue = "true", matchIfMissing = false)
@Service
public class AlphaVantageService { ... }
```

Add `app.alphavantage.enabled: true` to `application.yaml` (currently missing, defaults to false).

When disabled, all methods return `"NO_DATA_AVAILABLE"` string.

**Rationale:** Matches the pattern already used by `FredService` (returns `"NO_DATA_AVAILABLE"` when FRED key is missing). The `VendorRouter` already handles error strings gracefully.

### D4: Extract ResearchPlanService

**Decision:** Create a new `ResearchPlanService` that encapsulates the shared agent process creation logic currently duplicated across `TradingApiController`, `TradingHtmxController`, and `ProcessStatusController`.

```java
@Service
public class ResearchPlanService {
    AgentProcess createProcess(AgentPlatform platform, String ticker, String feedback);
    void approveProcess(AgentPlatform platform, String processId, boolean approved, String feedback);
    String extractPlanContent(AgentProcess process);
    String extractInvestmentPlan(AgentProcess process);
}
```

**Rationale:** Reduces ~60 lines of duplicated code across 3 controllers. Each controller then becomes a thin adapter (HTTP → service → response).

### D5: StringBuilder in RiskDebateAgent

**Decision:** Replace `history += "\n" + response` with `StringBuilder`.

**Rationale:** At 3 rounds × 3 debators = 9 concatenations, the performance impact is negligible. But using `StringBuilder` is the correct pattern and prevents future regression if the loop count increases.

### D6: Pre-compile Regex Patterns in sanitizeForPrompt

**Decision:** Pre-compile the 6 regex patterns in `DebateAgent.sanitizeForPrompt()` as `static final Pattern` fields.

**Rationale:** Currently each call to `sanitizeForPrompt` compiles 6 regex patterns. With user feedback potentially flowing through this on every HITL review, pre-compilation saves repeated compilation overhead. Also adds input length guard (reject input > 10000 chars before regex processing) to mitigate ReDoS risk.

### D7: Boolean Parsing — Explicit Check

**Decision:** Replace `Boolean.parseBoolean(approved)` with `StringUtils.equalsIgnoreCase(approved, "true")` or an explicit enum parse.

**Rationale:** `Boolean.parseBoolean()` only accepts exact "true" (case-insensitive). Users sending "TRUE", "yes", "1" will silently reject. An explicit check is more predictable and testable.

### D8: FileCache Migration to Path

**Decision:** Replace `java.io.File` with `java.nio.file.Path` in `FileCache` for consistency with rest of codebase.

**Rationale:** `CheckpointStore`, `DecisionMemoryRepository`, and most other classes use `Path`. `FileCache` is the last holdout. No functional impact — purely for consistency.

## Architecture

```
Before:
  TradingApiController ──┐
  TradingHtmxController ─┼── AgentProcess creation (duplicated)
  ProcessStatusController┘

After:
  TradingApiController ──┐
  TradingHtmxController ─┼── ResearchPlanService (shared)
  ProcessStatusController┘
```

## File Changes Summary

| File | Change |
|------|--------|
| `RiskManager.jinja` | `{trader_decision}` → `{{ trader_decision }}` |
| `ResearchTypes.java` | Remove 5 dead fields from `InvestmentDebateState` |
| `AlphaVantageService.java` | Add `@ConditionalOnProperty`, graceful degradation |
| `RiskDebateAgent.java` | `StringBuilder` for history |
| `DebateAgent.java` | Pre-compile regex patterns, input length guard |
| `TradingHtmxController.java` | Extract to `ResearchPlanService`, explicit boolean check |
| `TradingApiController.java` | Extract to `ResearchPlanService` |
| `FileCache.java` | Migrate `File` → `Path` |
| `application.yaml` | Add `app.alphavantage.enabled: true`, default `log-max-entries: 1000` |
| `HitlService.java` | Daemon thread for scheduled executor |
| **New:** `ResearchPlanService.java` | Shared service for controller duplication |