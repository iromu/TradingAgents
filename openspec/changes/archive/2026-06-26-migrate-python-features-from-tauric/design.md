# Design: Migrate Python (Tauric) Features to Java (Embelabel)

## Context

The Java project (Gekko) and Python project (Tauric/TradingAgents) implement the same core idea: multi-agent LLM systems for financial trading research. The Java project has a production-grade web UI, HITL checkpoints, and SSE real-time updates. The Python project has features the Java project lacks:

1. **Decision memory with reflection** — learns from past decisions over time
2. **Checkpoint/resume** — crash recovery from last completed phase
3. **Extended data sources** — FRED macro data, Polymarket prediction markets, StockTwits/Reddit
4. **Instrument identity resolution** — deterministic company resolution prevents LLM hallucination
5. **Multi-provider LLM** — 16+ providers vs. 1 (OpenAI-compatible)

The migration uses Embabel's patterns: `@Agent`, `@Action`, `@LlmTool`, `@State`, `@Condition`, `PromptContributor`, and the existing `FileCache` for disk persistence.

## Existing Architecture (What Stays)

```
┌──────────────────────────────────────────────────────────────┐
│              CURRENT JAVA ARCHITECTURE                        │
│                                                               │
│  OrchestratorAgent                                            │
│  ├── tickerFromForm() → Ticker                               │
│  ├── generateResearchPlan() → ResearchPlan                   │
│  ├── waitForPlanApproval() → HITL                            │
│  └── executeDebate() → DebateAgent (subprocess)              │
│                                                               │
│  DebateAgent                                                  │
│  ├── generateFundamentalsReport() → FundamentalsReport       │
│  ├── generateMarketReport() → MarketReport                   │
│  ├── generateNewsReport() → NewsReport                       │
│  ├── generateSocialMediaReport() → SocialMediaReport         │
│  ├── prepareDebateBriefs() → DebateBriefs                    │
│  ├── runDebate() → DebateLoopAgent (subprocess)              │
│  ├── runRiskDebate() → RiskDebateAgent (subprocess)          │
│  ├── runPortfolioManager() → PortfolioDecision               │
│  ├── waitForReview() → HITL                                  │
│  └── researchManager() → InvestmentPlan (terminal goal)      │
│                                                               │
│  Web UI (Thymeleaf/HTMX) + SSE events                        │
│  FileCache (disk-based, SHA-256 hashed)                      │
│  VendorRouter (Alpha Vantage, Yahoo Finance)                 │
└──────────────────────────────────────────────────────────────┘
```

## Design: Decision Memory

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              DECISION MEMORY SYSTEM                           │
│                                                               │
│  Phase A: Store (every propagate)                            │
│  ┌──────────────┐     ┌───────────────┐     ┌───────────┐   │
│  │ DebateAgent  │──▶ │ DecisionMemory│──▶ │ FileStore │   │
│  │ .research-   │     │ .storeDecision│     │ append    │   │
│  │ Manager()    │     │               │     │ markdown  │   │
│  └──────────────┘     └───────────────┘     └───────────┘   │
│                                                               │
│  Phase B: Resolve (next run, same ticker)                    │
│  ┌──────────────┐     ┌───────────────┐     ┌───────────┐   │
│  │ Orchestrator │──▶ │ DecisionMemory│──▶ │ LLM Refl. │   │
│  │ .start()     │     │ .resolvePending│    │ @Tool     │   │
│  └──────────────┘     └───────────────┘     └───────────┘   │
│                           │                                 │
│                           ▼                                 │
│                      Atomic update (temp + rename)          │
│                                                               │
│  Injection (every PortfolioManager call)                     │
│  ┌──────────────────────────────────────────┐                │
│  │ DecisionMemory.generatePastContext()      │                │
│  │ → 5 same-ticker + 3 cross-ticker lessons │                │
│  │ → bound to blackboard as String           │                │
│  │ → injected into ResearchManager.jinja     │                │
│  └──────────────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

