# Tasks: Fix Embabel Best Practices

## Wave 1: Critical Fixes (No Risk)

- [x] **T1: Fix Jinja variable syntax in RiskManager.jinja**
  - Change line 11: `**{trader_decision}**` → `**{{ trader_decision }}**`
  - Verify no other single-brace Jinja variables in any `.jinja` file

- [x] **T2: Remove dead fields from InvestmentDebateState**
  - Remove: `riskAssessment`, `latestSpeaker`, `currentAggressiveResponse`, `currentConservativeResponse`, `currentNeutralResponse`, `traderProposal`
  - Updated `DebateLoopAgent.java` constructor call
  - Verified zero references to removed fields via grep

## Wave 2: Startup Reliability

- [x] **T3: Make AlphaVantageService conditionally loaded**
  - Added `@ConditionalOnProperty(prefix = "app.alphavantage", name = "enabled", havingValue = "true", matchIfMissing = false)`
  - Added `app.alphavantage.enabled: true` to `application.yaml`
  - VendorRouter handles null AlphaVantageService gracefully (pre-existing)
  - Removed `validateApiKey()` `@PostConstruct` method

- [x] **T4: Add default maxEntries to DecisionMemoryRepository**
  - Changed `application.yaml`: `log-max-entries: 1000` (was `0`)

- [x] **T5: Make HitlService executor a daemon thread**
  - Changed to `Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; })`

## Wave 3: Code Quality

- [x] **T6: StringBuilder in RiskDebateAgent**
  - Replaced `history += "\n" + currentXxx` with `StringBuilder` in `assessRisk()` loop

- [x] **T7: Pre-compile regex in DebateAgent.sanitizeForPrompt()**
  - Created 6 `static final Pattern` fields for all regex patterns
  - Added input length guard (rejects input > 10000 chars before regex processing)
  - Uses `Pattern.matcher(input).replaceAll(...)` sequentially (not chained String.replaceAll)

- [x] **T8: Extract ResearchPlanService**
  - Created `ResearchPlanService` with shared agent process creation logic
  - Refactored `TradingApiController` to use the service
  - Refactored `TradingHtmxController` to use the service
  - Refactored `ProcessStatusController` to use the service

- [x] **T9: Fix Boolean.parseBoolean in TradingHtmxController**
  - Replaced with `StringUtils.equalsIgnoreCase(approved, "true")`

- [x] **T10: Migrate FileCache to Path**
  - Replaced `java.io.File` with `java.nio.file.Path` throughout `FileCache`

## Wave 4: Verification

- [x] **T11: Run full test suite**
  - `./mvnw test` — 393 tests, 0 failures, 0 errors
  - `./mvnw verify` — BUILD SUCCESS

- [x] **T12: Verify Jinja fix**
  - RiskManager.jinja line 11 confirmed: `**{{ trader_decision }}**`
  - No single-brace Jinja variables found in any `.jinja` file

- [x] **T13: Verify startup without Alpha Vantage key**
  - `@ConditionalOnProperty(matchIfMissing = false)` confirmed
  - VendorRouter handles null service gracefully

- [x] **T14: Verify InvestmentDebateState fields removed**
  - `grep -r "\.latestSpeaker()\|\.currentAggressiveResponse()\|\.currentConservativeResponse()\|\.currentNeutralResponse()\|\.traderProposal()" src/main/java/`
  - Zero matches confirmed
  - InvestmentDebateState now has 7 fields (was 12)