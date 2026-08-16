## Why

Code review identified 69 issues across the TradingAgents codebase. After verifying against actual source code, ~57 real issues remain (12 were false positives — already fixed). The issues range from blocking test failures to security vulnerabilities, null safety gaps, and missing AOT hints that prevent GraalVM native image builds. This change addresses all confirmed issues in a single, phased remediation effort.

## What Changes

### Phase 1: Unbreak tests (BLOCKING)
- Fix `TraderAgentConfig` constructor calls in 3 test files — the record grew from 13 to 16 fields (added `researchTokenBudget`, `provider`, `bestModel`, `cheapestModel`, and 3 provider config records), but tests still pass the old argument count
- Affected files: `DebateLoopAgentTest.java`, `TraderAgentConfigTest.java`, `DebateLoopAgentIntegrationTest.java`

### Phase 2: Security hardening (CRITICAL)
- Add `spring-boot-starter-security` dependency with minimal configuration (session-scoped auth for HITL endpoints)
- Enable CSRF protection for all POST endpoints
- Add input validation and length limits on user feedback in all controllers
- Add process ownership verification to prevent unauthorized access via known `processId`

### Phase 3: Input validation & null safety (HIGH)
- Add `Objects.requireNonNull(briefs)` guard in `DebateLoopAgent.debate()`
- Add null/blank validation on `ticker` and `researchPlan` in `Trader.traderProposal()`
- Add null-safe cast in `CheckpointStore.restore()` for `map.get("ticker")`
- Add `@Valid` annotations on `TraderAgentConfig` nested provider config properties

### Phase 4: DebateLoopAgent hardening (HIGH)
- Make `RiskDebateAgent.MAX_RISK_DEBATE_ROUNDS` configurable via `TraderAgentConfig.maxRiskDebateRounds`
- Fix cache key collision risk by adding namespace delimiter prefix (e.g., `debate:bull:` prefix)
- Eliminate duplicate `computeSimilarity()` calls (currently called twice per iteration — once for logging, once for convergence)
- Improve `extractRating()` to handle conflicting buy/sell keywords (currently first-match-wins)
- Improve `extractThesis()` heuristic robustness (currently fragile against LLM output variations)
- Fix `CODE_FENCE_UNCLOSED` regex to non-greedy (`.*?$` → `.*?$` with `MULTILINE` flag already, but make non-greedy explicit)

### Phase 5: AOT hints (MEDIUM)
- Register `TraderProposalOutput`, `PortfolioDecisionOutput`, `BudgetExceededException`, `RiskAssessmentOutput`, and `SubtractIndicator` in AOT runtime hints

### Phase 6: Data quality & miscellaneous (MEDIUM)
- Fix `YFinService` NaN propagation — replace `Double.NaN` with `null` in `DecimalNum.valueOf()` calls
- Convert `BudgetExceededException` from class to record
- Add `@ConditionalOnProperty` toggle to `FundamentalDataTools` (like `FredDataTools` and `PolymarketDataTools`)
- Replace `Thread.sleep()` in `InstrumentIdentityAgent` with scheduled retry
- Fix `PolymarketService` fallback probability display (shows "N/A" string in numeric column)
- Convert `CheckpointAgent.restoreCheckpoint()` return from raw `Map<String, Object>` to typed record
- Sanitize flash messages in `TradingHtmxController`
- Fix `HitlService` LRU eviction edge case (unbounded growth when no sessions are expired)
- Manage `HitlService` scheduled executor as Spring-managed bean

## Capabilities

### New Capabilities
- `security`: Endpoint security with CSRF protection, input validation on user feedback, and process ownership verification
- `test-infrastructure`: Correct test compilation with up-to-date `TraderAgentConfig` constructor signatures

### Modified Capabilities
- `agent-orchestration`: Configurable risk debate rounds, null safety on debate inputs, cache key namespace delimiters, duplicate similarity elimination
- `agent-quality`: Improved rating/thesis extraction robustness, non-greedy regex patterns, AOT hint registrations
- `reliability-fixes`: NaN handling in YFinService, BudgetExceededException as record, Thread.sleep replacement, Flash message sanitization, HitlService LRU eviction fix

## Impact

### Affected code
- **Test files**: 3 test files with constructor fixes
- **Security**: New `SecurityConfig.java`, `pom.xml` dependency addition
- **Controllers**: Input validation in `TradingApiController`, `TradingHtmxController`, `ProcessStatusController`
- **Agents**: `DebateLoopAgent`, `RiskDebateAgent`, `Trader`, `CheckpointAgent`, `InstrumentIdentityAgent`
- **Configuration**: `TraderAgentConfig` (new field: `maxRiskDebateRounds`), `HitlConfig`
- **Data services**: `YFinService`, `PolymarketService`, `FundamentalDataTools`
- **AOT**: `TraderAgentRuntimeHintsRegistrar.java`
- **Utilities**: `BudgetExceededException.java` (class → record)

### Dependencies
- `spring-boot-starter-security` (new)

### Systems
- All POST endpoints now require CSRF tokens
- HITL endpoints require process ownership verification
- Native image builds now include missing reflection hints
