# Tasks: Migrate Python Features from Tauric to Java

## Phase 1: Instrument Identity (2-3 hours)

### 1.1 Create InstrumentContext record
- [ ] 1.1.1 Create `InstrumentContext.java` in `agent/identity/` package
- [ ] 1.1.2 Add fields: `ticker`, `companyName`, `sector`, `industry`, `exchange`, `currency`
- [ ] 1.1.3 Make it a Java record (immutable, serializable)
- [ ] 1.1.4 Add `@JsonIgnore` annotations if needed for JSON serialization

### 1.2 Create InstrumentIdentityAgent
- [ ] 1.2.1 Create `InstrumentIdentityAgent.java` with `@Agent` annotation
- [ ] 1.2.2 Implement `resolveIdentity(Ticker ticker)` → `InstrumentContext`
- [ ] 1.2.3 Use `YFinService.getTickerInfo()` for data fetching
- [ ] 1.2.4 Cache results via `FileCache` with 24-hour TTL
- [ ] 1.2.5 Add `@Condition(pre = "tickerFormatIsValid(ticker)")` guard
- [ ] 1.2.6 Handle exceptions gracefully (return null, not throw)
- [ ] 1.2.7 Implement `validateTicker(Ticker input)` → `boolean`
- [ ] 1.2.8 Add `@Autowired` fields for `YFinService` and `FileCache`

### 1.3 Create InstrumentContextPromptContributor
- [ ] 1.3.1 Create `InstrumentContextPromptContributor.java` in `agent/identity/`
- [ ] 1.3.2 Implement `PromptContributor` interface
- [ ] 1.3.3 Read `InstrumentContext` from blackboard
- [ ] 1.3.4 Inject system message with company name, sector, industry, exchange
- [ ] 1.3.5 Include "Do not confuse with other companies" warning
- [ ] 1.3.6 Skip injection if `InstrumentContext` is null (fail-open)
- [ ] 1.3.7 Register as Spring `@Component`

### 1.4 Wire into OrchestratorAgent
- [ ] 1.4.1 Add `@Autowired InstrumentIdentityAgent identityAgent` to `OrchestratorAgent`
- [ ] 1.4.2 Call `identityAgent.resolveIdentity(ticker)` after `tickerFromForm()`
- [ ] 1.4.3 Bind result to blackboard (Embelabel auto-binds return values)
- [ ] 1.4.4 Verify: check that instrument context appears in agent prompts

### 1.5 Test instrument identity
- [ ] 1.5.1 Create `InstrumentIdentityAgentTest.java` with `FakeOperationContext`
- [ ] 1.5.2 Test successful resolution of AAPL
- [ ] 1.5.3 Test caching (second call returns cached result)
- [ ] 1.5.4 Test failure handling (YFinService throws → returns null)
- [ ] 1.5.5 Test validation (empty ticker → returns false)

---

## Phase 2: Decision Memory (4-6 hours)

### 2.1 Create domain records
- [ ] 2.1.1 Create `PendingDecision.java` in `agent/memory/`
- [ ] 2.1.2 Create `ResolvedDecision.java` in `agent/memory/`
- [ ] 2.1.3 Add all required fields (ticker, date, rating, returns, reflection, etc.)
- [ ] 2.1.4 Make them Java records

### 2.2 Create DecisionMemoryRepository
- [ ] 2.2.1 Create `DecisionMemoryRepository.java` in `agent/memory/`
- [ ] 2.2.2 Implement file path config (`app.memory.log-path`, default `~/.tradingagents/memory/trading_memory.md`)
- [ ] 2.2.3 Implement `appendPending()` — atomic write (temp + rename)
- [ ] 2.2.4 Implement `resolve()` — atomic update (read, modify, write)
- [ ] 2.2.5 Implement `getPendingEntries(Ticker)` — parse with regex
- [ ] 2.2.6 Implement `hasPendingEntriesFor(Ticker)` — check for pending status
- [ ] 2.2.7 Implement `generatePastContext(Ticker)` — extract 5 same-ticker + 3 cross-ticker
- [ ] 2.2.8 Implement `rotate()` — prune oldest entries if over `max-entries`
- [ ] 2.2.9 Implement `recoverFromCorruption()` — truncate to last complete entry
- [ ] 2.2.10 Use regex patterns matching Python's `_DECISION_RE` and `_REFLECTION_RE`
- [ ] 2.2.11 Use `<!-- ENTRY_END -->` as separator
- [ ] 2.2.12 Add `@Autowired` for config injection

