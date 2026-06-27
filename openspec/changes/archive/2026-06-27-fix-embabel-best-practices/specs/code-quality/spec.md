# Spec: Code Quality Improvements

## Why
Multiple medium/low severity code quality issues identified by the critic and reviewer agents that degrade maintainability, correctness, or performance.

## What Changes

### 1. StringBuilder in RiskDebateAgent
- Replace `history += "\n" + currentXxx` with `StringBuilder` in `RiskDebateAgent.assessRisk()` loop

### 2. Dead fields in InvestmentDebateState
- Remove 5 unused fields from `InvestmentDebateState`: `latestSpeaker`, `currentAggressiveResponse`, `currentConservativeResponse`, `currentNeutralResponse`, `traderProposal`
- These fields are always `""` or `null` and never read anywhere in the codebase

### 3. Boolean.parseBoolean robustness
- Replace `Boolean.parseBoolean(approved)` with case-insensitive check in `TradingHtmxController.submitPlanApproval()`
- Accept "true", "TRUE", "yes", "1" as approval

### 4. FileCache migration to Path
- Replace `java.io.File` with `java.nio.file.Path` in `FileCache` for consistency with rest of codebase

### 5. DecisionMemoryRepository maxEntries default
- Change default `log-max-entries` from `0` (unlimited) to `1000` to prevent unbounded file growth

### 6. ReDoS mitigation in sanitizeForPrompt
- Pre-compile regex patterns as `static final Pattern` fields
- Add input length guard (reject input > 10000 chars before regex processing)

## Acceptance Criteria
- [ ] `RiskDebateAgent.assessRisk()` uses `StringBuilder` for history accumulation
- [ ] `InvestmentDebateState` record has 7 fields (not 12) — dead fields removed
- [ ] All references to removed `InvestmentDebateState` fields are removed
- [ ] `TradingHtmxController.submitPlanApproval()` accepts "true", "TRUE", "yes", "1" as approval
- [ ] `FileCache` uses `java.nio.file.Path` throughout
- [ ] Default `log-max-entries` is 1000
- [ ] `sanitizeForPrompt()` pre-compiles regex patterns
- [ ] `sanitizeForPrompt()` rejects input > 10000 chars before regex processing
- [ ] Build passes (`./mvnw verify`)
- [ ] All existing tests pass