### Domain Model

```java
// In com.embabel.gekko.agent.memory

record PendingDecision(
    Ticker ticker,
    TradingDate tradeDate,
    String rating,           // Buy/Hold/Sell/Overweight/Underweight
    String executiveSummary,
    String investmentThesis,
    LocalDateTime storedAt
) {}

record ResolvedDecision(
    Ticker ticker,
    TradingDate tradeDate,
    String rating,
    BigDecimal rawReturn,
    BigDecimal alphaReturn,
    String benchmark,
    int daysHeld,
    String reflection,
    LocalDateTime storedAt,
    LocalDateTime resolvedAt
) {}
```

### File Format (matching Python)

```markdown
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

### Embabel Implementation

```java
@Agent(description = "Decision memory system that learns from past outcomes")
class DecisionMemoryAgent {

    @Autowired DecisionMemoryRepository repository;

    @Action(description = "Store a pending decision for later resolution")
    void storeDecision(
        OperationContext ctx,
        Ticker ticker,
        TradingDate tradeDate,
        String rating,
        String executiveSummary,
        String investmentThesis
    ) {
        repository.appendPending(ticker, tradeDate, rating, executiveSummary, investmentThesis);
    }

    @Action(description = "Resolve pending decisions with actual returns and generate reflection")
    @Condition(pre = "repository.hasPendingEntriesFor(ticker)")
    void resolvePending(
        OperationContext ctx,
        Ticker ticker,
        TradingDate tradeDate
    ) throws LlmInvocationException {
        var pending = repository.getPending(ticker, tradeDate);
        // 1. Fetch actual returns via @Tool
        var returns = fetchReturns(ticker, tradeDate);
        // 2. LLM reflection
        var reflection = ctx.ai()
            .withLlmByRole(BEST_ROLE)
            .withId("memory-reflection")
            .withTemplate("memory/reflection")
            .createObject(String.class, model);
        // 3. Atomic update
        repository.resolve(pending, returns, reflection);
    }

    @Action(description = "Generate past_context for injection into PM prompt")
    String generatePastContext(Ticker ticker) {
        return repository.generatePastContext(ticker);
        // Returns formatted string: 5 same-ticker + 3 cross-ticker lessons
    }

    @Tool(description = "Fetch actual returns for a ticker on a given date")
    ReturnsData fetchReturns(Ticker ticker, TradingDate tradeDate) {
        // Use YFinService or AlphaVantageService to get 5-day return
        // Alpha vs benchmark (SPY)
    }
}
```

### Repository (File-based, reuses FileCache patterns)

```java
@Component
class DecisionMemoryRepository {

    private final Path memoryLogPath;
    private static final String ENTRY_SEPARATOR = "<!-- ENTRY_END -->";

    // Atomic append: temp file + rename (same pattern as FileCache)
    void appendPending(...) { ... }

    // Parse entries with regex (same pattern as Python's _DECISION_RE)
    List<PendingDecision> getPendingEntries(Ticker ticker) { ... }

    // Atomic update: read, modify, write (temp + rename)
    void resolve(PendingDecision pending, ReturnsData returns, String reflection) { ... }