### 2.3 Create DecisionMemoryAgent
- [ ] 2.3.1 Create `DecisionMemoryAgent.java` with `@Agent` annotation
- [ ] 2.3.2 Implement `storeDecision(Ticker, TradingDate, String rating, String summary, String thesis)` — void
- [ ] 2.3.3 Implement `resolvePending(Ticker, TradingDate)` — with `@Condition(pre = "hasPendingEntries")`
- [ ] 2.3.4 Implement `generatePastContext(Ticker)` → String
- [ ] 2.3.5 Implement `@Tool fetchReturns(Ticker, TradingDate)` → ReturnsData
- [ ] 2.3.6 Use `BEST_ROLE` model for LLM reflection
- [ ] 2.3.7 Use `withId("memory-reflection")` for traceability
- [ ] 2.3.8 Use `withTemplate("memory/reflection")` for prompt template
- [ ] 2.3.9 Add `@Autowired` for `DecisionMemoryRepository`

### 2.4 Create reflection prompt template
- [ ] 2.4.1 Create `prompts/memory/reflection.jinja`
- [ ] 2.4.2 Include: decision rating, executive summary, actual returns, alpha vs benchmark
- [ ] 2.4.3 Ask for one-paragraph analysis of what went right/wrong
- [ ] 2.4.4 Keep it concise (LLM shouldn't write an essay)

### 2.5 Wire into existing agents
- [ ] 2.5.1 Add `@Autowired DecisionMemoryAgent memoryAgent` to `DebateAgent`
- [ ] 2.5.2 Call `memoryAgent.storeDecision()` after `researchManager()` completes
- [ ] 2.5.3 Add `@Autowired DecisionMemoryAgent memoryAgent` to `OrchestratorAgent`
- [ ] 2.5.4 Call `memoryAgent.resolvePending()` at start of pipeline (before debate)
- [ ] 2.5.5 Call `memoryAgent.generatePastContext()` and bind to blackboard
- [ ] 2.5.6 Update `ResearchManager.jinja` to use existing `past_context` slot

### 2.6 Test memory system
- [ ] 2.6.1 Create `DecisionMemoryRepositoryTest.java`
- [ ] 2.6.2 Test append pending entry
- [ ] 2.6.3 Test parse pending entry
- [ ] 2.6.4 Test resolve entry (with mock returns)
- [ ] 2.6.5 Test generate past_context (with mock data)
- [ ] 2.6.6 Test rotation (max-entries)
- [ ] 2.6.7 Test atomic write (temp + rename)
- [ ] 2.6.8 Test corruption recovery
- [ ] 2.6.9 Create `DecisionMemoryAgentTest.java` with `FakeOperationContext`
- [ ] 2.6.10 Test storeDecision action
- [ ] 2.6.11 Test resolvePending action (with mocked fetchReturns)
- [ ] 2.6.12 Test generatePastContext action

---

## Phase 3: Checkpoint / Resume (4-6 hours)

### 3.1 Create CheckpointStore
- [ ] 3.1.1 Create `CheckpointStore.java` in `agent/checkpoint/`
- [ ] 3.1.2 Implement JSON file storage (data/checkpoints/<TICKER>.json)
- [ ] 3.1.3 Implement `saveCheckpoint(ticker, date, phase, blackboardJson)`
- [ ] 3.1.4 Implement `getCheckpoint(ticker, date)` → `CheckpointEntry`
- [ ] 3.1.5 Implement `hasCheckpoint(ticker, date)` → boolean
- [ ] 3.1.6 Implement `deleteCheckpoint(ticker, date)`
- [ ] 3.1.7 Use Jackson `ObjectMapper` for JSON serialization
- [ ] 3.1.8 Use atomic writes (temp + rename)
- [ ] 3.1.9 Add `@Autowired` for config injection (`app.checkpoint.dir`)

### 3.2 Create CheckpointAgent
- [ ] 3.2.1 Create `CheckpointAgent.java` with `@Agent` annotation
- [ ] 3.2.2 Implement `saveCheckpoint(Ticker, TradingDate, String phase)` — void
- [ ] 3.2.3 Add `@Condition(post = "isPhaseCompleted(phase)")` guard
- [ ] 3.2.4 Serialize blackboard to JSON via Jackson
- [ ] 3.2.5 Implement `restoreCheckpoint(Ticker, TradingDate)` → `Map<String, Object>`
- [ ] 3.2.6 Add `@Condition(pre = "store.hasCheckpoint(ticker, date)")` guard
- [ ] 3.2.7 Deserialize JSON back to typed objects
- [ ] 3.2.8 Return last completed phase
- [ ] 3.2.9 Implement `clearCheckpoint(Ticker, TradingDate)` — void
- [ ] 3.2.10 Add `@Autowired` for `CheckpointStore`

### 3.3 Wire into existing agents
- [ ] 3.3.1 Add `@Autowired CheckpointAgent checkpointAgent` to `OrchestratorAgent`
- [ ] 3.3.2 Call `checkpointAgent.restoreCheckpoint()` at start of `start()` action
- [ ] 3.3.3 Wire result into blackboard (Embelabel auto-binds return values)
- [ ] 3.3.4 Add `@Autowired CheckpointAgent checkpointAgent` to `DebateAgent`
- [ ] 3.3.5 Call `checkpointAgent.saveCheckpoint()` after each phase completes
- [ ] 3.3.6 Call `checkpointAgent.clearCheckpoint()` after `researchManager()` completes
- [ ] 3.3.7 Skip checkpoint actions when `app.checkpoint.enabled` is false

### 3.4 Test checkpoint system
- [ ] 3.4.1 Create `CheckpointStoreTest.java`
- [ ] 3.4.2 Test save and restore checkpoint
- [ ] 3.4.3 Test JSON serialization/deserialization
- [ ] 3.4.4 Test hasCheckpoint / deleteCheckpoint
- [ ] 3.4.5 Test atomic write (temp + rename)
- [ ] 3.4.6 Create `CheckpointAgentTest.java` with `FakeOperationContext`
- [ ] 3.4.7 Test saveCheckpoint action
- [ ] 3.4.8 Test restoreCheckpoint action
- [ ] 3.4.9 Test clearCheckpoint action

---

## Phase 4: Extended Data Sources (3-4 hours)

### 4.1 Create FredService
- [ ] 4.1.1 Create `FredService.java` in `dataflows/`
- [ ] 4.1.2 Implement `getSeries(String seriesId, int limit)` — returns markdown table
- [ ] 4.1.3 Implement `getMultipleSeries(List<String> seriesIds)` — returns map of series
- [ ] 4.1.4 Implement `getDashboard()` — returns standard macro indicators
- [ ] 4.1.5 Use `RestTemplate` for HTTP requests
- [ ] 4.1.6 Cache responses via `FileCache`
- [ ] 4.1.7 Handle errors gracefully (return empty results, not exceptions)
- [ ] 4.1.8 Add `@Autowired` for API key config (`app.fred.api-key`)
- [ ] 4.1.9 FRED API base URL: `https://api.stlouisfed.org/fred/`

### 4.2 Create FredDataTools
- [ ] 4.2.1 Create `FredDataTools.java` in `tools/`
- [ ] 4.2.2 Add `@EmbelabelComponent` annotation
- [ ] 4.2.3 Implement `@LlmTool getMacroIndicators(String seriesId)` → String
- [ ] 4.2.4 Implement `@LlmTool getMacroDashboard()` → String
- [ ] 4.2.5 Add `@ToolParam` descriptions
- [ ] 4.2.6 Add `@Autowired` for `FredService`
- [ ] 4.2.7 Return formatted markdown (matching existing tool output format)

### 4.3 Create PolymarketService
- [ ] 4.3.1 Create `PolymarketService.java` in `dataflows/`
- [ ] 4.3.2 Implement `searchMarkets(String query)` — returns markdown table
- [ ] 4.3.3 Implement `getMarket(String slug)` — returns market details
- [ ] 4.3.4 Use `RestTemplate` for HTTP requests
- [ ] 4.3.5 Cache responses via `FileCache`
- [ ] 4.3.6 Polymarket base URL: `https://clob.polymarket.com/`
- [ ] 4.3.7 No API key required
- [ ] 4.3.8 Handle errors gracefully

### 4.4 Create PolymarketDataTools
- [ ] 4.4.1 Create `PolymarketDataTools.java` in `tools/`
- [ ] 4.4.2 Add `@EmbelabelComponent` annotation
- [ ] 4.4.3 Implement `@LlmTool getPredictionMarkets(String query)` → String
- [ ] 4.4.4 Add `@ToolParam` description
- [ ] 4.4.5 Add `@Autowired` for `PolymarketService`
- [ ] 4.4.6 Return formatted markdown

### 4.5 Integrate with VendorRouter
- [ ] 4.5.1 Add `MACRO_DATA` and `PREDICTION_MARKETS` to `DataSourceCategory` enum
- [ ] 4.5.2 Add routing: `MACRO_DATA` → `FredService`, `PREDICTION_MARKETS` → `PolymarketService`
- [ ] 4.5.3 Add config: `data_vendors.macro_data = fred`, `data_vendors.prediction_markets = polymarket`

### 4.6 Test data sources
- [ ] 4.6.1 Create `FredServiceTest.java`
- [ ] 4.6.2 Test getSeries (mock RestTemplate)
- [ ] 4.6.3 Test getMultipleSeries (mock RestTemplate)
- [ ] 4.6.4 Test error handling (429 rate limit → empty results)
- [ ] 4.6.5 Create `PolymarketServiceTest.java`
- [ ] 4.6.6 Test searchMarkets (mock RestTemplate)
- [ ] 4.6.7 Test getMarket (mock RestTemplate)
- [ ] 4.6.8 Create `FredDataToolsTest.java` with `FakeOperationContext`
- [ ] 4.6.9 Test LLM tool calling
- [ ] 4.6.10 Create `PolymarketDataToolsTest.java` with `FakeOperationContext`
- [ ] 4.6.11 Test LLM tool calling

---

## Phase 5: Multi-Provider LLM (2-3 hours)

### 5.1 Add Maven dependencies
- [ ] 5.1.1 Add `embabel-agent-starter-anthropic` to `pom.xml`
- [ ] 5.1.2 Add `embabel-agent-starter-google` to `pom.xml`
- [ ] 5.1.3 Add `embabel-agent-starter-azure` to `pom.xml`
- [ ] 5.1.4 Add `embabel-agent-starter-bedrock` to `pom.xml`
- [ ] 5.1.5 Add `embabel-agent-starter-ollama` to `pom.xml`
- [ ] 5.1.6 Verify `embabel-agent-starter-openai-custom` is still present

### 5.2 Update configuration
- [ ] 5.2.1 Add `provider` property to `TraderAgentConfig`
- [ ] 5.2.2 Add `best-model` and `cheapest-model` properties
- [ ] 5.2.3 Add provider-specific config: `anthropic.effort`, `google.thinking-level`, `openai.reasoning-effort`
- [ ] 5.2.4 Add `app.fred.api-key` config property
- [ ] 5.2.5 Add `app.memory.log-path` config property
- [ ] 5.2.6 Add `app.memory.log-max-entries` config property
- [ ] 5.2.7 Add `app.checkpoint.enabled` config property
- [ ] 5.2.8 Add `app.checkpoint.dir` config property
- [ ] 5.2.9 Update `application.yaml` with new config sections

### 5.3 Wire provider selection
- [ ] 5.3.1 Update `TraderAgentConfig` to select provider based on config
- [ ] 5.3.2 Update `BEST_ROLE` and `CHEAPEST_ROLE` model selection
- [ ] 5.3.3 Forward provider-specific settings via `LlmOptions` fluent methods
- [ ] 5.3.4 Validate API key at startup (not at first LLM call)

### 5.4 Test multi-provider
- [ ] 5.4.1 Create `TraderAgentConfigTest.java`
- [ ] 5.4.2 Test provider selection (openai-compat, anthropic, google)
- [ ] 5.4.3 Test model selection per role
- [ ] 5.4.4 Test provider-specific settings forwarding
- [ ] 5.4.5 Test API key validation at startup

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
- [ ] 6.2.1 Run `./mvnw compile` to verify no compilation errors
- [ ] 6.2.2 Run `./mvnw verify` to confirm build passes
- [ ] 6.2.3 Fix any test failures

### 6.3 Documentation
- [ ] 6.3.1 Update `AGENTS.md` with new features
- [ ] 6.3.2 Update `README.md` with new configuration options
- [ ] 6.3.3 Add inline comments for new code (if necessary)

### 6.4 Cleanup
- [ ] 6.4.1 Remove unused imports
- [ ] 6.4.2 Verify no compilation warnings
- [ ] 6.4.3 Verify all new files are in correct packages
