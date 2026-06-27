# Proposal: Migrate Python Features from Tauric to Java (Embelabel)

## Why

The Java project (Gekko) and the Python project (Tauric/TradingAgents) share the same core architecture — multi-agent LLM trading research with analyst reports, bull/bear debate, risk debate, and portfolio manager decision. However, the Python project has several features the Java project lacks:

1. **Memory & Reflection** — The Python system learns from past decisions by storing outcomes and injecting reflections into future prompts. The Java project has no learning mechanism.
2. **Checkpoint/Resume** — The Python system can resume a crashed run from the last successful node. The Java project restarts from scratch on failure.
3. **Broader data sources** — The Python project integrates FRED macro data, Polymarket prediction markets, and social media (StockTwits, Reddit). The Java project only has Alpha Vantage and Yahoo Finance.
4. **Instrument identity resolution** — The Python project resolves company identity (name, sector, exchange) before any agent runs, preventing LLM hallucination. The Java project has no such guardrail.
5. **Multi-provider LLM support** — The Python project supports 16+ providers (OpenAI, Anthropic, Google, Azure, Bedrock, Ollama, etc.). The Java project only supports OpenAI-compatible endpoints.

These gaps make the Java project a less complete research tool. Migrating these features using Embabel patterns will bring the Java project to feature parity with the Python project while retaining the Java project's advantages (web UI, HITL, SSE).

## What Changes

### New Capabilities

- **`decision-memory`** — Append-only markdown decision log with two-phase storage (pending → resolved via actual returns) and LLM-generated reflections injected into future Portfolio Manager prompts
- **`checkpoint-resume`** — Crash recovery via blackboard snapshot persistence to disk (SQLite or JSON), keyed by `(ticker, tradeDate)`, with automatic checkpoint clear on success
- **`extended-data-sources`** — New `@LlmTool` classes for FRED macro data and Polymarket prediction markets, integrated via the existing `VendorRouter`
- **`instrument-identity`** — Pre-run company identity resolution (name, sector, industry, exchange) with LRU caching and prompt injection via a `PromptContributor`
- **`multi-provider-llm`** — Add Embabel starter dependencies for Anthropic (`embabel-agent-starter-anthropic`) and Google (`embabel-agent-starter-google`) to enable provider selection beyond OpenAI-compatible

### Modified Capabilities

- **`agent-orchestration`** — Extend with new agent types (DecisionMemoryAgent, CheckpointAgent, InstrumentIdentityAgent) and integrate them into the existing Orchestrator → DebateAgent pipeline
- **`native-hitl`** — Unchanged; Java already has superior HITL via `WaitFor.formSubmission()`

### Deleted Capabilities

None.

## Impact

### New files:
- `src/main/java/com/embabel/gekko/agent/memory/DecisionMemoryAgent.java` — Memory system agent
- `src/main/java/com/embabel/gekko/agent/memory/DecisionMemoryRepository.java` — Markdown file I/O with atomic writes
- `src/main/java/com/embabel/gekko/agent/checkpoint/CheckpointAgent.java` — Checkpoint management agent
- `src/main/java/com/embabel/gekko/agent/checkpoint/CheckpointStore.java` — Disk persistence (SQLite or JSON)
- `src/main/java/com/embabel/gekko/agent/identity/InstrumentIdentityAgent.java` — Company identity resolution
- `src/main/java/com/embabel/gekko/agent/identity/InstrumentContext.java` — Domain record
- `src/main/java/com/embabel/gekko/agent/identity/InstrumentContextPromptContributor.java` — Prompt injection
- `src/main/java/com/embabel/gekko/dataflows/FredService.java` — FRED macro data service
- `src/main/java/com/embabel/gekko/dataflows/PolymarketService.java` — Polymarket prediction markets service
- `src/main/java/com/embabel/gekko/tools/FredDataTools.java` — FRED `@LlmTool`
- `src/main/java/com/embabel/gekko/tools/PolymarketDataTools.java` — Polymarket `@LlmTool`
- `openspec/specs/decision-memory/spec.md`
- `openspec/specs/checkpoint-resume/spec.md`
- `openspec/specs/extended-data-sources/spec.md`
- `openspec/specs/instrument-identity/spec.md`
- `openspec/specs/multi-provider-llm/spec.md`

### Modified files:
- `pom.xml` — Add Anthropic and Google Embabel starters, FRED HTTP client dependency
- `application.yaml` — Add provider selection config, FRED API key config
- `TraderAgentConfig.java` — Wire new agents
- `OrchestratorAgent.java` — Integrate instrument identity resolution and checkpoint restore
- `DebateAgent.java` — Integrate memory store and checkpoint save
- `GekkoApplication.java` — Component scan for new packages
- `VendorRouter.java` — Add FRED and Polymarket routing

### Unchanged:
- Existing agent classes (Trader, RiskDebateAgent, DebateLoopAgent, PortfolioManager)
- Web UI (Thymeleaf/HTMX templates, SSE controllers)
- Existing data services (AlphaVantageService, YFinService)
- Existing tools (MarketDataTools, FundamentalDataTools, NewsDataTools)
- Existing prompt templates
- FileCache (reused for memory log and instrument cache)

### BREAKING: None

This is a pure addition. All existing HTTP endpoints, blackboard types, and agent interfaces remain unchanged.
