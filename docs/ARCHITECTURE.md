# TradingAgents (Gekko) — Complete System Architecture

> **Purpose:** This document defines the complete architecture of the TradingAgents system so it can be reimplemented in another programming language or with different frameworks. It covers every component, their responsibilities, interfaces, data flows, and external dependencies.

> **Version:** 0.1.0 — Based on Gekko v0.1.0-SNAPSHOT (Spring Boot 3.5.13, Embabel 0.5.0-SNAPSHOT)

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Architecture Principles](#2-architecture-principles)
3. [High-Level Architecture](#3-high-level-architecture)
4. [Component Reference](#4-component-reference)
5. [Agent Architecture](#5-agent-architecture)
6. [Data Flow](#6-data-flow)
7. [External Dependencies](#7-external-dependencies)
8. [Data Models](#8-data-models)
9. [Persistence Layer](#9-persistence-layer)
10. [Web Interface](#10-web-interface)
11. [Configuration](#11-configuration)
12. [Observability](#12-observability)
13. [Security](#13-security)
14. [Deployment](#14-deployment)
15. [Testing](#15-testing)

---

## 1. System Overview

TradingAgents (code name: **Gekko**) is a **multi-agent AI trading research platform**. It accepts a stock ticker symbol, runs a structured research pipeline involving specialized AI agents, and produces a final investment recommendation with human-in-the-loop approval.

### Core Workflow

```
User submits ticker
       │
       ▼
┌─────────────────────┐
│  OrchestratorAgent   │  ← Resolve identity, generate research plan, HITL approval
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│    DebateAgent       │  ← Orchestrates the full pipeline:
│                      │    1. Generate 4 analyst reports
│                      │    2. Distill into debate briefs
│                      │    3. Run bull/bear debate loop
│                      │    4. Run risk debate
│                      │    5. Portfolio manager decision
│                      │    6. HITL review
│                      │    7. Final investment plan
└─────────┬───────────┘
          │
          ▼
   Final investment plan → stored to decision memory
```

### Key Design Patterns

- **Multi-agent architecture:** Each agent has a distinct role, blackboard, and planner
- **Debate-driven decision making:** Bull vs. bear arguments converge through iterative debate
- **Human-in-the-loop (HITL):** WaitFor checkpoints allow human review at critical junctures
- **Structured LLM output:** Agents use typed output records with free-text fallback
- **Disk-based caching:** All LLM calls and API responses are cached on disk for idempotency
- **Decision memory:** Past decisions are resolved with actual returns and LLM reflections are stored for future context injection

---

## 2. Architecture Principles

1. **Agents are first-class citizens.** Each `@Agent` is an independent Spring component with its own blackboard, planner, and set of `@Action` methods.
2. **Subprocess isolation.** Sub-agents communicate via `asSubProcess()` with isolated blackboards — no shared mutable state.
3. **Synchronous LLM calls.** All LLM interactions use the synchronous `createObject()` API; no manual streaming.
4. **Structured output with fallback.** Agents attempt typed output first, fall back to free-text on failure.
5. **Cache everything.** FileCache provides per-key locking, SHA-256 hashed paths, and atomic writes.
6. **Sanitize all user input.** Jinja injection prevention is mandatory for any user-provided text entering prompts.
7. **Deterministic agent discovery.** Agents are auto-discovered via `@Agent` annotation scanning.

---

## 3. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         User Interface Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │  HTMX Web    │  │  REST API    │  │  Spring Shell (CLI)      │  │
│  │  (Thymeleaf) │  │  (JSON)      │  │  (TickerShellCommands)   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────────┘  │
└─────────┼────────────────┼──────────────────────┼──────────────────┘
          │                │                      │
          ▼                ▼                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Agent Platform Layer                         │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              Embabel Agent Framework (Spring AI)                │ │
│  │  @Agent  @Action  @AchievesGoal  Blackboard  Planner  HITL     │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │Orchestrator  │  │  Debate      │  │  DebateLoopAgent          │  │
│  │  Agent       │  │  Agent       │  │  (Bull/Bear loop)         │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────────┘  │
│         │                 │                      │                   │
│  ┌──────┴───────┐  ┌──────┴───────┐  ┌──────────┴───────────────┐  │
│  │RiskDebate   │  │  Trader      │  │  PortfolioManager         │  │
│  │  Agent       │  │  Agent       │  │  Agent                    │  │
│  └──────┬───────┘  └──────────────┘  └──────────────────────────┘  │
│         │                                                            │
│  ┌──────┴────────────────────────────────────────────────────────┐  │
│  │  Risk Debators: Aggressive | Conservative | Neutral            │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │BullResearcher│  │BearResearcher│  │InstrumentIdentityAgent    │  │
│  └──────────────┘  └──────────────┘  └──────────────────────────┘  │
│                                                                      │
│  ┌──────────────┐  ┌──────────────────────────────────────────┐    │
│  │DecisionMemory│  │  CheckpointAgent / CheckpointStore        │    │
│  │  Agent       │  │  (Crash recovery via JSON snapshots)      │    │
│  └──────────────┘  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────┬──────────────────────────┘
                                           │
┌──────────────────────────────────────────┼──────────────────────────┐
│                         Data Flow Layer                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │AlphaVantage  │  │ YFinService  │  │ FredService               │  │
│  │  Service      │  │ (Yahoo Fin.) │  │ (Federal Reserve)         │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────────┘  │
│         │                 │                      │                   │
│  ┌──────┴──────────────────────────────────────────────┐           │
│  │              VendorRouter                           │           │
│  │  (Routes LLM tool calls to appropriate vendor)      │           │
│  └─────────────────────────────────────────────────────┘           │
│  ┌─────────────────────────────────────────────────────┐           │
│  │              PolymarketService                      │           │
│  └─────────────────────────────────────────────────────┘           │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  LLM Tools (exposed to agents via Spring AI @Tool):            │ │
│  │  FundamentalDataTools | MarketDataTools | NewsDataTools        │ │
│  │  FredDataTools | PolymarketDataTools                           │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
                                           │
┌──────────────────────────────────────────┼──────────────────────────┐
│                      Persistence & Utilities                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │  FileCache   │  │ DecisionMem. │  │  CheckpointStore         │  │
│  │  (LLM cache) │  │ Repository   │  │  (JSON snapshots)        │  │
│  └──────────────┘  └──────────────┘  └──────────────────────────┘  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │ IndicatorMapper│ │ DateUtils   │  │  AgentUtils              │  │
│  └──────────────┘  └──────────────┘  └──────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
                                           │
┌──────────────────────────────────────────┼──────────────────────────┐
│                       External Services                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │ Alpha Vantage│  │ Yahoo Fin.   │  │  FRED API                 │  │
│  │ (Fundamentals│  │ (OHLCV data) │  │ (Macro indicators)        │  │
│  │  & News)     │  │              │  │                          │  │
│  └──────────────┘  └──────────────┘  └──────────────────────────┘  │
│  ┌──────────────┐  ┌──────────────────────────────────────────┐    │
│  │ Polymarket   │  │  LLM Endpoint (LiteLLM/OpenAI-compatible)│    │
│  │ (Prediction  │  │  e.g., Qwen3.6-35B-A3B at spark.local:4000│    │
│  │  Markets)    │  │                                          │    │
│  └──────────────┘  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 4. Component Reference

### 4.1 Entry Point

| Component | Class | Responsibility |
|-----------|-------|----------------|
| Application | `GekkoApplication` | Spring Boot main class. Enables agent scanning with `@EnableAgents(loggingTheme = "gekko")`. Registers AOT runtime hints for GraalVM native compilation. |
| Shell Commands | `TickerShellCommands` | Spring Shell CLI entry point (placeholder). |

### 4.2 Configuration

| Component | Class | Responsibility |
|-----------|-------|----------------|
| Agent Scanning | `AgentScanningConfiguration` | Registers Embabel's `BeanPostProcessor` for automatic `@Agent` annotation discovery. Enables both annotation-based and bean-based scanning. |
| Agent Config | `TraderAgentConfig` | Configuration properties bound to `app.llm-options.*`. Key fields: `similarityThreshold` (default 0.8), `maxDebateIterations` (default 5), LLM options for ticker/writer roles, researcher/writer personas. |

### 4.3 Domain Models

All domain models are **Java records** (immutable value types). See [Section 8](#8-data-models) for full schema.

| Package | Types |
|---------|-------|
| `com.embabel.gekko.domain` | `ResearchTypes` (DebateBriefs, InvestmentDebateState, InvestmentPlan, Ticker, InvestmentReviewFeedback, ResearchPlan, PlanApproval), `Analysts` (FundamentalsReport, MarketReport, NewsReport, SocialMediaReport), `PortfolioDecisionOutput`, `PortfolioRating`, `ResearchPlanOutput`, `SentimentBand`, `SentimentReportOutput`, `TraderAction`, `TraderProposalOutput` |

### 4.4 Web Controllers

| Component | Class | Base Path | Protocol |
|-----------|-------|-----------|----------|
| HTMX Controller | `TradingHtmxController` | `/`, `/research` | HTML + HTMX (server-rendered Thymeleaf) |
| REST Controller | `TradingApiController` | `/api` | JSON |
| Platform Controller | `PlatformController` | `/platform` | Simple home page |
| HITL Status Controller | `ProcessStatusController` | `/status/{processId}` | HTML (HITL workflow) |

### 4.5 HITL Infrastructure

| Component | Class | Responsibility |
|-----------|-------|----------------|
| HITL Service | `HitlService` | In-memory session store (ConcurrentHashMap). TTL-based cleanup (24h default). Atomic operations via `computeIfAbsent` and `compute`. |
| HITL Event Listener | `HitlAgenticEventListener` | Listens to `AgentProcessFinishedEvent`. Creates HITL session on `FAILED` status, removes on `COMPLETED`. Ordered at 100 (before logging). |
| HITL Config | `HitlConfig` | Spring `@Bean` definition for `HitlService` with configurable TTL. |

### 4.6 Observability

| Component | Class | Responsibility |
|-----------|-------|----------------|
| Chat Model Filter | `ChatModelCompletionContentObservationFilter` | Micrometer observation filter. Extracts prompt and completion text from `ChatModelObservationContext` as high-cardinality key-values for OpenTelemetry tracing. |

### 4.7 Native Image Support

| Component | Class | Responsibility |
|-----------|-------|----------------|
| Runtime Hints | `TraderAgentRuntimeHintsRegistrar` | Registers reflection hints for all agent classes and shared record types. Enables GraalVM native-image compilation. |
| Reflect Config | `META-INF/native-image/.../reflect-config.json` | JSON reflection configuration for Embabel agent API and Ollama autoconfigure. |

---

## 5. Agent Architecture

### 5.1 Agent Hierarchy

```
OrchestratorAgent (entry point)
    │
    └─► DebateAgent (full pipeline orchestration)
            │
            ├─► DebateLoopAgent (subprocess: bull/bear debate)
            │       ├─ BullResearcher
            │       └─ BearResearcher
            │
            ├─► RiskDebateAgent (subprocess: 3-round risk debate)
            │       ├─ AggressiveDebator
            │       ├─ ConservativeDebator
            │       └─ NeutralDebator
            │
            ├─ Trader (transaction proposal)
            ├─ PortfolioManager (final decision)
            └─ DecisionMemoryAgent (learning from outcomes)
```

### 5.2 OrchestratorAgent

**Role:** Entry point. Accepts ticker input, resolves instrument identity, generates research plan, waits for HITL approval, delegates to DebateAgent.

**Actions:**
| Action | Description | LLM Role | Template |
|--------|-------------|----------|----------|
| `resolveIdentity()` | Resolves ticker to company metadata via YFinService | — | — |
| `generateResearchPlan()` | Generates research plan for ticker | BEST | `managers/ResearchManager` |
| `waitForReview()` | WaitFor HITL checkpoint for plan approval | — | — |
| `runDebate()` | Delegates full pipeline to DebateAgent via `asSubProcess` | — | — |
| `storeFinalDecision()` | Stores final plan to decision memory | — | — |

**Key behavior:**
- Uses `FileCache` for caching research plans
- `asSubProcess(InvestmentPlan.class, debateAgent)` creates isolated blackboard for DebateAgent
- `WaitFor.formSubmission()` creates a HITL checkpoint before final plan generation
- `sanitizeForPrompt()` strips Jinja syntax, control characters, and truncates to 1000 chars

### 5.3 DebateAgent

**Role:** Orchestrates the full research pipeline. This is the main workflow coordinator.

**Pipeline (sequential):**

```
1. generateFundamentalsReport()  → FundamentalsReport
2. generateMarketReport()        → MarketReport
3. generateNewsReport()          → NewsReport
4. generateSocialMediaReport()   → SocialMediaReport
5. prepareDebateBriefs()         → DebateBriefs (distills each report)
6. runDebate()                   → InvestmentDebateState (DebateLoopAgent subprocess)
7. runTrader()                   → String (TraderProposalOutput)
8. runRiskDebate()               → RiskAssessment (RiskDebateAgent subprocess)
9. runPortfolioManager()         → String (PortfolioDecisionOutput)
10. waitForReview()              → InvestmentReviewFeedback (HITL checkpoint)
11. researchManager()            → InvestmentPlan (final plan with feedback)
12. storeFinalDecision()         → void (persist to memory)
```

**Caching strategy:** Every report generation and debate brief distillation is cached via `FileCache.getOrCompute()`. Cache keys are derived from ticker + report type (e.g., `AAPL_fundamentals`).

**Sanitization:** `sanitizeForPrompt()` prevents Jinja template injection from user feedback:
- Blocks `{{ ... }}`, `{% ... %}` patterns
- Blocks markdown code fences
- Strips control characters (except newline/tab/CR)
- Truncates to 1000 characters
- Wraps in `<user_feedback>` XML tags

### 5.4 DebateLoopAgent

**Role:** Runs the iterative bull/bear debate loop with convergence detection.

**Algorithm:**
```
RepeatUntil convergence or maxIterations:
  1. BullResearcher.argue(briefs, history) → bullResponse
  2. BearResearcher.argue(briefs, history) → bearResponse
  3. Append both to history
  4. Compute Jaccard bigram similarity between consecutive bull responses
  5. If similarity >= threshold → stop (converged)
```

**Convergence detection:** Jaccard similarity on character bigrams between consecutive bull responses. Default threshold: 0.8. Default max iterations: 5.

**Output:** `InvestmentDebateState` containing:
- `history`: Full debate transcript
- `bullHistory`: Bull arguments only
- `bearHistory`: Bear arguments only
- `count`: Total debate turns
- `briefs`: The debate briefs used

### 5.5 RiskDebateAgent

**Role:** Runs a 3-round structured risk debate with three debator personas.

**Algorithm:**
```
For round 0..2:
  1. AggressiveDebator.argue(traderDecision, reports, history, conservative, neutral)
  2. ConservativeDebator.argue(traderDecision, reports, history, aggressive, neutral)
  3. NeutralDebator.argue(traderDecision, reports, history, aggressive, conservative)
  4. Append all to history

Judge: RiskManager template → RiskAssessmentOutput → RiskAssessment
Fallback: Free-text parsing with keyword heuristics
```

**Risk levels:** `RISKY`, `NEUTRAL`, `CONSERVATIVE`

### 5.6 Trader Agent

**Role:** Translates the Research Manager's investment plan into a concrete transaction proposal.

**Output:** `TraderProposalOutput` with:
- `action`: BUY / HOLD / SELL
- `reasoning`: 2-4 sentence justification
- `entryPrice`: Optional target entry price
- `stopLoss`: Optional stop-loss price
- `positionSizing`: Optional sizing guidance (e.g., "5% of portfolio")

### 5.7 Portfolio Manager

**Role:** Judges the risk debate, reads the Research Plan and Trader's proposal, and produces the final structured portfolio decision.

**Output:** `PortfolioDecisionOutput` with:
- `rating`: BUY / OVERWEIGHT / HOLD / UNDERWEIGHT / SELL
- `executiveSummary`: Concise action plan
- `investmentThesis`: Detailed reasoning
- `priceTarget`: Optional target price
- `timeHorizon`: Optional holding period

### 5.8 Decision Memory Agent

**Role:** Learns from past trading outcomes. Stores decisions, resolves them with actual returns, and injects past context into future decisions.

**Actions:**
| Action | Description |
|--------|-------------|
| `storeDecision()` | Stores a pending decision (ticker, date, rating, summary, thesis) to file |
| `resolvePending()` | Fetches 5-day return from Yahoo Finance, generates LLM reflection, updates entry to resolved |
| `generatePastContext()` | Generates context string: up to 5 same-ticker + 3 cross-ticker lessons |
| `fetchReturns()` | Computes raw return and alpha vs SPY benchmark from OHLCV data |

**Memory format:** Markdown file with entries separated by `<!-- ENTRY_END -->`. Each entry has a header `[date | ticker | rating | status]` and optional DECISION/REFLECTION blocks.

### 5.9 Instrument Identity Agent

**Role:** Resolves a ticker symbol to real company metadata (name, sector, industry, exchange, currency) to prevent LLM hallucination.

**Data source:** Yahoo Finance API via `YFinService.getTickerInfo()`.
**Cache:** FileCache with 24-hour TTL, key prefix `identity:`.
**Validation:** Ticker must match `^[A-Z0-9.\\-]+$`.

### 5.10 Checkpoint Agent

**Role:** Crash recovery via blackboard snapshot persistence.

**Mechanism:**
- After each phase completes, `saveCheckpoint()` writes the blackboard state to a JSON file
- On restart, `restoreCheckpoint()` reads the last completed phase and restores state
- `clearCheckpoint()` removes the checkpoint on successful completion
- Atomic writes (temp file + rename) prevent corruption
- Path traversal protection on ticker filenames

**File format:** `data/checkpoints/<TICKER>.json`
```json
{
  "ticker": "AAPL",
  "tradeDate": "2026-01-15",
  "lastCompletedPhase": "debate",
  "phases": {
    "reports": {"blackboard": {...}},
    "debate": {"blackboard": {...}}
  },
  "savedAt": "2026-01-15T10:30:00"
}
```

---

## 6. Data Flow

### 6.1 Complete Pipeline Flow

```
User Input (Ticker: "AAPL")
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ OrchestratorAgent.resolveIdentity()                          │
│   └─► YFinService.getTickerInfo("AAPL")                      │
│       └─► InstrumentContext: {ticker, name, sector, ...}     │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ OrchestratorAgent.generateResearchPlan()                     │
│   └─► FileCache.getOrCompute("AAPL_research_manager")        │
│       └─► LLM (BEST) → ResearchPlan                          │
│   └─► WaitFor.formSubmission() → HITL checkpoint             │
└─────────────────────────────────────────────────────────────┘
    │  (user approves)
    ▼
┌─────────────────────────────────────────────────────────────┐
│ OrchestratorAgent.runDebate() → asSubProcess(DebateAgent)    │
│                                                              │
│  DebateAgent.generateFundamentalsReport()                    │
│   └─► FileCache.getOrCompute("AAPL_fundamentals")            │
│       └─► LLM (CHEAPEST) → FundamentalsReport               │
│                                                              │
│  DebateAgent.generateMarketReport()                          │
│   └─► FileCache.getOrCompute("AAPL_market")                  │
│       └─► LLM (CHEAPEST) → MarketReport                     │
│                                                              │
│  DebateAgent.generateNewsReport()                            │
│   └─► FileCache.getOrCompute("AAPL_news")                    │
│       └─► LLM (CHEAPEST) → NewsReport                       │
│                                                              │
│  DebateAgent.generateSocialMediaReport()                     │
│   └─► FileCache.getOrCompute("AAPL_social_media")            │
│       └─► LLM (CHEAPEST) → SocialMediaReport                │
│                                                              │
│  DebateAgent.prepareDebateBriefs()                           │
│   └─► FileCache.getOrCompute("AAPL_briefs")                  │
│       └─► LLM (CHEAPEST) × 4 → DebateBriefs                 │
│                                                              │
│  DebateAgent.runDebate() → asSubProcess(DebateLoopAgent)     │
│   └─► RepeatUntil convergence:                               │
│       ├─ BullResearcher.argue() → LLM (BEST)                │
│       ├─ BearResearcher.argue() → LLM (BEST)                │
│       └─ Jaccard bigram similarity check                    │
│   └─► InvestmentDebateState                                  │
│                                                              │
│  DebateAgent.runTrader() → Trader.traderProposal()           │
│   └─► LLM (BEST) → TraderProposalOutput                      │
│                                                              │
│  DebateAgent.runRiskDebate() → asSubProcess(RiskDebateAgent) │
│   └─► 3 rounds of:                                          │
│       ├─ AggressiveDebator.argue()                          │
│       ├─ ConservativeDebator.argue()                        │
│       └─ NeutralDebator.argue()                             │
│   └─► LLM (BEST) → RiskAssessment (RISKY/NEUTRAL/CONSERVATIVE)
│                                                              │
│  DebateAgent.runPortfolioManager() → PortfolioManager        │
│   └─► LLM (BEST) → PortfolioDecisionOutput                   │
│                                                              │
│  DebateAgent.waitForReview() → WaitFor.formSubmission()      │
│   └─► HITL checkpoint: user reviews debate, provides feedback│
│                                                              │
│  DebateAgent.researchManager()                               │
│   └─► FileCache.getOrCompute("AAPL_research_manager")        │
│       └─► LLM (BEST) → InvestmentPlan                        │
│                                                              │
│  DebateAgent.storeFinalDecision()                            │
│   └─► DecisionMemoryRepository.appendPending()               │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ Later: DecisionMemoryAgent.resolvePending()                  │
│   └─► YFinService.getYFinDataOnline("AAPL", tradeDate, +5d)  │
│   └─► LLM (BEST) → Reflection                                │
│   └─► DecisionMemoryRepository.resolve()                     │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 Tool Call Flow (LLM → External Data)

```
Agent @Action method
    │
    ▼
LLM tool calling (Spring AI)
    │
    ▼
┌──────────────────────────────────────────────────┐
│ FundamentalDataTools                             │
│  ├─ get_fundamentals() ──► VendorRouter          │
│  ├─ get_balance_sheet() ──► VendorRouter         │
│  ├─ get_cashflow() ──► VendorRouter             │
│  └─ get_income_statement() ──► VendorRouter      │
├──────────────────────────────────────────────────┤
│ MarketDataTools                                  │
│  ├─ get_stock_data() ──► YFinService             │
│  └─ get_indicators() ──► YFinService             │
├──────────────────────────────────────────────────┤
│ NewsDataTools                                    │
│  ├─ get_news() ──► VendorRouter                  │
│  ├─ get_global_news() ──► VendorRouter           │
│  ├─ get_insider_sentiment() ──► VendorRouter     │
│  └─ get_insider_transactions() ──► VendorRouter   │
├──────────────────────────────────────────────────┤
│ FredDataTools                                    │
│  ├─ getMacroIndicators() ──► FredService         │
│  └─ getMacroDashboard() ──► FredService          │
├──────────────────────────────────────────────────┤
│ PolymarketDataTools                              │
│  └─ getPredictionMarkets() ──► PolymarketService │
└──────────────────────────────────────────────────┘
    │
    ▼
VendorRouter.switch on method name → AlphaVantageService
    │
    ▼
FileCache.getOrCompute() → HTTP GET → Response
```

### 6.3 HITL Flow

```
Agent process runs → enters WAITING state (WaitFor.formSubmission)
    │
    ▼
Web UI polls /status/{processId}
    │
    ▼
ProcessStatusController detects WAITING → renders waiting.html
    │
    ▼
User reviews debate → submits form (feedback + approved)
    │
    ▼
POST /status/{processId}/waitfor
    │
    ├─ Find FormBindingRequest on blackboard
    ├─ Build FormSubmission from form fields
    ├─ FormBindingRequest.onResponse() → bind to blackboard
    ├─ agentPlatform.start(process) → resume
    └─ Process continues to researchManager()
```

---

## 7. External Dependencies

### 7.1 LLM Endpoint

| Property | Value |
|----------|-------|
| Protocol | OpenAI-compatible API |
| Base URL | `${OPENAI_BASE_URL:http://spark.local:4000}` (LiteLLM) |
| API Key | `${OPENAI_API_KEY:dummy}` |
| Model | `${OPENAI_MODEL:Qwen3.6-35B-A3B}` |
| Roles | `cheapest` and `best` both map to the same model (configurable) |
| Framework | Embabel `openai-custom` starter |

**Model role usage:**
- `CHEAPEST_ROLE`: Used for analyst report generation and debate brief distillation (cost optimization)
- `BEST_ROLE`: Used for debate arguments, risk assessment, portfolio decision, final plan (quality)

### 7.2 Data Providers

| Provider | Service | API Key | Purpose |
|----------|---------|---------|---------|
| Alpha Vantage | `AlphaVantageService` | `${ALPHAVANTAGE_API_KEY}` | Fundamentals, balance sheet, cash flow, income statement, news, insider sentiment/transactions |
| Yahoo Finance | `YFinService` | None | OHLCV historical data, company metadata, TA4J indicator calculation |
| FRED | `FredService` | `${FRED_API_KEY}` | Macroeconomic indicators (GDP, CPI, unemployment, Fed funds rate) |
| Polymarket | `PolymarketService` | None | Prediction market outcomes and probabilities |

**Alpha Vantage endpoints:**
- `OVERVIEW` — company fundamentals
- `BALANCE_SHEET` — balance sheet (quarterly/annual)
- `CASH_FLOW` — cash flow statement
- `INCOME_STATEMENT` — income statement
- `NEWS_SENTIMENT` — news with sentiment scores
- `INSIDER_SENTIMENT` — insider sentiment data
- `INSIDER_TRANSACTIONS` — insider transaction history

**FRED series IDs:** `GDP`, `CPIAUCSL`, `UNRATE`, `FEDFUNDS`, `TB3MS`

### 7.3 Technical Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.5.13 | Application framework |
| Embabel Agent | 0.5.0-SNAPSHOT | Multi-agent framework (Spring AI based) |
| TA4J | 0.18 | Technical analysis indicators |
| YahooFinanceAPI | 3.17.0 | Yahoo Finance Java client |
| Lombok | — | Boilerplate reduction |
| Micrometer Tracing + OTLP | — | OpenTelemetry observability |
| Thymeleaf | — | Server-side HTML templating |
| Jackson | — | JSON serialization |

### 7.4 Optional Dependencies (provided scope)

| Dependency | Purpose |
|------------|---------|
| `embabel-agent-starter-anthropic` | Anthropic Claude model support |
| `embabel-agent-starter-bedrock` | AWS Bedrock model support |
| `embabel-agent-starter-ollama` | Local Ollama model support |

---

## 8. Data Models

### 8.1 Shared Types (`ResearchTypes`)

```java
// Debate briefs from 4 analyst reports
record DebateBriefs(
    String fundamentalsBrief,
    String marketBrief,
    String newsBrief,
    String socialBrief
)

// State of the bull/bear debate
record InvestmentDebateState(
    List<String> history,          // Full debate transcript
    List<String> bullHistory,       // Bull arguments only
    List<String> bearHistory,       // Bear arguments only
    String currentResponse,         // Last argument
    int count,                      // Total turns
    DebateBriefs briefs,            // Source briefs
    RiskAssessment riskAssessment,  // Risk assessment (injected later)
    String latestSpeaker,           // Risk debate: last speaker
    String currentAggressiveResponse,
    String currentConservativeResponse,
    String currentNeutralResponse,
    String traderProposal           // Trader's proposal
)

// Final investment plan
record InvestmentPlan(
    String judgeDecision,           // The final plan text
    InvestmentDebateState investmentDebateState
)

// Input ticker with optional feedback
record Ticker(
    String content,                 // Ticker symbol
    String feedback                 // User feedback (from HITL)
)

// HITL review feedback
record InvestmentReviewFeedback(
    String feedback,                // User feedback text
    boolean approved                // Whether to proceed
)
```

### 8.2 Analyst Reports

```java
record FundamentalsReport(String content) implements Report
record MarketReport(String content) implements Report
record NewsReport(String content) implements Report
record SocialMediaReport(String content) implements Report
```

### 8.3 Portfolio Decision

```java
record PortfolioDecisionOutput(
    PortfolioRating rating,         // BUY/OVERWEIGHT/HOLD/UNDERWEIGHT/SELL
    String executiveSummary,        // Concise action plan
    String investmentThesis,        // Detailed reasoning
    BigDecimal priceTarget,         // Optional target price
    String timeHorizon             // Optional holding period
)

enum PortfolioRating { BUY, OVERWEIGHT, HOLD, UNDERWEIGHT, SELL }
```

### 8.4 Trader Proposal

```java
record TraderProposalOutput(
    TraderAction action,            // BUY/HOLD/SELL
    String reasoning,               // 2-4 sentence justification
    BigDecimal entryPrice,          // Optional target entry price
    BigDecimal stopLoss,            // Optional stop-loss price
    String positionSizing           // Optional sizing guidance
)

enum TraderAction { BUY, HOLD, SELL }
```

### 8.5 Risk Assessment

```java
record RiskAssessment(
    RiskLevel level,               // RISKY/NEUTRAL/CONSERVATIVE
    String reasoning               // Justification
)

enum RiskLevel { RISKY, NEUTRAL, CONSERVATIVE }
```

### 8.6 Decision Memory

```java
// Pending decision awaiting resolution
record PendingDecision(
    String ticker,
    String tradeDate,
    String rating,
    String executiveSummary,
    String investmentThesis,
    LocalDateTime storedAt
)

// Resolved decision with actual returns
record ResolvedDecision(
    String ticker,
    String tradeDate,
    String rating,
    BigDecimal rawReturn,
    BigDecimal alphaReturn,
    String benchmark,
    int daysHeld,
    String reflection,
    LocalDateTime storedAt,
    LocalDateTime resolvedAt
)
```

### 8.7 Instrument Identity

```java
record InstrumentContext(
    String ticker,
    String companyName,
    String sector,
    String industry,
    String exchange,
    String currency
)
```

### 8.8 HITL Session

```java
record HitlSession(
    String processId,
    String agentName,
    String errorMessage,
    LocalDateTime occurredAt,
    String userInput,
    String feedback,
    boolean userActionTaken
)
```

### 8.9 Checkpoint Entry

```java
record CheckpointEntry(
    String ticker,
    String tradeDate,
    String lastCompletedPhase,
    Map<String, Object> phases     // phase → {blackboard: {...}}
)
```

---

## 9. Persistence Layer

### 9.1 FileCache (LLM Cache)

| Property | Value |
|----------|-------|
| Location | `data/llm/cache/` |
| File naming | SHA-256 hash of cache key + `.json` or `.md` extension |
| Concurrency | Per-key `synchronized` locks via `ConcurrentHashMap` |
| Atomicity | Temp file + `Files.move()` with `REPLACE_EXISTING` |
| Content | JSON for typed records, Markdown for strings |

**Usage pattern:**
```java
cache.getOrCompute(key, ResultType.class, () -> {
    // Compute value (e.g., LLM call)
    return computedValue;
});
```

### 9.2 Decision Memory Repository

| Property | Value |
|----------|-------|
| Location | `~/.tradingagents/memory/trading_memory.md` (configurable) |
| Format | Markdown with `<!-- ENTRY_END -->` separators |
| Atomicity | Temp file + rename |
| Rotation | Prune oldest resolved entries when over `app.memory.log-max-entries` |
| Corruption recovery | Truncate to last complete entry |

**Entry format:**
```
[2026-01-15 | AAPL | Buy | pending]

DECISION:
**Rating**: Buy

**Executive Summary**: Strong conviction to enter position.

**Investment Thesis**: Revenue growth accelerating...

<!-- ENTRY_END -->

[2026-01-15 | AAPL | Buy | +3.2% | +1.5% | 5d]

DECISION:
**Rating**: Buy

REFLECTION:
The buy call was correct because...

<!-- ENTRY_END -->
```

### 9.3 Checkpoint Store

| Property | Value |
|----------|-------|
| Location | `data/checkpoints/<TICKER>.json` (configurable) |
| Format | JSON with phase → blackboard state mapping |
| Atomicity | Temp file + rename |
| Security | Path traversal protection on ticker filenames |

### 9.4 Alpha Vantage Cache

| Property | Value |
|----------|-------|
| Location | `data/alphavantage/` (configurable) |
| File naming | `<cacheKey>.json` |
| Atomicity | Standard write (no temp file) |

---

## 10. Web Interface

### 10.1 HTMX Web Application

**Template structure:**
```
templates/
├── form.html                    # Ticker input form
├── plan-review.html             # Plan review page (WAITING state)
├── plan.html                    # Completed plan display
└── common/
    ├── layout.html              # Base layout (CSS, navigation)
    ├── platform.html            # Platform home page
    ├── processing.html          # Processing status (SSE updates)
    ├── processing-error.html    # Error display
    ├── hitl.html                # HITL form for failed processes
    ├── waiting.html             # WaitFor HITL form (debate review)
    ├── user-info.html           # User info fragment
    └── fragments/
        ├── empty.html           # Empty fragment placeholder
        ├── footer.html          # Footer fragment
        └── plan-complete.html   # Plan complete fragment
```

**CSS:** `static/css/project.css` (dark theme), `static/css/embabel-common-dark.css` (framework dark theme)

**Key endpoints:**

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/` | Show ticker input form |
| POST | `/plan` | Start research pipeline |
| GET | `/plan/review/{processId}` | Review generated plan (WAITING) |
| POST | `/plan/review/{processId}` | Submit plan approval |
| GET | `/plan/status/{processId}` | Poll process status |
| GET | `/status/{processId}` | HITL status polling |
| POST | `/status/{processId}/resubmit` | Resubmit failed process with feedback |
| POST | `/status/{processId}/waitfor` | Submit WaitFor HITL form |

### 10.2 REST API

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/api/plan` | `{ticker, feedback?}` | `{processId, status, plan?, message}` |
| GET | `/api/plan/{processId}/status` | — | `{processId, status, message, plan?, investmentPlan?}` |
| POST | `/api/plan/{processId}/approve` | `{approved, feedback?}` | `{processId, status, message}` |

### 10.3 State Machine

```
                    ┌──────────────────────────────────┐
                    │          RUNNING                  │
                    │  (agent process executing)        │
                    └──────────┬───────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
    ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
    │   WAITING     │  │   FAILED      │  │ COMPLETED     │
    │ (HITL waiting │  │ (HITL session │  │ (final plan   │
    │  for approval)│  │  available)   │  │  stored)      │
    └───────┬───────┘  └───────┬───────┘  └───────────────┘
            │                   │
            ▼                   ▼
    ┌───────────────┐   ┌───────────────┐
    │  Plan approved│   │  Resubmit     │
    │  → RUNNING    │   │  → RUNNING    │
    └───────────────┘   └───────────────┘
```

---

## 11. Configuration

### 11.1 Application Properties

```yaml
spring:
  application:
    name: Gekko
  profiles:
    active: base,app,observability,local

embabel:
  models:
    default-llm: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
    llms:
      cheapest: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
      best: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
  agent:
    platform:
      models:
        openai:
          custom:
            api-key: ${OPENAI_API_KEY:dummy}
            base-url: ${OPENAI_BASE_URL:http://spark.local:4000}
            models: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
      scanning:
        annotation: true
        bean: true

app:
  memory:
    log-path: ~/.tradingagents/memory/trading_memory.md
    log-max-entries: 0
  checkpoint:
    enabled: false
    dir: data/checkpoints
  fred:
    api-key: ${FRED_API_KEY:}
    enabled: true
  polymarket:
    enabled: true
  llm-options:
    provider: openai-compat
    best-model: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
    cheapest-model: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
  alphavantage:
    api-key: ${ALPHAVANTAGE_API_KEY:}
    output-directory: data/alphavantage
    connect-timeout-ms: 10000
    read-timeout-ms: 30000
```

### 11.2 LLM Options Record

```java
record LlmOptions(
    String provider,           // "openai-compat", "anthropic", "google"
    String bestModel,
    String cheapestModel,
    LlmOptions.AnthropicOptions anthropic,
    LlmOptions.GoogleOptions google,
    LlmOptions.OpenAiOptions openai
)
```

### 11.3 Agent Configuration

```java
record TraderAgentConfig(
    LlmOptions tickerLlm,
    LlmOptions writerLlm,
    int maxConcurrency,
    RoleGoalBackstory researcher,
    RoleGoalBackstory outliner,
    RoleGoalBackstory writer,
    String outputDirectory,
    double similarityThreshold,   // Default: 0.8
    int maxDebateIterations       // Default: 5
)
```

---

## 12. Observability

### 12.1 Tracing

| Component | Purpose |
|-----------|---------|
| Micrometer Tracing | OpenTelemetry bridge |
| OTLP Exporter | Export traces to collector |
| ChatModelCompletionContentObservationFilter | Extracts prompt/completion text from LLM calls as trace attributes |

### 12.2 Logging

- Package-level logging: `com.embabel` at `WARN` level
- Custom logging theme: `gekko` (color palette defined in `GekkoColorPalette`)
- Custom event listener: `GekkoLoggingAgenticEventListener`

### 12.3 Actuator

Spring Boot Actuator endpoints available for health checks and metrics.

---

## 13. Security

### 13.1 Input Sanitization

All user-provided text entering prompts is sanitized via `sanitizeForPrompt()`:

1. Block Jinja template expressions (`{{ ... }}`, `{% ... %}`)
2. Block markdown code fences (```)
3. Strip control characters (except `\t`, `\n`, `\r`)
4. Truncate to 1000 characters
5. Wrap in XML tags (`<user_feedback>`)

### 13.2 Path Traversal Protection

- FileCache: Sanitizes cache keys, strips `..`, `/`, `\`, null bytes, shell metacharacters
- CheckpointStore: Validates ticker matches `^[A-Za-z0-9._-]+$`, verifies resolved path stays within checkpoint directory
- Alpha Vantage cache: No user input directly used as file path

### 13.3 Concurrency Safety

- HITL sessions: `ConcurrentHashMap` with `computeIfAbsent` / `compute` for atomic operations
- WaitFor submissions: `synchronized (agentProcess)` blocks
- Cache: Per-key locking via `ConcurrentHashMap`
- Decision memory: Atomic writes with temp file + rename

---

## 14. Deployment

### 14.1 Build

```bash
./mvnw verify
```

### 14.2 Run

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 14.3 Native Image

```bash
./mvnw verify -Pnative
```

Uses GraalVM native-image with custom reflection hints registered via `TraderAgentRuntimeHintsRegistrar`.

### 14.4 Docker

`docker-compose.yml` / `compose.yml` for containerized deployment.

### 14.5 Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `OPENAI_BASE_URL` | `http://spark.local:4000` | LLM endpoint URL |
| `OPENAI_API_KEY` | `dummy` | LLM API key |
| `OPENAI_MODEL` | `Qwen3.6-35B-A3B` | LLM model name |
| `ALPHAVANTAGE_API_KEY` | — | Alpha Vantage API key |
| `FRED_API_KEY` | — | FRED API key |

---

## 15. Testing

### 15.1 Test Categories

| Category | Tests |
|----------|-------|
| Agent detection | `AgentDetectionIntegrationTest` |
| LLM integration | `DebateAgentLLMTest`, `TraderAgentLLMTest`, `RiskDebateServiceLLMTest`, `ResearcherLLMTest` |
| Unit tests | `DebateBriefsUnitTest`, `RiskDebateServiceUnitTest`, `OrchestratorAgentResearchPlanTest`, `TraderAgentTickerValidationTest` |
| Data flow | `AlphaVantageServiceTest`, `VendorRouterTest` |
| HITL | `HitlServiceTest` |
| Indicators | `MFIIndicatorTest`, `SubtractIndicatorTest`, `VWAPIndicatorTest`, `VWMAIndicatorTest` |
| Tools | `FundamentalDataToolsTest`, `MarketDataToolsTest`, `NewsDataToolsTest` |
| Utilities | `DateUtilsTest`, `FileCacheTest`, `IndicatorMapperTest` |
| Identity | `InstrumentIdentityAgentTest` |
| Researchers | `BearResearcherTest`, `BullResearcherTest` |

### 15.2 Test Infrastructure

- `FakeActionContext` — Mock action context for unit testing agents without LLM
- `PureLogicTest` — Tests for pure logic without framework dependencies
- Embabel test starter: `embabel-agent-test` for agent testing utilities

---

## Appendix A: Prompt Template Reference

All prompt templates are Jinja2 templates stored in `src/main/resources/prompts/`.

### Analyst Templates

| Template | Role | LLM Role |
|----------|------|----------|
| `analysts/_BaseAnalyst.jinja` | Base template with tool context | — |
| `analysts/FundamentalsAnalyst.jinja` | Analyze financial statements, company profile | CHEAPEST |
| `analysts/MarketAnalyst.jinja` | Select and analyze technical indicators | CHEAPEST |
| `analysts/NewsAnalyst.jinja` | Analyze recent news and macro trends | CHEAPEST |
| `analysts/SocialMediaAnalyst.jinja` | Analyze social media sentiment and company news | CHEAPEST |

### Debate Templates

| Template | Role | LLM Role |
|----------|------|----------|
| `debate/Distiller.jinja` | Distill analyst report into structured brief | CHEAPEST |
| `researchers/BullResearcher.jinja` | Argue for the stock (bull case) | BEST |
| `researchers/BearResearcher.jinja` | Argue against the stock (bear case) | BEST |

### Manager Templates

| Template | Role | LLM Role |
|----------|------|----------|
| `managers/ResearchManager.jinja` | Evaluate debate, produce investment plan | BEST |
| `managers/Trader.jinja` | Produce transaction proposal | BEST |
| `managers/RiskManager.jinja` | Judge risk debate, produce risk assessment | BEST |
| `managers/PortfolioManager.jinja` | Final portfolio decision | BEST |

### Risk Templates

| Template | Role | LLM Role |
|----------|------|----------|
| `risk/AggressiveDebator.jinja` | Champion high-risk, high-reward | BEST |
| `risk/ConservativeDebator.jinja` | Protect capital, minimize risk | BEST |
| `risk/NeutralDebator.jinja` | Balanced perspective | BEST |

### Memory Templates

| Template | Role | LLM Role |
|----------|------|----------|
| `memory/reflection.jinja` | Analyze past decision outcome | BEST |

---

## Appendix B: TA4J Indicator Reference

| Code | Description | Implementation |
|------|-------------|----------------|
| `close_50_sma` | 50-period Simple Moving Average | `SMAIndicator(close, 50)` |
| `close_200_sma` | 200-period Simple Moving Average | `SMAIndicator(close, 200)` |
| `close_10_ema` | 10-period Exponential Moving Average | `EMAIndicator(close, 10)` |
| `macd` | MACD line (EMA12 - EMA26) | `SubtractIndicator(EMA12, EMA26)` |
| `macds` | MACD signal line (9-period EMA of MACD) | `EMAIndicator(macdLine, 9)` |
| `macdh` | MACD histogram (MACD - signal) | `SubtractIndicator(macdLine, signalLine)` |
| `rsi` | Relative Strength Index (14-period) | `RSIIndicator(close, 14)` |
| `vwma` | Volume Weighted Moving Average (20-period) | `VWAPIndicator(series, 20)` (custom) |
| `atr` | Average True Range (14-period) | `ATRIndicator(series, 14)` |
| `boll` | Bollinger Bands middle line (20 SMA) | `SMAIndicator(close, 20)` |

### Custom Indicators

| Class | Description |
|-------|-------------|
| `MFIIndicator` | Money Flow Index (TA4J custom) |
| `SubtractIndicator` | Subtracts two indicators (left - right) |
| `VWAPIndicator` | Rolling-window Volume Weighted Moving Average |
| `VWMAIndicator` | Volume Weighted Moving Average (rolling window) |

---

## Appendix C: Agent Annotation Reference

All agents use the Embabel annotation system:

| Annotation | Purpose |
|------------|---------|
| `@Agent` | Marks a class as an agent. Has `description` attribute. |
| `@Action` | Marks a method as an agent action. Has `description` attribute. |
| `@AchievesGoal` | Marks an action as achieving a specific goal. Has `description` attribute. |
| `@Component` | Spring component registration (all agents are Spring beans). |
| `@RegisterReflectionForBinding` | GraalVM native-image reflection hints for output types. |

**Agent discovery:** Automatic via `@EnableAgents` which scans for `@Agent`-annotated classes and bean definitions.

---

## Appendix D: Key Algorithms

### D.1 Jaccard Bigram Similarity (Debate Convergence)

```
function computeSimilarity(String a, String b):
    bigramsA = set of all 2-character substrings of lowercased a
    bigramsB = set of all 2-character substrings of lowercased b
    intersection = bigramsA ∩ bigramsB
    union = bigramsA ∪ bigramsB
    return |intersection| / |union|
```

### D.2 Decision Memory Context Generation

```
function generatePastContext(String ticker):
    entries = parse resolved decisions from memory file
    sameTicker = entries where entry.ticker == ticker (last 5)
    crossTicker = entries where entry.ticker != ticker (last 3)
    return "PAST DECISION MEMORY:\n" + join(sameTicker + crossTicker, "\n---\n")
```

### D.3 Risk Assessment Classification (Fallback)

```
function classifyRisk(String lower):
    if contains("buy") and contains any of ["risk", "bold", "aggressive", "high"]:
        return RISKY
    if contains any of ["sell", "avoid", "cautious", "conservative", "safe"]:
        return CONSERVATIVE
    return NEUTRAL
```

---

## Appendix E: File Structure Summary

```
src/main/java/com/embabel/gekko/
├── GekkoApplication.java                    # Spring Boot entry point
├── TickerShellCommands.java                 # CLI shell commands (placeholder)
├── agent/
│   ├── OrchestratorAgent.java               # Entry point orchestrator
│   ├── DebateAgent.java                     # Full pipeline coordinator
│   ├── DebateLoopAgent.java                 # Bull/bear debate loop
│   ├── RiskDebateAgent.java                 # 3-round risk debate
│   ├── Trader.java                          # Transaction proposal
│   ├── RiskAssessment.java                  # Risk assessment record
│   ├── RiskAssessmentOutput.java            # Risk assessment output record
│   ├── RiskLevel.java                       # Risk level enum
│   ├── checkpoint/
│   │   ├── CheckpointAgent.java             # Crash recovery agent
│   │   └── CheckpointStore.java             # JSON checkpoint persistence
│   ├── identity/
│   │   ├── InstrumentIdentityAgent.java     # Ticker → company metadata
│   │   ├── InstrumentContext.java           # Identity record
│   │   └── InstrumentContextPromptContributor.java
│   ├── managers/
│   │   └── PortfolioManager.java            # Final portfolio decision
│   ├── memory/
│   │   ├── DecisionMemoryAgent.java         # Learning from outcomes
│   │   ├── DecisionMemoryRepository.java    # File-based memory storage
│   │   ├── PendingDecision.java             # Pending decision record
│   │   └── ResolvedDecision.java            # Resolved decision record
│   ├── researchers/
│   │   ├── BullResearcher.java              # Bull case generator
│   │   └─ BearResearcher.java               # Bear case generator
│   └── risk/
│       ├── AggressiveDebator.java           # High-risk advocate
│       ├── ConservativeDebator.java         # Capital preservation
│       └── NeutralDebator.java              # Balanced perspective
├── config/
│   ├── AgentScanningConfiguration.java      # Agent auto-discovery
│   └── TraderAgentConfig.java               # Agent configuration
├── dataflows/
│   ├── AlphaVantageService.java             # Alpha Vantage API client
│   ├── FredService.java                     # FRED API client
│   ├── PolymarketService.java               # Polymarket API client
│   ├── VendorRouter.java                    # LLM tool call router
│   └── YFinService.java                     # Yahoo Finance client
├── domain/
│   ├── Analysts.java                        # Analyst report records
│   ├── PortfolioDecisionOutput.java         # Portfolio decision output
│   ├── PortfolioRating.java                 # Rating enum
│   ├── ResearchPlanOutput.java              # Research plan output
│   ├── ResearchTypes.java                   # Shared types
│   ├── SentimentBand.java                   # Sentiment enum
│   ├── SentimentReportOutput.java           # Sentiment report output
│   ├── TraderAction.java                    # Action enum
│   └── TraderProposalOutput.java            # Trader proposal output
├── htmx/
│   ├── GenericProcessingValues.java         # Processing state record
│   ├── HitlAgenticEventListener.java        # HITL event listener
│   ├── HitlConfig.java                      # HITL configuration
│   ├── HitlService.java                     # HITL session store
│   ├── PlatformController.java              # Platform home page
│   └── ProcessStatusController.java         # HITL workflow controller
├── indicators/
│   ├── MFIIndicator.java                    # Money Flow Index
│   ├── SubtractIndicator.java               # Indicator subtraction
│   ├── VWAPIndicator.java                   # VWAP indicator
│   └── VWMAIndicator.java                   # VWMA indicator
├── observability/
│   └── ChatModelCompletionContentObservationFilter.java
├── tools/
│   ├── FundamentalDataTools.java            # Fundamental data LLM tools
│   ├── MarketDataTools.java                 # Market data LLM tools
│   ├── NewsDataTools.java                   # News data LLM tools
│   ├── FredDataTools.java                   # FRED data LLM tools
│   └── PolymarketDataTools.java             # Polymarket LLM tools
├── util/
│   ├── AgentUtils.java                      # Shared agent utilities
│   ├── DateUtils.java                       # Date utilities
│   ├── FileCache.java                       # Disk-based LRU cache
│   └── IndicatorMapper.java                 # Indicator code → TA4J
├── web/
│   ├── TradingApiController.java            # REST API
│   └── TradingHtmxController.java           # HTMX web controller
└── aot/hint/
    └── TraderAgentRuntimeHintsRegistrar.java # GraalVM hints

src/main/resources/
├── application.yaml                         # Main configuration
├── prompts/                                 # Jinja prompt templates
│   ├── analysts/                            # 4 analyst templates
│   ├── debate/                              # Distiller template
│   ├── managers/                            # 4 manager templates
│   ├── memory/                              # Reflection template
│   ├── researchers/                         # 2 researcher templates
│   └── risk/                                # 3 risk templates
├── static/css/                              # CSS files
└── templates/                               # Thymeleaf templates
```
