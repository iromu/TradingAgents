## Context

The TradingAgents project is a multi-agent trading research platform built on Embabel (Spring Boot 3.5.13, Java 25). The core `TraderAgent` orchestrates a pipeline: ticker input → four analyst reports → debate briefs → bull/bear debate → HITL checkpoint → research plan. The system has accumulated technical debt across three dimensions:

1. **Reliability**: Cache key bugs cause stale data, no HTTP timeouts cause hangs, FileCache race condition causes wasted computation.
2. **Agent Quality**: The debate loop is hardcoded to 2 iterations with no convergence check. The risk debate system (4 prompt files) is dead code. The MarketAnalyst's tool calls are commented out.
3. **Engineering Rigor**: Only 5 test files cover ~200 lines of code out of ~2500+ lines of production logic. The core orchestrator has zero tests.

```
TraderAgent Pipeline:
┌─────────┐  ┌──────────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐  ┌──────────┐  ┌────────────┐
│ Ticker  │→│ 4 Analysts   │→│ Briefs   │→│ Debate   │→│ HITL    │→│ Manager  │→│  Plan    │
│ Input   │  │ (Fund/Market │  │ (Distill)│  │ (Bull/Bear│  │ Check  │  │ (Plan  ) │  │ (Output) │
│         │  │  /News/Social)│  │          │  │  x2 iter) │  │         │  │         │  │           │
└─────────┘  └──────────────┘  └──────────┘  │  ← BUG:  │  └─────────┘  └──────────┘  └────────────┘
                                             │  fixed 2  │
                                             └──────────┘
```

## Goals / Non-Goals

**Goals:**
- Fix all known cache key bugs so API responses are correctly cached per query parameters
- Add HTTP timeouts to prevent indefinite blocking on external API calls
- Fix FileCache race condition to prevent duplicate computation
- Replace fixed debate loop with convergence-based loop
- Wire up or remove the risk debate system
- Enable MarketAnalyst tool calls
- Unify prompt file extensions
- Expand test coverage to core agent logic and services

**Non-Goals:**
- Adding new agent types or new data sources
- Changing the LLM endpoint or model configuration
- Adding real trading execution (research only)
- Refactoring the entire TraderAgent into smaller agents
- Adding a database (file-based cache only)
- CI/CD pipeline changes

## Decisions

### Decision 1: Debate Convergence — Text Similarity (Not Semantic)

**Choice:** Use text-based similarity (n-gram overlap or token-based Jaccard similarity) to detect when the debate has stabilized.

**Rationale:**
- Semantic similarity would require an embedding model, adding a dependency and latency
- Text overlap is fast, deterministic, and works well for debate where agents restate and refine arguments
- The debate loop is already bounded by a max iteration count, so convergence is an early-termination optimization

**Alternatives considered:**
- **Semantic similarity (embeddings)**: More accurate but adds dependency (e.g., on-disk embedding model). Overkill for detecting stabilization.
- **Topic coherence score**: Complex to implement, requires topic modeling.
- **Fixed iteration with higher default**: Simpler but wastes LLM calls on redundant rounds.

**Implementation:**
```java
// In InvestmentDebateState, track bull/bear response pairs
// After each round, compare last bull response with previous bull response (or last bear with previous bear)
// If similarity >= threshold (default 0.8), stop
private double computeSimilarity(String a, String b) {
    // Tokenize, compute Jaccard similarity on bigrams
    Set<String> bigramsA = ngrams(a, 2);
    Set<String> bigramsB = ngrams(b, 2);
    // ... intersection / union
}
```

### Decision 2: Risk Debate — Wire Up, Don't Delete

**Choice:** Wire the existing risk debate prompts into the workflow as a sub-process between the investment debate and the HITL checkpoint.

**Rationale:**
- The four prompt files (RiskManager, AggresiveDebator, ConservativeDebator, NeutralDebator) are well-structured and represent deliberate design work
- Risk assessment is a valuable dimension that the current pipeline lacks
- The prompts already exist — wiring them is lower effort than rewriting

**Alternatives considered:**
- **Delete the prompts**: Wastes existing work, loses the risk assessment capability
- **Simplify to a single risk prompt**: Loses the multi-agent debate pattern that the rest of the system uses

**Implementation:**
```
Pipeline:
  Debate → Risk Debate (Risky/Neutral/Conservative debate) → HITL Checkpoint → Research Manager

New state field:
  InvestmentDebateState.riskAssessment: RiskAssessment { level: Risky|Neutral|Conservative, reasoning: String }
```

### Decision 3: MarketAnalyst Tools — Wire YFinService Directly

**Choice:** Add tool methods to expose YFinService's TA4J indicator calculations and stock data to the MarketAnalyst.

**Rationale:**
- The MarketAnalyst prompt already references `get_stock_data` and `get_indicators` tools
- YFinService already has the methods (`getBarSeries`, `calculateIndicators`)
- The simplest path is to create a `MarketDataTools` class that wraps YFinService