    // Parse and extract: 5 same-ticker + 3 cross-ticker
    String generatePastContext(Ticker ticker) { ... }
}
```

### Prompt Injection

The `generatePastContext()` result is bound to the blackboard as a `String` (or a new `PastContext` record). The `ResearchManager.jinja` template already has a `past_context` variable slot — we just need to make sure the blackboard type matches.

### Design Decisions

**D1: Markdown file format (matching Python)**
- Same `<!-- ENTRY_END -->` separator — cannot appear in LLM output
- Same regex-parseable format — Python and Java can share the same log file if needed
- Atomic writes via temp file + rename (FileCache pattern)

**D2: Resolution on next run (not immediate)**
- The Python project resolves on the next run for the same ticker
- This avoids blocking the current run with a return-fetching LLM call
- The `resolvePending` action has a `@Condition(pre = ...)` so it only runs when pending entries exist

**D3: LLM reflection uses BEST_ROLE model**
- Reflection is a critical reasoning task — should use the same model as the Portfolio Manager
- Configured via `withLlmByRole(BEST_ROLE)`

**D4: Same file path as Python (optional)**
- Default: `~/.tradingagents/memory/trading_memory.md` (matching Python)
- Configurable via `app.memory.log-path`
- This allows both Python and Java projects to share the same memory log

## Design: Checkpoint / Resume

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│            CHECKPOINT / RESUME SYSTEM                        │
│                                                               │
│  On process start:                                            │
│  ┌──────────────┐     ┌───────────────┐     ┌───────────┐   │
│  │ Orchestrator │──▶ │ CheckpointAgent│──▶ │ Checkpoint│   │
│  │ .start()     │     │ .restore()     │     │ Store     │   │
│  └──────────────┘     └───────────────┘     └───────────┘   │
│        │                        │                    │       │
│        │                   Found?                Restored    │
│        │                        │                    │       │
│        ▼                   Skipped              blackboard   │
│  Pipeline runs...                                         │
│        │                                                    │
│        ▼                                                    │
│  ┌──────────────┐     ┌───────────────┐     ┌───────────┐   │
│  │ DebateAgent  │──▶ │ CheckpointAgent│──▶ │ Save      │   │
│  │ .research-   │     │ .save()        │     │ snapshot  │   │
│  │ Manager()    │     │                │     │ per phase │   │
│  └──────────────┘     └───────────────┘     └───────────┘   │
│        │                        │                    │       │
│        ▼                        ▼                    │       │
│  COMPLETED              Clear checkpoint            │       │
│                                                               │
│  Persistence:                                                 │
│  ┌─────────────────────────────────────┐                    │
│  │ CheckpointStore (JSON file)         │                    │
│  │ Path: data/checkpoints/<TICKER>.json│                    │
│  │ Key: (ticker, tradeDate, phase)     │                    │
│  │ Value: serialized blackboard state  │                    │
│  └─────────────────────────────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

### Domain Model

```java
@State
record CheckpointEntry(
    String ticker,
    String tradeDate,
    String phase,            // e.g., "researchPlan", "debate", "riskDebate", "completed"
    String blackboardSnapshot, // JSON-serialized blackboard
    LocalDateTime savedAt
) {}
```

### Embabel Implementation

```java
@Agent(description = "Checkpoint manager for crash recovery")
class CheckpointAgent {

    @Autowired CheckpointStore store;

    @Action(description = "Restore blackboard from checkpoint if exists")
    @Condition(pre = "store.hasCheckpoint(ticker, tradeDate)")
    Map<String, Object> restoreCheckpoint(
        OperationContext ctx,
        Ticker ticker,
        TradingDate tradeDate
    ) {
        var entry = store.getCheckpoint(ticker.toString(), tradeDate.toString());
        if (entry != null) {
            // Restore blackboard from snapshot
            // Skip already-completed phases
            return deserializeBlackboard(entry.blackboardSnapshot());
        }
        return null;
    }

    @Action(description = "Save blackboard snapshot after each phase")
    @Condition(post = "isPhaseCompleted(phase)")
    void saveCheckpoint(
        OperationContext ctx,
        Ticker ticker,
        TradingDate tradeDate,
        String phase
    ) {
        var snapshot = serializeBlackboard(ctx.blackboard());
        store.saveCheckpoint(ticker.toString(), tradeDate.toString(), phase, snapshot);
    }

    @Action(description = "Clear checkpoint on successful completion")
    void clearCheckpoint(
        OperationContext ctx,
        Ticker ticker,
        TradingDate tradeDate
    ) {
        store.deleteCheckpoint(ticker.toString(), tradeDate.toString());
    }
}
```

### Store Implementation (JSON file, reuses FileCache patterns)

```java
@Component
class CheckpointStore {

