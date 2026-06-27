# Tasks: Fix Embabel Best Practices

## Wave 1: Critical Fixes (No Risk)

- [ ] **T1: Fix Jinja variable syntax in RiskManager.jinja**
  - Change line 11: `**{trader_decision}**` → `**{{ trader_decision }}**`
  - Verify no other single-brace Jinja variables in any `.jinja` file

- [ ] **T2: Remove dead fields from InvestmentDebateState**
  - Remove: `riskAssessment`, `latestSpeaker`, `currentAggressiveResponse`, `currentConservativeResponse`, `currentNeutralResponse`, `traderProposal`
  - Update `DebateLoopAgent.java` constructor call (line 88)
  - Verify zero references to removed fields via grep

## Wave 2: Startup Reliability

- [ ] **T3: Make AlphaVantageService conditionally loaded**
  - Add `@ConditionalOnProperty(prefix = "app.alphavantage", name = "enabled", havingValue = "true", matchIfMissing = false)`
  - Add `app.alphavantage.enabled: true` to `application.yaml`
  - When disabled, all methods return `"NO_DATA_AVAILABLE"` (matching FredService)
  - Remove `validateApiKey()` `@PostConstruct` method

- [ ] **T4: Add default maxEntries to DecisionMemoryRepository**
  - Change `application.yaml`: `log-max-entries: 1000` (was `0`)
  - Document that `0` means "unlimited"

- [ ] **T5: Make HitlService executor a daemon thread**
  - Change `Executors.newSingleThreadScheduledExecutor()` to daemon variant

## Wave 3: Code Quality

- [ ] **T6: StringBuilder in RiskDebateAgent**
  - Replace `history += "\n" + currentXxx` with `StringBuilder` in `assessRisk()` loop

- [ ] **T7: Pre-compile regex in DebateAgent.sanitizeForPrompt()**
  - Create `static final Pattern` fields for the 6 regex patterns
  - Add input length guard (> 10000 chars rejected before regex processing)

- [ ] **T8: Extract ResearchPlanService**
  - Create `ResearchPlanService` with shared agent process creation logic
  - Refactor `TradingApiController` to use the service
  - Refactor `TradingHtmxController` to use the service
  - Refactor `ProcessStatusController` to use the service

- [ ] **T9: Fix Boolean.parseBoolean in TradingHtmxController**
  - Replace with case-insensitive check: `StringUtils.equalsIgnoreCase(approved, "true")`

- [ ] **T10: Migrate FileCache to Path**
  - Replace `java.io.File` with `java.nio.file.Path` throughout `FileCache`

## Wave 4: Verification

- [ ] **T11: Run full test suite**
  - `./mvnw test` — all unit tests pass
  - `./mvnw verify` — build passes, integration tests excluded by default

- [ ] **T12: Verify Jinja fix**
  - Read prompt output from RiskManager.jinja to confirm `{{ trader_decision }}` renders correctly

- [ ] **T13: Verify startup without Alpha Vantage key**
  - Start app without `app.alphavantage.api-key` configured
  - Confirm app starts with a warning, not an exception

- [ ] **T14: Verify InvestmentDebateState fields removed**
  - `grep -r "\.latestSpeaker()\|\.currentAggressiveResponse()\|\.currentConservativeResponse()\|\.currentNeutralResponse()\|\.traderProposal()" src/main/java/`
  - Expected: zero matches