**Implementation:**
```java
// New file: src/main/java/com/embabel/gekko/tools/MarketDataTools.java
@Tool(description = "Get stock price data for a ticker")
public String get_stock_data(@ToolParam String ticker) { ... }

@Tool(description = "Calculate technical indicators for a ticker")
public String get_indicators(@ToolParam String ticker, @ToolParam String indicators) { ... }
```

### Decision 4: FileCache Race Fix — Synchronized Block, Not Lock

**Choice:** Use `synchronized` on a per-key cache entry map instead of the existing `ReentrantReadWriteLock` for the `getOrCompute` method.

**Rationale:**
- The current `ReentrantReadWriteLock` protects the entire cache, but the race is at the per-key level
- A `ConcurrentHashMap.computeIfAbsent()` pattern with a fallback check is the standard Java approach
- Simpler code, fewer lines, well-understood pattern

**Implementation:**
```java
// Use ConcurrentHashMap.computeIfAbsent with double-check
synchronized (getLockForKey(key)) {
    // Double-check after acquiring lock
    if (cache.containsKey(key)) {
        return deserialize(key, extension);
    }
    T result = supplier.get();
    serialize(key, result, extension);
    cache.put(key, result);
    return result;
}
```

### Decision 5: RestTemplate Timeout — SimpleClientHttpRequestFactory

**Choice:** Use `SimpleClientHttpRequestFactory` with explicit connect and read timeout values.

**Rationale:**
- Simple, no new dependencies
- Consistent with the existing code style (the codebase already uses `SimpleClientHttpRequestFactory` for connect timeout)
- Add a new `readTimeoutMs` config property alongside the existing `connectTimeoutMs`

**Implementation:**
```java
// In AlphaVantageService:
@Value("${app.alphavantage.connect-timeout-ms:10000}")
private int connectTimeoutMs;

@Value("${app.alphavantage.read-timeout-ms:30000}")
private int readTimeoutMs;

// In RestTemplate creation:
SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
factory.setConnectTimeout(connectTimeoutMs);
factory.setReadTimeout(readTimeoutMs);
restTemplate = new RestTemplate(factory);
```

### Decision 6: Prompt Extension — Rename .txt → .jinja

**Choice:** Rename all `.txt` analyst prompt files to `.jinja` and update the resource references in `TraderAgent`.

**Rationale:**
- All other prompt files use `.jinja`
- The base template `_BaseAnalyst.jinja` is already a Jinja template
- Consistent extension avoids confusion about template processing
- No functional change — Spring's `Resource.getContentAsString()` works with any extension

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Debate convergence threshold is wrong | Debate stops too early or too late | Make threshold configurable; add logging of similarity scores; start with 0.8 |
| Risk debate adds latency to pipeline | Each ticker analysis takes longer | Risk debate uses CHEAPEST_ROLE LLM; make it optional via config; cache risk debate results |
| MarketAnalyst tools expose raw data | LLM might produce noisy output | Start with filtered/summarized data; add tool parameter validation |
| FileCache lock change has edge cases | Potential data corruption or deadlock | Thorough concurrency testing; keep lock scope minimal; add exception handling |
| RestTemplate timeout causes more failures | Some API calls legitimately take >30s | Make timeout configurable; add retry logic for timeout exceptions |
| Renaming prompt files breaks existing cache | Cached data keyed by prompt content might be stale | Cache is content-addressed (SHA-256 of key string), not prompt-dependent — no impact |

## Migration Plan

1. **Phase 1 — Reliability fixes** (low risk, high impact):
   - Fix cache key bugs in `AlphaVantageService`
   - Add RestTemplate read timeout
   - Fix FileCache race condition
   - Run existing tests to verify no regression

2. **Phase 2 — Agent quality** (medium risk):
   - Add debate convergence check
   - Wire up risk debate system
   - Enable MarketAnalyst tools
   - Rename prompt files `.txt` → `.jinja`
   - Run existing tests

3. **Phase 3 — Test coverage** (low risk, high effort):
   - Add unit tests for TraderAgent actions
   - Add unit tests for BullResearcher, BearResearcher
   - Add unit tests for YFinService, VendorRouter
   - Add integration test for full pipeline

**Rollback:** Each phase is independently revertable. The changes are localized to specific files with no cross-cutting refactoring.

## Open Questions

1. **Debate convergence threshold**: What similarity threshold works best? Start with 0.8 (Jaccard on bigrams) and make it configurable.
2. **Risk debate integration point**: Should risk debate run before or after the investment debate? Before seems more logical (assess risk → debate investment within risk constraints).
3. **MarketAnalyst tool scope**: What data should `get_indicators` return? All TA4J indicators? A subset? Configurable list?
4. **FileCache serialization**: The current `FileCache` uses Jackson ObjectMapper. Does the race fix need to account for partial writes? (Yes — write to temp file, then rename atomically.)