    private final Path checkpointDir;
    private final ObjectMapper jsonMapper;

    // Key: ticker + tradeDate → single JSON file
    // Content: list of phase checkpoints
    void saveCheckpoint(String ticker, String date, String phase, String snapshot) { ... }

    CheckpointEntry getCheckpoint(String ticker, String date) { ... }

    boolean hasCheckpoint(String ticker, String date) { ... }

    void deleteCheckpoint(String ticker, String date) { ... }
}
```

### Design Decisions

**D1: JSON file per ticker (not SQLite)**
- Simpler, no new dependency
- FileCache already has the atomic write pattern (temp + rename)
- Race condition risk is low (single-user, single-process)
- If concurrent access becomes a problem, migrate to SQLite later

**D2: Phase-level granularity (not node-level)**
- LangGraph saves after each node; Embabel's agents are higher-level
- Save after each major phase: researchPlan → debate → riskDebate → completed
- This is sufficient for crash recovery without excessive I/O

**D3: Blackboard serialization via Jackson**
- Embabel's blackboard holds typed objects (Java records)
- Jackson can serialize/deserialize records to/from JSON
- `ObjectMapper` is already available via Spring Boot auto-config

**D4: Checkpoint clear on success**
- Same as LangGraph's behavior
- Prevents stale checkpoints from interfering with future runs
- If the process fails mid-phase, the checkpoint remains for resume

## Design: Extended Data Sources

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│            EXTENDED DATA SOURCES                             │
│                                                               │
│  New @LlmTool classes:                                       │
│  ┌─────────────────┐  ┌──────────────────┐                  │
│  │ FredDataTools   │  │ PolymarketData   │                  │
│  │ @LlmTool        │  │ Tools            │                  │
│  │ getMacroData()  │  │ getPrediction-   │                  │
│  │                 │  │ Markets()        │                  │
│  └────────┬────────┘  └────────┬─────────┘                  │
│           │                     │                            │
│           ▼                     ▼                            │
│  ┌─────────────────────────────────────┐                    │
│  │           VendorRouter              │                    │
│  │  macro_data ──▶ FRED                │                    │
│  │  prediction_markets ──▶ Polymarket  │                    │
│  └─────────────────────────────────────┘                    │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────────────────────────┐                    │
│  │  LLM tool-calling (existing)        │                    │
│  │  NewsDataTools.get_global_news()    │                    │
│  │  NewsDataTools.get_insider_sentiment()│                   │
│  └─────────────────────────────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

### FRED Service

```java
@Service
class FredService {

    private final RestTemplate restTemplate;
    private final String apiKey;

    // Fetch a FRED series (e.g., GDP, CPI, unemployment rate)
    String getSeries(String seriesId, int limit) {
        // GET https://api.stlouisfed.org/fred/series/observations
        // Returns JSON with observation dates and values
        // Format as markdown table
    }

    // Fetch multiple series at once (e.g., all macro indicators)
    Map<String, List<Observation>> getMultipleSeries(List<String> seriesIds) { ... }
}
```

### FRED Data Tools

```java
@EmbelabelComponent
class FredDataTools {

    @Autowired FredService fredService;

    @LlmTool(description = "Fetch macroeconomic indicators from FRED")
    String getMacroIndicators(
        @ToolParam(description = "Economic indicator series ID (e.g., GDP, CPIAUCSL, UNRATE)")
        String seriesId
    ) {
        return fredService.getSeries(seriesId, 365); // 1 year of data
    }

    @LlmTool(description = "Fetch a range of macroeconomic indicators")
    String getMacroDashboard(
        @ToolParam(description = "Comma-separated list of series IDs")
        String seriesIds
    ) {
        var results = fredService.getMultipleSeries(
            Arrays.asList(seriesIds.split(","))
        );
        return formatAsMarkdown(results);
    }
}
```

### Polymarket Service

```java
@Service
class PolymarketService {

