## 1. Reliability Fixes — Cache Key Bugs

- [x] 1.1 Fix `AlphaVantageService.getGlobalNews()` cache key to include `dateFrom` and `dateTo` parameters
- [x] 1.2 Fix `AlphaVantageService.getInsiderSentiment()` cache key to include all query parameters
- [x] 1.3 Audit all other AlphaVantageService methods for missing cache key parameters (verify each method)
- [x] 1.4 Add unit tests for AlphaVantageService cache key generation (verify different params produce different keys)

## 2. Reliability Fixes — HTTP Timeouts

- [x] 2.1 Add `readTimeoutMs` config field to `AlphaVantageService` with default 30000ms
- [x] 2.2 Configure `SimpleClientHttpRequestFactory` with both connect and read timeouts in `AlphaVantageService`
- [x] 2.3 Add `@Value` injection for `app.alphavantage.read-timeout-ms` in `application.yaml` (or ensure it's documented)
- [x] 2.4 Add unit test verifying RestTemplate has non-null timeout configuration

## 3. Reliability Fixes — FileCache Race Condition

- [x] 3.1 Analyze `FileCache.getOrCompute()` to identify the exact race window
- [x] 3.2 Implement per-key locking using `ConcurrentHashMap.computeIfAbsent()` with double-check pattern
- [x] 3.3 Add atomic write (write to temp file, then rename) to prevent partial read corruption
- [x] 3.4 Ensure lock is released on exception (try-finally or synchronized block)
- [x] 3.5 Add concurrent unit test: two threads requesting same key compute exactly once
- [x] 3.6 Add concurrent unit test: different keys compute independently

## 4. Agent Quality — Debate Convergence

- [x] 4.1 Create `computeSimilarity(String a, String b)` method using Jaccard similarity on bigrams
- [x] 4.2 Add `similarityThreshold` config field to `TraderAgentConfig` with default 0.8
- [x] 4.3 Add `maxIterations` config field to `TraderAgentConfig` with default 5
- [x] 4.4 Modify `debateInvestment()` to check convergence after each bull/bear pair
- [x] 4.5 Update `until()` predicate to check both convergence and max iterations
- [x] 4.6 Log similarity score after each iteration for debugging
- [x] 4.7 Add unit test: debate stops when responses are identical (similarity = 1.0)
- [x] 4.8 Add unit test: debate continues when responses differ significantly

## 5. Agent Quality — Risk Debate Integration

- [x] 5.1 Create `RiskAssessment` record: `{ level: RiskLevel, reasoning: String }` with enum `RiskLevel { RISKY, NEUTRAL, CONSERVATIVE }`
- [x] 5.2 Add `riskAssessment` field to `InvestmentDebateState`
- [x] 5.3 Create `RiskDebateService` component (or add to existing agent structure) with methods for Aggressive/Conservative/Neutral debaters
- [x] 5.4 Wire risk debate into `TraderAgent` pipeline between `debateInvestment()` and `waitForReview()`
- [x] 5.5 Pass risk assessment to `ResearchManager.jinja` prompt via template model
- [x] 5.6 Add unit test: risk debate produces a `RiskAssessment` result
- [x] 5.7 Add unit test: risk assessment is included in final debate state

## 6. Agent Quality — MarketAnalyst Tools

- [x] 6.1 Create `MarketDataTools.java` with `@Tool` methods: `get_stock_data(String ticker)` and `get_indicators(String ticker, String indicators)`
- [x] 6.2 Implement `get_stock_data` to call `YFinService` and return price data as JSON
- [x] 6.3 Implement `get_indicators` to call `YFinService` TA4J calculations and return results as JSON
- [x] 6.4 Uncomment and wire `tool_names` and `withToolObject` in `TraderAgent.generateMarketReport()`
- [x] 6.5 Register `MarketDataTools` as a Spring bean
- [x] 6.6 Add unit test: `MarketDataTools.get_stock_data()` returns valid JSON for a known ticker
- [x] 6.7 Add unit test: `MarketDataTools.get_indicators()` returns indicator calculations

## 7. Agent Quality — Prompt File Unification

- [x] 7.1 Rename `prompts/analysts/FundamentalsAnalyst.txt` → `FundamentalsAnalyst.jinja`
- [x] 7.2 Rename `prompts/analysts/MarketAnalyst.txt` → `MarketAnalyst.jinja`
- [x] 7.3 Rename `prompts/analysts/NewsAnalyst.txt` → `NewsAnalyst.jinja`
- [x] 7.4 Rename `prompts/analysts/SocialMediaAnalyst.txt` → `SocialMediaAnalyst.jinja`
- [x] 7.5 Update `@Value` resource references in `TraderAgent` from `.txt` to `.jinja`
- [x] 7.6 Verify all four analyst prompts still load correctly at startup
- [x] 7.7 Verify no `.txt` files remain in `prompts/`

## 8. Test Coverage — TraderAgent

- [x] 8.1 Add test: `tickerFromForm()` with valid input returns correct Ticker
- [x] 8.2 Add test: `tickerFromForm()` rejects invalid formats (special chars, too long)
- [x] 8.3 Add test: `tickerFromForm()` rejects blank/null input
- [x] 8.4 Add test: `prepareDebateBriefs()` validates all four report inputs
- [x] 8.5 Add test: `prepareDebateBriefs()` throws on null report
- [x] 8.6 Add test: `prepareDebateBriefs()` throws on blank report content
- [x] 8.7 Add test: `distill()` produces non-empty brief for valid input
- [x] 8.8 Add test: `streamWithTemplate()` renders template with correct model variables

## 9. Test Coverage — Researcher Agents

- [x] 9.1 Add test: `BullResearcher.argue()` returns non-empty response with valid briefs
- [x] 9.2 Add test: `BearResearcher.argue()` returns non-empty response with valid briefs
- [x] 9.3 Add test: `BullResearcher.argue()` includes brief content in response
- [x] 9.4 Add test: `BearResearcher.argue()` includes brief content in response

## 10. Test Coverage — Data Flow Services

- [x] 10.1 Add test: `YFinService.getBarSeries()` returns non-null for valid ticker
- [x] 10.2 Add test: `YFinService` throws on invalid ticker
- [x] 10.3 Add test: `VendorRouter.route()` delegates to correct service for known method
- [x] 10.4 Add test: `VendorRouter.route()` throws on unknown method name

## 11. Test Coverage — Integration Tests

- [x] 11.1 Create `TraderAgentIntegrationTest` with mock Alpha Vantage and Yahoo Finance responses
- [x] 11.2 Test full pipeline: ticker → analysts → briefs → debate → plan (with mocks)
- [x] 11.3 Test pipeline with API failure: verify graceful exception
- [x] 11.4 Test pipeline with cache hit: verify second run uses cached data

## 12. Verification — Build and Smoke Test

- [x] 12.1 Run `./mvnw verify` to ensure all tests pass
- [x] 12.2 Verify the application starts without errors
- [x] 12.3 Verify all prompt templates load correctly
- [x] 12.4 Run a quick end-to-end test with a known ticker (e.g., AAPL)
