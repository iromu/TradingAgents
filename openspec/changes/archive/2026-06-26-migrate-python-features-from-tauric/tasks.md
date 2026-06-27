# Tasks: Migrate Python Features from Tauric to Java

## Phase 1: Instrument Identity (2-3 hours)

### 1.1 Create InstrumentContext record
- [x] 1.1.1 Create `InstrumentContext.java` in `agent/identity/` package
- [x] 1.1.2 Add fields: `ticker`, `companyName`, `sector`, `industry`, `exchange`, `currency`
- [x] 1.1.3 Make it a Java record (immutable, serializable)
- [x] 1.1.4 Add `@JsonIgnore` annotations if needed for JSON serialization

### 1.2 Create InstrumentIdentityAgent
- [x] 1.2.1 Create `InstrumentIdentityAgent.java` with `@Agent` annotation
- [x] 1.2.2 Implement `resolveIdentity(Ticker ticker)` → `InstrumentContext`
- [x] 1.2.3 Use `YFinService.getTickerInfo()` for data fetching
- [x] 1.2.4 Cache results via `FileCache` with 24-hour TTL
- [x] 1.2.5 Add `@Condition(pre = "tickerFormatIsValid(ticker)")` guard
- [x] 1.2.6 Handle exceptions gracefully (return null, not throw)
- [x] 1.2.7 Implement `validateTicker(Ticker input)` → `boolean`
- [x] 1.2.8 Add `@Autowired` fields for `YFinService` and `FileCache`

### 1.3 Create InstrumentContextPromptContributor
- [x] 1.3.1 Create `InstrumentContextPromptContributor.java` in `agent/identity/`
- [x] 1.3.2 Implement `PromptContributor` interface
- [x] 1.3.3 Read `InstrumentContext` from blackboard
- [x] 1.3.4 Inject system message with company name, sector, industry, exchange
- [x] 1.3.5 Include "Do not confuse with other companies" warning
- [x] 1.3.6 Skip injection if `InstrumentContext` is null (fail-open)
- [x] 1.3.7 Register as Spring `@Component`

### 1.4 Wire into OrchestratorAgent
- [x] 1.4.1 Add `@Autowired InstrumentIdentityAgent identityAgent` to `OrchestratorAgent`
- [x] 1.4.2 Call `identityAgent.resolveIdentity(ticker)` after `tickerFromForm()`
- [x] 1.4.3 Bind result to blackboard (Embelabel auto-binds return values)
- [x] 1.4.4 Verify: check that instrument context appears in agent prompts

### 1.5 Test instrument identity
- [x] 1.5.1 Create `InstrumentIdentityAgentTest.java` with `FakeOperationContext`
- [x] 1.5.2 Test successful resolution of AAPL
- [x] 1.5.3 Test caching (second call returns cached result)
- [x] 1.5.4 Test failure handling (YFinService throws → returns null)
- [x] 1.5.5 Test validation (empty ticker → returns false)

---

## Phase 2: Decision Memory (4-6 hours)

### 2.1 Create domain records
- [x] 2.1.1 Create `PendingDecision.java` in `agent/memory/`
- [x] 2.1.2 Create `ResolvedDecision.java` in `agent/memory/`
- [x] 2.1.3 Add all required fields (ticker, date, rating, returns, reflection, etc.)
- [x] 2.1.4 Make them Java records

### 2.2 Create DecisionMemoryRepository
- [x] 2.2.1 Create `DecisionMemoryRepository.java` in `agent/memory/`
- [x] 2.2.2 Implement file path config (`app.memory.log-path`, default `~/.tradingagents/memory/trading_memory.md`)
- [x] 2.2.3 Implement `appendPending()` — atomic write (temp + rename)
- [x] 2.2.4 Implement `resolve()` — atomic update (read, modify, write)
- [x] 2.2.5 Implement `getPendingEntries(Ticker)` — parse with regex
- [x] 2.2.6 Implement `hasPendingEntriesFor(Ticker)` — check for pending status
- [x] 2.2.7 Implement `generatePastContext(Ticker)` — extract 5 same-ticker + 3 cross-ticker
- [x] 2.2.8 Implement `rotate()` — prune oldest entries if over `max-entries`
- [x] 2.2.9 Implement `recoverFromCorruption()` — truncate to last complete entry
- [x] 2.2.10 Use regex patterns matching Python's `_DECISION_RE` and `_REFLECTION_RE`
- [x] 2.2.11 Use `<!-- ENTRY_END -->` as separator
- [x] 2.2.12 Add `@Autowired` for config injection