    private final RestTemplate restTemplate;

    // Polymarket has no API key requirement
    // GET https://clob.polymarket.com/markets?search=...
    List<Market> searchMarkets(String query) { ... }

    // Get market details by slug
    Market getMarket(String slug) { ... }
}
```

### Polymarket Data Tools

```java
@EmbelabelComponent
class PolymarketDataTools {

    @Autowired PolymarketService polymarketService;

    @LlmTool(description = "Fetch prediction market probabilities from Polymarket")
    String getPredictionMarkets(
        @ToolParam(description = "Market topic or question (e.g., 'Fed rate cut', 'election')")
        String query
    ) {
        var markets = polymarketService.searchMarkets(query);
        return formatAsMarkdown(markets);
    }
}
```

### VendorRouter Integration

```java
// In VendorRouter.java — add new routing entries:
enum DataSourceCategory {
    CORE_STOCK_APIS,
    TECHNICAL_INDICATORS,
    FUNDAMENTAL_DATA,
    NEWS_DATA,
    MACRO_DATA,          // NEW: FRED only
    PREDICTION_MARKETS,  // NEW: Polymarket only
    SENTIMENT_DATA       // FUTURE: StockTwits, Reddit
}
```

### Design Decisions

**D1: @LlmTool pattern (matching existing tools)**
- `FredDataTools` and `PolymarketDataTools` follow the same pattern as `MarketDataTools`, `FundamentalDataTools`, `NewsDataTools`
- `@EmbelabelComponent` for auto-discovery
- `@LlmTool` methods exposed to LLM as function-calling tools

**D2: FRED requires API key (free), Polymarket is keyless**
- FRED: `app.fred.api-key` config property
- Polymarket: no key required
- Both use `RestTemplate` (already available via Spring Boot)

**D3: Markdown output format (matching existing tools)**
- All tool methods return formatted markdown, not raw JSON
- The LLM reads the markdown in its tool-calling context
- Consistent with existing `MarketDataTools`, `FundamentalDataTools`, etc.

**D4: StockTwits/Reddit deferred**
- These require API keys or scraping
- The Python project uses them for the Sentiment Analyst
- Can be added later when API access is available

## Design: Instrument Identity

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│            INSTRUMENT IDENTITY RESOLUTION                    │
│                                                               │
│  On Orchestrator.start():                                     │
│  ┌──────────────┐     ┌──────────────────┐                  │
│  │ Orchestrator │──▶ │ InstrumentIdentity│                  │
│  │ .tickerFrom  │     │ .resolveIdentity │                  │
│  │ Form()       │     │                  │                  │
│  └──────────────┘     └────────┬─────────┘                  │
│                                 │                            │
│                                 ▼                            │
│                          ┌──────────────┐                    │
│                          │ FileCache    │                    │
│                          │ (LRU cache)  │                    │
│                          └──────────────┘                    │
│                                 │                            │
│                                 ▼                            │
│                          ┌──────────────┐                    │
│                          │ Instrument-  │                    │
│                          │ Context      │                    │
│                          │ (blackboard) │                    │
│                          └──────────────┘                    │
│                                 │                            │
│                                 ▼                            │
│                          ┌──────────────┐                    │
│                          │ Prompt-      │                    │
│                          │ Contributor  │                    │
│                          │ injects into │                    │
│                          │ every prompt │                    │
│                          └──────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

### Domain Model

```java
record InstrumentContext(
    String ticker,
    String companyName,
    String sector,
    String industry,
    String exchange,
    String currency
) {}
```

### Embabel Implementation

```java
@Agent(description = "Resolve and anchor instrument identity to prevent LLM hallucination")
class InstrumentIdentityAgent {

    @Autowired YFinService yfinService;
    @Autowired FileCache fileCache;

    @Action(description = "Resolve ticker to real company identity")
    InstrumentContext resolveIdentity(Ticker ticker) {
        // Check FileCache first (LRU cache)
        var cached = fileCache.get(InstrumentContext.class, "identity:" + ticker);
        if (cached != null) return cached;

        // Fetch from Yahoo Finance
        var info = yfinService.getTickerInfo(ticker);
        var context = new InstrumentContext(
            ticker,
            info.name(),
            info.sector(),
            info.industry(),
            info.exchange(),
            info.currency()
        );

        // Cache for 24 hours
        fileCache.put("identity:" + ticker, context, Duration.ofHours(24));
        return context;
    }
}
```

### PromptContributor

```java
@Component
class InstrumentContextPromptContributor implements PromptContributor {

    @Override
    public void contribute(PromptContext ctx) {
        var identity = ctx.blackboard().get(InstrumentContext.class);
        if (identity != null) {
            ctx.addSystemMessage("""
                INSTRUMENT CONTEXT:
                You are analyzing: %s (%s)
                Sector: %s
                Industry: %s
                Exchange: %s
                
                IMPORTANT: You are analyzing %s. Do not confuse it with any other company.
                All price data, news, and analysis MUST refer to %s.
                """.formatted(
                    identity.companyName(), identity.ticker(),
                    identity.sector(), identity.industry(), identity.exchange(),
                    identity.companyName(), identity.companyName()
                ));
        }
    }
}
```

### Design Decisions

**D1: Yahoo Finance as the identity source**
- YFinService already exists in the Java project
- Same source as the Python project's `yf.Ticker(ticker).info`
- Graceful degradation: if lookup fails, continue without context (fail-open)

**D2: LRU cache via FileCache**
- FileCache already has SHA-256 hashed keys and per-key locking
- Reuse it for instrument identity caching
- 24-hour TTL (company metadata doesn't change frequently)

**D3: PromptContributor for injection**
- Embabel's `PromptContributor` interface injects content into every prompt
- Clean separation: the contributor doesn't know about agent logic
- Same pattern as Embabel's built-in persona/role contributors

**D4: Fail-open (graceful degradation)**
- If Yahoo Finance is down or the ticker is invalid, continue without context
- The LLM still gets the ticker symbol, just not the company metadata
- Same behavior as the Python project's fail-open design

## Design: Multi-Provider LLM

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│            MULTI-PROVIDER LLM                                │
│                                                               │
│  Current:                     Future:                        │
│  ┌──────────────────┐        ┌──────────────────────┐       │
│  │ OpenAI-compatible│        │ OpenAI-compatible    │       │
│  │ (LiteLLM)        │   ──▶  │ (LiteLLM)            │       │
│  │ spark.local:4000 │        │                      │       │
│  └──────────────────┘        │ ┌────────────────┐   │       │
│                               │ │ OpenAI         │   │       │
│                               │ │ Anthropic      │   │       │
│                               │ │ Google Gemini  │   │       │
│                               │ │ Azure OpenAI   │   │       │
│                               │ │ AWS Bedrock    │   │       │
│                               │ │ Ollama         │   │       │
│                               │ └────────────────┘   │       │
│                               └──────────────────────┘       │
│                                                               │
│  Embabel starters:                                            │
│  ┌──────────────────────────────────────────┐                │
│  │ embabel-agent-starter-openai-custom      │ (already)      │
│  │ embabel-agent-starter-anthropic    (new) │                │
│  │ embabel-agent-starter-google       (new) │                │
│  │ embabel-agent-starter-azure          (new)│               │
│  │ embabel-agent-starter-bedrock        (new)│               │
│  │ embabel-agent-starter-ollama         (new)│               │
│  └──────────────────────────────────────────┘                │
│                                                               │
│  Configuration:                                               │
│  app.llm-options.provider: openai | anthropic | google | ...  │
│  app.llm-options.best-model: gpt-5.5 | claude-opus-4 | ...   │
│  app.llm-options.cheapest-model: ...                          │
└─────────────────────────────────────────────────────────────┘
```