### 2.3 Create DecisionMemoryAgent
- [x] 2.3.1 Create `DecisionMemoryAgent.java` with `@Agent` annotation
- [x] 2.3.2 Implement `storeDecision(Ticker, TradingDate, String rating, String summary, String thesis)` — void
- [x] 2.3.3 Implement `resolvePending(Ticker, TradingDate)` — with `@Condition(pre = "hasPendingEntries")`
- [x] 2.3.4 Implement `generatePastContext(Ticker)` → String
- [x] 2.3.5 Implement `@Tool fetchReturns(Ticker, TradingDate)` → ReturnsData
- [x] 2.3.6 Use `BEST_ROLE` model for LLM reflection
- [x] 2.3.7 Use `withId("memory-reflection")` for traceability
- [x] 2.3.8 Use `withTemplate("memory/reflection")` for prompt template
- [x] 2.3.9 Add `@Autowired` for `DecisionMemoryRepository`

### 2.4 Create reflection prompt template
- [x] 2.4.1 Create `prompts/memory/reflection.jinja`
- [x] 2.4.2 Include: decision rating, executive summary, actual returns, alpha vs benchmark
- [x] 2.4.3 Ask for one-paragraph analysis of what went right/wrong
- [x] 2.4.4 Keep it concise (LLM shouldn't write an essay)

### 2.5 Wire into existing agents
- [x] 2.5.1 Add `@Autowired DecisionMemoryAgent memoryAgent` to `DebateAgent`
- [x] 2.5.2 Call `memoryAgent.storeDecision()` after `researchManager()` completes
- [x] 2.5.3 Add `@Autowired DecisionMemoryAgent memoryAgent` to `OrchestratorAgent`
- [x] 2.5.4 Call `memoryAgent.resolvePending()` at start of pipeline (before debate)
- [x] 2.5.5 Call `memoryAgent.generatePastContext()` and bind to blackboard
- [x] 2.5.6 Update `ResearchManager.jinja` to use existing `past_context` slot

### 2.6 Test memory system
- [x] 2.6.1 Create `DecisionMemoryRepositoryTest.java`
- [x] 2.6.2 Test append pending entry
- [x] 2.6.3 Test parse pending entry
- [x] 2.6.4 Test resolve entry (with mock returns)
- [x] 2.6.5 Test generate past_context (with mock data)
- [x] 2.6.6 Test rotation (max-entries)
- [x] 2.6.7 Test atomic write (temp + rename)
- [x] 2.6.8 Test corruption recovery
- [x] 2.6.9 Create `DecisionMemoryAgentTest.java` with `FakeOperationContext`
- [x] 2.6.10 Test storeDecision action
- [x] 2.6.11 Test resolvePending action (with mocked fetchReturns)
- [x] 2.6.12 Test generatePastContext action

---

## Phase 3: Checkpoint / Resume (4-6 hours)

### 3.1 Create CheckpointStore
- [x] 3.1.1 Create `CheckpointStore.java` in `agent/checkpoint/`
- [x] 3.1.2 Implement JSON file storage (data/checkpoints/<TICKER>.json)
- [x] 3.1.3 Implement `saveCheckpoint(ticker, date, phase, blackboardJson)`
- [x] 3.1.4 Implement `getCheckpoint(ticker, date)` → `CheckpointEntry`
- [x] 3.1.5 Implement `hasCheckpoint(ticker, date)` → boolean
- [x] 3.1.6 Implement `deleteCheckpoint(ticker, date)`
- [x] 3.1.7 Use Jackson `ObjectMapper` for JSON serialization
- [x] 3.1.8 Use atomic writes (temp + rename)
- [x] 3.1.9 Add `@Autowired` for config injection (`app.checkpoint.dir`)

### 3.2 Create CheckpointAgent
- [x] 3.2.1 Create `CheckpointAgent.java` with `@Agent` annotation
- [x] 3.2.2 Implement `saveCheckpoint(Ticker, TradingDate, String phase)` — void
- [x] 3.2.3 Add `@Condition(post = "isPhaseCompleted(phase)")` guard
- [x] 3.2.4 Serialize blackboard to JSON via Jackson
- [x] 3.2.5 Implement `restoreCheckpoint(Ticker, TradingDate)` → `Map<String, Object>`
- [x] 3.2.6 Add `@Condition(pre = "store.hasCheckpoint(ticker, date)")` guard
- [x] 3.2.7 Deserialize JSON back to typed objects
- [x] 3.2.8 Return last completed phase
- [x] 3.2.9 Implement `clearCheckpoint(Ticker, TradingDate)` — void
- [x] 3.2.10 Add `@Autowired` for `CheckpointStore`

### 3.3 Wire into existing agents
- [x] 3.3.1 Add `@Autowired CheckpointAgent checkpointAgent` to `OrchestratorAgent`
- [x] 3.3.2 Call `checkpointAgent.restoreCheckpoint()` at start of `start()` action
- [x] 3.3.3 Wire result into blackboard (Embelabel auto-binds return values)
- [x] 3.3.4 Add `@Autowired CheckpointAgent checkpointAgent` to `DebateAgent`
- [x] 3.3.5 Call `checkpointAgent.saveCheckpoint()` after each phase completes
- [x] 3.3.6 Call `checkpointAgent.clearCheckpoint()` after `researchManager()` completes
- [x] 3.3.7 Skip checkpoint actions when `app.checkpoint.enabled` is false

### 3.4 Test checkpoint system
- [x] 3.4.1 Create `CheckpointStoreTest.java`
- [x] 3.4.2 Test save and restore checkpoint
- [x] 3.4.3 Test JSON serialization/deserialization
- [x] 3.4.4 Test hasCheckpoint / deleteCheckpoint
- [x] 3.4.5 Test atomic write (temp + rename)
- [x] 3.4.6 Create `CheckpointAgentTest.java` with `FakeOperationContext`
- [x] 3.4.7 Test saveCheckpoint action
- [x] 3.4.8 Test restoreCheckpoint action
- [x] 3.4.9 Test clearCheckpoint action

---

## Phase 4: Extended Data Sources (3-4 hours)

### 4.1 Create FredService
- [x] 4.1.1 Create `FredService.java` in `dataflows/`
- [x] 4.1.2 Implement `getSeries(String seriesId, int limit)` — returns markdown table
- [x] 4.1.3 Implement `getMultipleSeries(List<String> seriesIds)` — returns map of series
- [x] 4.1.4 Implement `getDashboard()` — returns standard macro indicators
- [x] 4.1.5 Use `RestTemplate` for HTTP requests
- [x] 4.1.6 Cache responses via `FileCache`
- [x] 4.1.7 Handle errors gracefully (return empty results, not exceptions)
- [x] 4.1.8 Add `@Autowired` for API key config (`app.fred.api-key`)
- [x] 4.1.9 FRED API base URL: `https://api.stlouisfed.org/fred/`

### 4.2 Create FredDataTools
- [x] 4.2.1 Create `FredDataTools.java` in `tools/`
- [x] 4.2.2 Add `@EmbelabelComponent` annotation
- [x] 4.2.3 Implement `@LlmTool getMacroIndicators(String seriesId)` → String
- [x] 4.2.4 Implement `@LlmTool getMacroDashboard()` → String
- [x] 4.2.5 Add `@ToolParam` descriptions
- [x] 4.2.6 Add `@Autowired` for `FredService`
- [x] 4.2.7 Return formatted markdown (matching existing tool output format)

### 4.3 Create PolymarketService
- [x] 4.3.1 Create `PolymarketService.java` in `dataflows/`
- [x] 4.3.2 Implement `searchMarkets(String query)` — returns markdown table
- [x] 4.3.3 Implement `getMarket(String slug)` — returns market details
- [x] 4.3.4 Use `RestTemplate` for HTTP requests
- [x] 4.3.5 Cache responses via `FileCache`
- [x] 4.3.6 Polymarket base URL: `https://clob.polymarket.com/`
- [x] 4.3.7 No API key required
- [x] 4.3.8 Handle errors gracefully

### 4.4 Create PolymarketDataTools
- [x] 4.4.1 Create `PolymarketDataTools.java` in `tools/`
- [x] 4.4.2 Add `@EmbelabelComponent` annotation
- [x] 4.4.3 Implement `@LlmTool getPredictionMarkets(String query)` → String
- [x] 4.4.4 Add `@ToolParam` description
- [x] 4.4.5 Add `@Autowired` for `PolymarketService`
- [x] 4.4.6 Return formatted markdown

### 4.5 Integrate with VendorRouter
- [x] 4.5.1 Add `MACRO_DATA` and `PREDICTION_MARKETS` to `DataSourceCategory` enum
- [x] 4.5.2 Add routing: `MACRO_DATA` → `FredService`, `PREDICTION_MARKETS` → `PolymarketService`
- [x] 4.5.3 Add config: `data_vendors.macro_data = fred`, `data_vendors.prediction_markets = polymarket`

### 4.6 Test data sources
- [x] 4.6.1 Create `FredServiceTest.java`
- [x] 4.6.2 Test getSeries (mock RestTemplate)
- [x] 4.6.3 Test getMultipleSeries (mock RestTemplate)
- [x] 4.6.4 Test error handling (429 rate limit → empty results)
- [x] 4.6.5 Create `PolymarketServiceTest.java`
- [x] 4.6.6 Test searchMarkets (mock RestTemplate)
- [x] 4.6.7 Test getMarket (mock RestTemplate)
- [x] 4.6.8 Create `FredDataToolsTest.java` with `FakeOperationContext`
- [x] 4.6.9 Test LLM tool calling
- [x] 4.6.10 Create `PolymarketDataToolsTest.java` with `FakeOperationContext`
- [x] 4.6.11 Test LLM tool calling

---

## Phase 5: Multi-Provider LLM (2-3 hours)

### 5.1 Add Maven dependencies
- [x] 5.1.1 Add `embabel-agent-starter-anthropic` to `pom.xml`
- [x] 5.1.2 Add `embabel-agent-starter-google` to `pom.xml` (declared, not yet in repo)
- [x] 5.1.3 Add `embabel-agent-starter-azure` to `pom.xml` (declared, not yet in repo)
- [x] 5.1.4 Add `embabel-agent-starter-bedrock` to `pom.xml`
- [x] 5.1.5 Add `embabel-agent-starter-ollama` to `pom.xml`
- [x] 5.1.6 Verify `embabel-agent-starter-openai-custom` is still present

### 5.2 Update configuration
- [x] 5.2.1 Add `provider` property to `TraderAgentConfig`
- [x] 5.2.2 Add `best-model` and `cheapest-model` properties
- [x] 5.2.3 Add provider-specific config: `anthropic.effort`, `google.thinking-level`, `openai.reasoning-effort`
- [x] 5.2.4 Add `app.fred.api-key` config property
- [x] 5.2.5 Add `app.memory.log-path` config property
- [x] 5.2.6 Add `app.memory.log-max-entries` config property
- [x] 5.2.7 Add `app.checkpoint.enabled` config property
- [x] 5.2.8 Add `app.checkpoint.dir` config property
- [x] 5.2.9 Update `application.yaml` with new config sections

### 5.3 Wire provider selection
- [x] 5.3.1 Update `TraderAgentConfig` to select provider based on config
- [x] 5.3.2 Update `BEST_ROLE` and `CHEAPEST_ROLE` model selection
- [x] 5.3.3 Forward provider-specific settings via `LlmOptions` fluent methods
- [x] 5.3.4 Validate API key at startup (not at first LLM call)

### 5.4 Test multi-provider
- [x] 5.4.1 Create `TraderAgentConfigTest.java`
- [x] 5.4.2 Test provider selection (openai-compat, anthropic, google)
- [x] 5.4.3 Test model selection per role
- [x] 5.4.4 Test provider-specific settings forwarding
- [x] 5.4.5 Test API key validation at startup

---

## Phase 6: Integration & Polish (2-3 hours)

### 6.1 End-to-end testing
- [ ] 6.1.1 Run full pipeline with instrument identity enabled
- [ ] 6.1.2 Run full pipeline with memory logging enabled
- [ ] 6.1.3 Run full pipeline with checkpoint enabled
- [ ] 6.1.4 Run full pipeline with FRED data tools
- [ ] 6.1.5 Run full pipeline with Polymarket data tools
- [ ] 6.1.6 Verify memory log file format matches Python project
- [ ] 6.1.7 Verify checkpoint file format

### 6.2 Build verification
- [x] 6.2.1 Run `./mvnw compile` to verify no compilation errors
- [x] 6.2.2 Run `./mvnw verify` to confirm build passes
- [x] 6.2.3 Fix any test failures
  - Fixed: InstrumentContextPromptContributor missing @Component
  - Fixed: OrchestratorAgent missing InstrumentContextPromptContributor field
  - Fixed: DebateAgent.storeFinalDecision called inside researchManager
  - Fixed: CheckpointStore.deleteCheckpoint signature (added tradeDate)
  - Fixed: TraderAgentConfig missing provider/model config fields
  - Fixed: All test files updated for new record constructors
  - 340 tests pass, 0 failures

### 6.3 Documentation
- [x] 6.3.1 Update `AGENTS.md` with new features
- [x] 6.3.2 Update `README.md` with new configuration options
- [x] 6.3.3 Add inline comments for new code (if necessary)

### 6.4 Cleanup
- [x] 6.4.1 Remove unused imports
- [x] 6.4.2 Verify no compilation warnings
- [x] 6.4.3 Verify all new files are in correct packages