### Embabel Implementation

```yaml
# application.yaml — NEW section
app:
  llm-options:
    provider: openai-compat  # openai, anthropic, google, azure, bedrock, ollama
    best-model: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
    cheapest-model: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
    # Provider-specific settings
    anthropic:
      effort: medium  # high, medium, low
    google:
      thinking-level: minimal  # high, minimal, balanced
    openai:
      reasoning-effort: medium  # high, medium, low
```

### Design Decisions

**D1: Add Embabel starter dependencies**
- Each provider has its own Embabel starter
- `embabel-agent-starter-anthropic`, `embabel-agent-starter-google`, etc.
- These are Maven dependencies in `pom.xml`

**D2: Provider selection via config**
- `app.llm-options.provider` controls which starter is active
- Same `BEST_ROLE` / `CHEAPEST_ROLE` model selection pattern
- Model names are provider-specific (e.g., `claude-opus-4` vs `gpt-5.5`)

**D3: Lazy loading (matching Python)**
- Embabel starters handle provider-specific imports
- Unused providers don't cause import errors
- Same pattern as Python's `factory.py` lazy imports

**D4: Keep OpenAI-compatible as default**
- The current setup (LiteLLM at spark.local:4000) works
- Adding providers is opt-in via config
- No breaking changes to existing deployments

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| Memory log format mismatch between Python and Java | Can't share log file | Use same format (`<!-- ENTRY_END -->`, same regex patterns) |
| Checkpoint serialization loses blackboard state | Resume fails or produces wrong results | Test with `FakeOperationContext`; use Jackson with proper serializers |
| FRED API rate limits | Missing macro data | Cache responses via FileCache; degrade gracefully |
| Polymarket API changes | Missing prediction market data | Monitor; add fallback to "no data available" |
| Instrument identity lookup fails | No company context in prompts | Fail-open (graceful degradation); log warning |
| More Maven dependencies | Larger build, potential conflicts | Use Embabel starters (maintained by same team); test compatibility |
| Blackboard serialization complexity | Checkpoint save/load bugs | Start with JSON file; migrate to SQLite if needed |

## Migration Phases

### Phase 1: Instrument Identity (2-3 hours)
- `InstrumentContext` record
- `InstrumentIdentityAgent` with `resolveIdentity()`
- `InstrumentContextPromptContributor`
- Wire in `OrchestratorAgent.start()`
- Test: verify company name appears in agent prompts

### Phase 2: Decision Memory (4-6 hours)
- `DecisionMemoryRepository` (markdown file I/O)
- `DecisionMemoryAgent` (store, resolve, generatePastContext)
- `@Tool fetchReturns()` for actual return fetching
- Wire in `DebateAgent.researchManager()` (store) and `OrchestratorAgent.start()` (resolve + inject)
- Test: verify memory log file, verify reflection injection

### Phase 3: Checkpoint / Resume (4-6 hours)
- `CheckpointStore` (JSON file, atomic writes)
- `CheckpointAgent` (restore, save, clear)
- Wire in `OrchestratorAgent.start()` (restore) and `DebateAgent` phases (save)
- Test: verify checkpoint save/load, verify resume after simulated crash

### Phase 4: Extended Data Sources (3-4 hours)
- `FredService` + `FredDataTools`
- `PolymarketService` + `PolymarketDataTools`
- Wire into `VendorRouter`
- Test: verify tool-calling works, verify markdown output

### Phase 5: Multi-Provider LLM (2-3 hours)
- Add Embabel starter dependencies to `pom.xml`
- Add provider selection config to `application.yaml`
- Update `TraderAgentConfig` to support provider selection
- Test: verify LLM calls work with each provider

### Phase 6: Integration & Polish (2-3 hours)
- End-to-end test: ticker → full pipeline with memory, checkpoint, identity
- Update existing tests (if any break)
- Documentation updates
