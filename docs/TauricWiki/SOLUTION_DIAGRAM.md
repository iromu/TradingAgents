# TradingAgents — Solution Diagram & Architecture Reference

> **Purpose**: This document defines the complete architecture of the TradingAgents system so that it can be reimplemented in another programming language or with different frameworks. It covers every component, their responsibilities, interfaces, data flows, and external dependencies.

> **⚠️ PARTIALLY OUTDATED**: This document was originally written for the Python/LangGraph implementation.
> The system has been refactored to **Java 25 / Spring Boot 3.5.13 / Embabel 0.5.0-SNAPSHOT**.
> See the **Current Java Implementation** section (Section 17) below for the actual codebase architecture.
> The sections above (1–16) describe the original Python architecture and serve as a conceptual reference.

---

## 1. System Overview

TradingAgents is a **multi-agent LLM-powered financial trading analysis framework**. It decomposes the investment research process into specialized roles organized into **5 teams** and **12 agents**, orchestrated via a directed graph with conditional debate loops.

### 1.1 Core Principles

| Principle | Description |
|-----------|-------------|
| **Role-based decomposition** | Each agent has a single responsibility (collect data, debate, decide) |
| **Debate-driven reasoning** | Conflicting viewpoints (bull/bear, aggressive/conservative) are resolved through structured dialogue |
| **Vendor abstraction** | Data sources are interchangeable via a routing layer |
| **Provider abstraction** | LLM providers are interchangeable via a client factory |
| **Structured output** | Decision agents use Pydantic schemas for consistent, machine-parseable output |
| **Append-only memory** | Past decisions are logged and fed back as context for continuous improvement |
| **Checkpoint resume** | Graph execution can be paused and resumed from the last successful node |

### 1.2 High-Level Architecture

> *This diagram shows the original Python/LangGraph architecture. The current Java implementation uses Spring Boot + Embabel with a different UI layer (HTMX).*

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TradingAgents                               │
│                                                                     │
│  ┌──────────┐    ┌──────────────┐    ┌──────────────────────────┐  │
│  │   CLI    │    │   Python API │    │    Configuration         │  │
│  │ (Typer + │    │ (Trading-    │    │ (DEFAULT_CONFIG +        │  │
│  │  Rich)   │    │   Agents-    │    │  env-var overrides)      │  │
│  └──────────┘    └──────┬───────┘    └──────────────────────────┘  │
│                         │                                           │
│              ┌──────────▼──────────┐                               │
│              │  TradingAgentsGraph  │                               │
│              │  (LangGraph State-   │                               │
│              │   Graph orchestrator)│                               │
│              └──────────┬──────────┘                               │
│                         │                                           │
│  ┌──────────────────────▼──────────────────────┐                   │
│  │              Agent Teams                     │                   │
│  │  ┌─────────┐ ┌─────────┐ ┌──────┐ ┌──────┐  │                   │
│  │  │ Analyst │ │Research │ │Trader│ │ Risk │  │                   │
│  │  │  Team   │ │  Team   │ │      │ │Mgmt  │  │                   │
│  │  └─────────┘ └─────────┘ └──────┘ └──────┘  │                   │
│  │                          ┌─────────┐         │                   │
│  │                          │  Portfolio│         │                   │
│  │                          │  Manager │         │                   │
│  │                          └─────────┘         │                   │
│  └──────────────────────┬──────────────────────┘                   │
│                         │                                           │
│  ┌──────────────────────▼──────────────────────┐                   │
│  │         Tools & Data Providers               │                   │
│  │  ┌──────────┐ ┌──────────┐ ┌────────────┐  │                   │
│  │  │  Stock   │ │  News    │ │  Macro /   │  │                   │
│  │  │  Data    │ │  Data    │ │  Prediction│  │                   │
│  │  │  & Tech  │ │  &       │ │  Markets   │  │                   │
│  │  │Indicators│ │Insider   │ │            │  │                   │
│  │  └──────────┘ └──────────┘ └────────────┘  │                   │
│  └──────────────────────┬──────────────────────┘                   │
│                         │                                           │
│  ┌──────────────────────▼──────────────────────┐                   │
│  │          LLM Provider Layer                  │                   │
│  │  OpenAI · Anthropic · Google · xAI ·        │                   │
│  │  DeepSeek · Qwen · GLM · MiniMax ·          │                   │
│  │  OpenRouter · Azure · Bedrock · Ollama ·    │                   │
│  │  OpenAI-Compatible (vLLM, LM Studio, ...)   │                   │
│  └─────────────────────────────────────────────┘                   │
│                                                                     │
│  ┌──────────────────────────────────────────────────┐               │
│  │         Persistence & Memory                     │               │
│  │  • Decision Log (append-only markdown)           │               │
│  │  • Checkpoint Resume (SQLite via LangGraph)      │               │
│  │  • State Logging (JSON per run)                  │               │
│  └──────────────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Inventory

### 2.1 Entry Points

| Component | Type | Description |
|-----------|------|-------------|
| `main.py` | Script | Example programmatic usage |
| `cli/main.py` | CLI App | Interactive Typer CLI with Rich live dashboard (1339 lines) |
| `TradingAgentsGraph` | Class | Python API entry point; `.propagate(ticker, date)` returns (state, rating) |

> *Note: The current Java implementation uses `TradingHtmxController` (Spring MVC) with Thymeleaf + HTMX instead of the Typer CLI.*

---

## 2.2 Agent Teams (12 Agents, 5 Teams)

#### Team 1: Analyst Team — Data Collection

| Agent | Factory Function | LLM Type | Tools | Output Field |
|-------|-----------------|----------|-------|-------------|
| Market Analyst | `create_market_analyst(llm)` | `quick_think_llm` | `get_stock_data`, `get_indicators`, `get_verified_market_snapshot` | `market_report` |
| Sentiment Analyst | `create_sentiment_analyst(llm)` | `quick_think_llm` | Pre-fetched data only (no tool calls) | `sentiment_report` |
| News Analyst | `create_news_analyst(llm)` | `quick_think_llm` | `get_news`, `get_global_news`, `get_insider_transactions`, `get_macro_indicators`, `get_prediction_markets` | `news_report` |
| Fundamentals Analyst | `create_fundamentals_analyst(llm)` | `quick_think_llm` | `get_fundamentals`, `get_balance_sheet`, `get_cashflow`, `get_income_statement` | `fundamentals_report` |

#### Team 2: Research Team — Debate

| Agent | Factory Function | LLM Type | Role |
|-------|-----------------|----------|------|
| Bull Researcher | `create_bull_researcher(llm)` | `quick_think_llm` | Argues for the investment position |
| Bear Researcher | `create_bear_researcher(llm)` | `quick_think_llm` | Argues against the investment position |
| Research Manager | `create_research_manager(llm)` | `deep_think_llm` | Synthesizes debate into structured `ResearchPlan` |

#### Team 3: Trading Team

| Agent | Factory Function | LLM Type | Output |
|-------|-----------------|----------|--------|
| Trader | `create_trader(llm)` | `quick_think_llm` | Structured `TraderProposal` (Buy/Hold/Sell + entry, stop-loss, sizing) |

#### Team 4: Risk Management — Risk Debate

| Agent | Factory Function | LLM Type | Perspective |
|-------|-----------------|----------|-------------|
| Aggressive Debator | `create_aggressive_debator(llm)` | `quick_think_llm` | High-risk, high-reward |
| Conservative Debator | `create_conservative_debator(llm)` | `quick_think_llm` | Capital preservation |
| Neutral Debator | `create_neutral_debator(llm)` | `quick_think_llm` | Balanced perspective |

#### Team 5: Portfolio Management

| Agent | Factory Function | LLM Type | Output |
|-------|-----------------|----------|--------|
| Portfolio Manager | `create_portfolio_manager(llm)` | `deep_think_llm` | Structured `PortfolioDecision` (5-tier rating + thesis) |

### 2.3 Tools (11 Tool Functions)

Tools are LangChain `@tool`-decorated functions. Each wraps `route_to_vendor()` which dispatches to the configured data provider.

> *Note: In the Java implementation, tools are Spring `@Service` classes (`YFinService`, `AlphaVantageService`, etc.) called directly from agent code — no LangChain tool decorators.*

| Tool | Category | Vendors | Description |
|------|----------|---------|-------------|
| `get_stock_data` | core_stock_apis | yfinance, Alpha Vantage | OHLCV price data |
| `get_indicators` | technical_indicators | yfinance, Alpha Vantage | Technical indicators (RSI, MACD, Bollinger, ATR, VWMA, MFI, SMA, EMA) |
| `get_fundamentals` | fundamental_data | yfinance, Alpha Vantage | Company financial overview |
| `get_balance_sheet` | fundamental_data | yfinance, Alpha Vantage | Balance sheet data |
| `get_cashflow` | fundamental_data | yfinance, Alpha Vantage | Cash flow statement |
| `get_income_statement` | fundamental_data | yfinance, Alpha Vantage | Income statement |
| `get_news` | news_data | yfinance, Alpha Vantage | Ticker-specific news |
| `get_global_news` | news_data | yfinance, Alpha Vantage | Global/macro news |
| `get_insider_transactions` | news_data | yfinance, Alpha Vantage | Insider trading activity |
| `get_macro_indicators` | macro_data | FRED | Macroeconomic data (CPI, unemployment, rates, GDP, VIX) |
| `get_prediction_markets` | prediction_markets | Polymarket | Market-implied event probabilities |
| `get_verified_market_snapshot` | (Market Analyst only) | yfinance + stockstats | Deterministic OHLCV + indicator snapshot for grounding |

### 2.4 Data Providers

| Provider | Type | API Key Required | Data Served |
|----------|------|-----------------|-------------|
| yfinance | Default vendor | No | Stock data, fundamentals, news, insider transactions, technical indicators |
| Alpha Vantage | Alternative vendor | Yes (`ALPHA_VANTAGE_API_KEY`) | Stock data, fundamentals, news, insider transactions, technical indicators |
| FRED | Macroeconomic | Yes (`FRED_API_KEY`) | Macro indicators (CPI, unemployment, rates, GDP, VIX) |
| Polymarket | Prediction markets | No | Prediction market probabilities (keyless public API) |
| StockTwits | Retail sentiment | No | Retail sentiment messages (keyless direct HTTP) |
| Reddit | Finance sentiment | No | Finance subreddit posts (keyless RSS/Atom feeds) |

### 2.5 LLM Providers (16 Total)

| Provider | `llm_provider` value | API Key Env Var | Client Class |
|----------|---------------------|-----------------|-------------|
| OpenAI | `openai` | `OPENAI_API_KEY` | `OpenAIClient` (Responses API) |
| Anthropic | `anthropic` | `ANTHROPIC_API_KEY` | `AnthropicClient` |
| Google Gemini | `google` | `GOOGLE_API_KEY` | `GoogleClient` |
| xAI (Grok) | `xai` | `XAI_API_KEY` | `OpenAIClient` (OpenAI-compatible) |
| DeepSeek | `deepseek` | `DEEPSEEK_API_KEY` | `OpenAIClient` (with DeepSeekChatOpenAI) |
| Qwen (Intl) | `qwen` | `DASHSCOPE_API_KEY` | `OpenAIClient` |
| Qwen (China) | `qwen-cn` | `DASHSCOPE_CN_API_KEY` | `OpenAIClient` |
| GLM (Intl) | `glm` | `ZHIPU_API_KEY` | `OpenAIClient` |
| GLM (China) | `glm-cn` | `ZHIPU_CN_API_KEY` | `OpenAIClient` |
| MiniMax (Global) | `minimax` | `MINIMAX_API_KEY` | `OpenAIClient` (with MinimaxChatOpenAI) |
| MiniMax (China) | `minimax-cn` | `MINIMAX_CN_API_KEY` | `OpenAIClient` |
| OpenRouter | `openrouter` | `OPENROUTER_API_KEY` | `OpenAIClient` |
| Azure OpenAI | `azure` | `AZURE_OPENAI_API_KEY` | `AzureOpenAIClient` |
| AWS Bedrock | `bedrock` | AWS credential chain | `BedrockClient` (optional extra) |
| Ollama (local) | `ollama` | none | `OpenAIClient` (keyless) |
| OpenAI-compatible | `openai_compatible` | `OPENAI_COMPATIBLE_API_KEY` (opt) | `OpenAIClient` |

> *Note: In Java, LLM providers are managed by Embabel's `ModelProvider.BEST_ROLE` / `CHEAPEST_ROLE` abstraction configured via `application.yaml` — no direct client classes.*

### 2.6 Pydantic Schemas (Structured Output)

| Schema | Produced By | Fields |
|--------|------------|--------|
| `ResearchPlan` | Research Manager | `recommendation` (5-tier), `rationale` (str), `strategic_actions` (str) |
| `TraderProposal` | Trader | `action` (Buy/Hold/Sell), `reasoning` (str), `entry_price` (float?), `stop_loss` (float?), `position_sizing` (str?) |
| `PortfolioDecision` | Portfolio Manager | `rating` (5-tier), `executive_summary` (str), `investment_thesis` (str), `price_target` (float?), `time_horizon` (str?) |
| `SentimentReport` | Sentiment Analyst | `overall_band` (6-tier), `overall_score` (0-10), `confidence` (low/med/high), `narrative` (str) |

**Rating enums:**
- `PortfolioRating`: BUY, OVERWEIGHT, HOLD, UNDERWEIGHT, SELL
- `TraderAction`: BUY, HOLD, SELL
- `SentimentBand`: BULLISH, MILDLY_BULLISH, NEUTRAL, MIXED, MILDLY_BEARISH, BEARISH

### 2.7 State Schema (LangGraph `AgentState`)

> *Note: In Java, state is managed via records in `ResearchTypes.java` — no `AgentState` or `MessagesState`.*

```
AgentState (extends MessagesState):
├── company_of_interest: str          # Ticker symbol
├── asset_type: str                   # "stock" or "crypto"
├── instrument_context: str           # Deterministic company identity
├── trade_date: str                   # Analysis date (YYYY-MM-DD)
├── sender: str                       # Agent that sent current message
├── market_report: str                # Market Analyst output
├── sentiment_report: str             # Sentiment Analyst output
├── news_report: str                  # News Analyst output
├── fundamentals_report: str          # Fundamentals Analyst output
├── investment_debate_state: InvestDebateState
│   ├── bull_history: str
│   ├── bear_history: str
│   ├── history: str
│   ├── current_response: str
│   ├── judge_decision: str
│   └── count: int
├── investment_plan: str              # Research Manager output
├── trader_investment_plan: str       # Trader output
├── risk_debate_state: RiskDebateState
│   ├── aggressive_history: str
│   ├── conservative_history: str
│   ├── neutral_history: str
│   ├── history: str
│   ├── latest_speaker: str
│   ├── current_aggressive_response: str
│   ├── current_conservative_response: str
│   ├── current_neutral_response: str
│   ├── judge_decision: str
│   └── count: int
├── final_trade_decision: str         # Portfolio Manager output
└── past_context: str                 # Memory log context injection
```

---

## 3. Execution Pipeline (5 Phases)

```
Phase 0: Preparation
─────────────────────────────────────────────────────────────
Input: ticker + trade_date
  │
  ├─> _resolve_pending_entries()     # Fetch returns for past pending decisions
  │     └─> _fetch_returns()         # yfinance: raw return + alpha vs benchmark
  │     └─> reflect_on_final_decision()  # LLM reflection on past predictions
  │     └─> batch_update_with_outcomes()  # Atomic update of memory log
  │
  ├─> resolve_instrument_context()   # yfinance lookup → company name, sector
  │
  ├─> create_initial_state()         # Build AgentState with all empty fields
  │     └─> past_context injection   # Memory log: past same-ticker + cross-ticker
  │
  └─> Compile graph (with checkpointer if enabled)


Phase 1: Analyst Data Collection (Sequential or Concurrent)
─────────────────────────────────────────────────────────────
  │
  ├─> Market Analyst ──tool calls──> get_stock_data, get_indicators,
  │         get_verified_market_snapshot
  │         └─> writes: market_report
  │
  ├─> Sentiment Analyst ──pre-fetched─> news + StockTwits + Reddit data
  │         └─> writes: sentiment_report (structured SentimentReport)
  │
  ├─> News Analyst ──tool calls──> get_news, get_global_news,
  │         get_insider_transactions, get_macro_indicators,
  │         get_prediction_markets
  │         └─> writes: news_report
  │
  └─> Fundamentals Analyst ──tool calls──> get_fundamentals,
         get_balance_sheet, get_cashflow, get_income_statement
         └─> writes: fundamentals_report


Phase 2: Bull/Bear Debate (Loop)
─────────────────────────────────────────────────────────────
  │
  ├─> Bull Researcher ──reads──> all 4 reports + debate history
  │         └─> writes: bull_history
  │
  ├─> Bear Researcher ──reads──> all 4 reports + debate history
  │         └─> writes: bear_history
  │
  ├─> (alternate) until count >= 2 * max_debate_rounds
  │
  └─> Research Manager ──reads──> debate history
         └─> writes: investment_plan (structured ResearchPlan)


Phase 3: Trader Decision
─────────────────────────────────────────────────────────────
  │
  └─> Trader ──reads──> ResearchPlan + all 4 reports
         └─> writes: trader_investment_plan (structured TraderProposal)


Phase 4: Risk Debate (Loop)
─────────────────────────────────────────────────────────────
  │
  ├─> Aggressive Debator ──reads──> trader proposal + all reports
  │         └─> writes: aggressive_history
  │
  ├─> Conservative Debator ──reads──> trader proposal + all reports
  │         └─> writes: conservative_history
  │
  ├─> Neutral Debator ──reads──> trader proposal + all reports
  │         └─> writes: neutral_history
  │
  └─> (cycle through all 3) until count >= 3 * max_risk_discuss_rounds


Phase 5: Portfolio Manager Decision
─────────────────────────────────────────────────────────────
  │
  └─> Portfolio Manager ──reads──> risk debate + trader plan + past_context
         └─> writes: final_trade_decision (structured PortfolioDecision)


Post-Processing
─────────────────────────────────────────────────────────────
  │
  ├─> _log_state() → JSON file        # Full state dump per run
  ├─> memory_log.store_decision()     # Append pending entry to markdown log
  └─> clear checkpoint (if enabled)
```

---

## 4. Graph Topology (LangGraph StateGraph)

```
LangGraph StateGraph(AgentState)

Nodes (dynamic + static):
─────────────────────────────
Dynamic (per selected analyst):
  - {analyst}_Agent        → LLM node (agent with prompt + tools)
  - {analyst}_Tools        → ToolNode (LangChain tool execution)
  - {analyst}_Clear        → Message delete node

Static:
  - Bull Researcher        → LLM node
  - Bear Researcher        → LLM node
  - Research Manager       → LLM node (structured output)
  - Trader                 → LLM node (structured output)
  - Aggressive Analyst     → LLM node
  - Conservative Analyst   → LLM node
  - Neutral Analyst        → LLM node
  - Portfolio Manager      → LLM node (structured output)

Edges:
─────────────────────────────
START → first_analyst_agent

For each analyst (sequential):
  analyst_agent ──conditional──> [analyst_tools, analyst_clear]
  analyst_tools → analyst_agent  (loop back for more tool calls)
  analyst_clear → next_analyst_agent (or Bull Researcher if last)

Debate loop:
  Bull Researcher ──conditional──> [Bear Researcher, Research Manager]
  Bear Researcher ──conditional──> [Bull Researcher, Research Manager]

Sequential chain:
  Research Manager → Trader → Aggressive Analyst

Risk debate loop:
  Aggressive Analyst ──conditional──> [Conservative Analyst, Portfolio Manager]
  Conservative Analyst ──conditional──> [Neutral Analyst, Portfolio Manager]
  Neutral Analyst ──conditional──> [Aggressive Analyst, Portfolio Manager]

Terminal:
  Portfolio Manager → END
```

---

## 5. Data Vendor Routing Layer

```
Tool (e.g., get_stock_data)
    │
    ▼
route_to_vendor(method, *args, **kwargs)
    │
    ├─> get_category_for_method(method)
    │     └─> "core_stock_apis" / "technical_indicators" / "fundamental_data" /
    │         "news_data" / "macro_data" / "prediction_markets"
    │
    ├─> get_vendor(category, method)
    │     ├─> Check tool_vendors config (takes precedence)
    │     └─> Fall back to data_vendors[category]
    │
    ├─> Build vendor_chain from config
    │     ├─> explicit list: ["yfinance", "alpha_vantage"]
    │     └─> "default" → all available vendors
    │
    └─> Iterate vendor_chain:
          ├─> VendorRateLimitError → skip to next vendor
          ├─> VendorNotConfiguredError → skip (log warning)
          ├─> NoMarketDataError → remember, try next vendor
          ├─> Other Exception → skip (log warning)
          └─> Success → return result
          
          If all vendors return NoMarketDataError:
              → Return NO_DATA_AVAILABLE sentinel string
          If all vendors throw real errors:
              → Raise first error
```

**Vendor Method Registry:**

```
VENDOR_METHODS = {
    "get_stock_data":         {"alpha_vantage": fn, "yfinance": fn},
    "get_indicators":         {"alpha_vantage": fn, "yfinance": fn},
    "get_fundamentals":       {"alpha_vantage": fn, "yfinance": fn},
    "get_balance_sheet":      {"alpha_vantage": fn, "yfinance": fn},
    "get_cashflow":           {"alpha_vantage": fn, "yfinance": fn},
    "get_income_statement":   {"alpha_vantage": fn, "yfinance": fn},
    "get_news":               {"alpha_vantage": fn, "yfinance": fn},
    "get_global_news":        {"yfinance": fn, "alpha_vantage": fn},
    "get_insider_transactions":{"alpha_vantage": fn, "yfinance": fn},
    "get_macro_indicators":   {"fred": fn},
    "get_prediction_markets": {"polymarket": fn},
}
```

---

## 6. LLM Client Layer

```
create_llm_client(provider, model, base_url=None, **kwargs)
    │
    ├─> Provider: "openai"        → OpenAIClient
    ├─> Provider: "anthropic"     → AnthropicClient
    ├─> Provider: "google"        → GoogleClient
    ├─> Provider: "azure"         → AzureOpenAIClient
    ├─> Provider: "bedrock"       → BedrockClient
    ├─> Provider: "ollama"        → OpenAIClient (keyless)
    ├─> Provider: "openai_compatible" → OpenAIClient
    └─> Provider: "xai","deepseek","qwen","qwen-cn",
        "glm","glm-cn","minimax","minimax-cn","openrouter"
        → OpenAIClient (with provider-specific chat class)
    │
    ├─> Provider-specific kwargs:
    │     ├─> "google"    → thinking_level ("high"/"minimal"/"low"/"medium")
    │     ├─> "openai"    → reasoning_effort ("high"/"medium"/"low")
    │     ├─> "anthropic" → effort ("high"/"medium"/"low")
    │     └─> cross-provider → temperature (float)
    │
    ├─> get_llm() → Returns langchain LLM instance
    │
    └─> Capability detection (per-model):
          ├─> tool_choice support (DeepSeek V4/reasoner: NO)
          ├─> structured output method selection
          └─> reasoning_split (MiniMax M2.x reasoning: YES)
```

**LLM Assignment:**

| LLM Variable | Default Model | Used By |
|-------------|--------------|---------|
| `deep_think_llm` | GPT-5.5 | Research Manager, Portfolio Manager |
| `quick_think_llm` | GPT-5.4-mini | All analysts, researchers, trader, risk debators |

---

## 7. Configuration System

```
DEFAULT_CONFIG (single source of truth)
    │
    └─> _ENV_OVERRIDES: TRADINGAGENTS_* env vars override at runtime
         ├─> TRADINGAGENTS_LLM_PROVIDER → llm_provider
         ├─> TRADINGAGENTS_DEEP_THINK_LLM → deep_think_llm
         ├─> TRADINGAGENTS_QUICK_THINK_LLM → quick_think_llm
         ├─> TRADINGAGENTS_LLM_BACKEND_URL → backend_url
         ├─> TRADINGAGENTS_OUTPUT_LANGUAGE → output_language
         ├─> TRADINGAGENTS_MAX_DEBATE_ROUNDS → max_debate_rounds
         ├─> TRADINGAGENTS_MAX_RISK_ROUNDS → max_risk_discuss_rounds
         ├─> TRADINGAGENTS_CHECKPOINT_ENABLED → checkpoint_enabled
         ├─> TRADINGAGENTS_BENCHMARK_TICKER → benchmark_ticker
         └─> TRADINGAGENTS_TEMPERATURE → temperature
```

**Key Configuration Keys:**

| Key | Default | Description |
|-----|---------|-------------|
| `llm_provider` | `"openai"` | LLM provider name |
| `deep_think_llm` | `"gpt-5.5"` | Model for complex reasoning |
| `quick_think_llm` | `"gpt-5.4-mini"` | Model for quick tasks |
| `backend_url` | `None` | Custom API endpoint |
| `temperature` | `None` | Sampling temperature |
| `google_thinking_level` | `None` | Gemini thinking level |
| `openai_reasoning_effort` | `None` | OpenAI reasoning effort |
| `anthropic_effort` | `None` | Anthropic effort level |
| `checkpoint_enabled` | `False` | Enable checkpoint resume |
| `output_language` | `"English"` | Output language for reports |
| `max_debate_rounds` | `1` | Max bull/bear debate rounds |
| `max_risk_discuss_rounds` | `1` | Max risk debate rounds |
| `analyst_concurrency_limit` | `1` | Analyst parallelism |
| `news_article_limit` | `20` | Max articles per ticker |
| `global_news_article_limit` | `10` | Max global news articles |
| `global_news_lookback_days` | `7` | Macro news lookback window |
| `data_vendors` | dict | Category → vendor mapping |
| `tool_vendors` | dict | Tool-level vendor overrides |
| `benchmark_ticker` | `None` | Override benchmark |
| `benchmark_map` | dict | Suffix → benchmark mapping |
| `results_dir` | `~/.tradingagents/logs` | Results output directory |
| `data_cache_dir` | `~/.tradingagents/cache` | Cache directory |
| `memory_log_path` | `~/.tradingagents/memory/trading_memory.md` | Decision log path |
| `memory_log_max_entries` | `None` | Max resolved entries (rotation) |

---

## 8. Memory & Persistence

### 8.1 Decision Log (Append-Only Markdown)

```
File: ~/.tradingagents/memory/trading_memory.md

Format per entry:
─────────────────────────────────────────────────────────────
[2026-01-15 | NVDA | Buy | +5.2% | +2.1% | 5d]

DECISION:
[Full portfolio decision text]

REFLECTION:
[LLM-generated reflection on prediction accuracy]

<!-- ENTRY_END -->
```

**Two-phase lifecycle:**
1. **Phase A (pending)**: `store_decision()` appends with `| pending]` tag — no LLM call
2. **Phase B (resolved)**: On next run for same ticker, `_fetch_returns()` computes returns, `reflect_on_final_decision()` generates reflection, `batch_update_with_outcomes()` atomically updates the log

**Memory context injection** (`get_past_context`):
- Last 5 same-ticker resolved decisions (with returns + reflections)
- Last 3 cross-ticker lessons (reflections only)
- Injected into Portfolio Manager prompt at run start

### 8.2 Checkpoint Resume (SQLite)

```
Directory: ~/.tradingagents/cache/checkpoints/<TICKER>.db

Enable: --checkpoint flag or config["checkpoint_enabled"] = True
Clear: --clear-checkpoints flag or config reset

Behavior:
  ├─> LangGraph SqliteSaver saves state after each node
  ├─> On resume: reads checkpoint_step() → resumes from last node
  ├─> On completion: checkpoint cleared automatically
  └─> Thread ID: <TICKER>:<date> (same ticker+date resumes, different date fresh)
```

### 8.3 State Logging (JSON)

```
Path: ~/.tradingagents/logs/<TICKER>/TradingAgentsStrategy_logs/
      full_states_log_<date>.json

Content: Full AgentState snapshot per run (all reports, debate histories, decisions)
```

---

## 9. CLI Interface

```
tradingagents                          # Launch interactive CLI
python -m cli.main                       # Alternative

Interactive prompts:
─────────────────────────────────────────────────────────────
1. Select ticker(s)                    # Manual input or default
2. Select analysis date                # YYYY-MM-DD format
3. Select LLM provider                 # OpenAI, Anthropic, Google, etc.
4. Select deep thinking model          # Model selection from catalog
5. Select quick thinking model         # Model selection from catalog
6. Select research depth               # Controls debate rounds
7. Select analysts                     # Market, Sentiment, News, Fundamentals
8. Provider-specific config            # Thinking level, reasoning effort, etc.
9. Confirm and run                     # Live Rich dashboard

Dashboard panels:
─────────────────────────────────────────────────────────────
┌─────────────────┬──────────────────────────────────────┐
│ Agent Status    │ Tool Calls (live)                    │
│ (spinning)      │                                      │
├─────────────────┼──────────────────────────────────────┤
│ Reports         │ Final Decision                     │
│ (as they load)  │                                      │
├─────────────────┴──────────────────────────────────────┤
│ Token Usage Stats (LLM calls, tool calls, tokens)      │
└────────────────────────────────────────────────────────┘
```

---

## 10. Dependencies

> *Note: The Python dependencies listed in the original version have been replaced by Java/Spring Boot dependencies. See Section 16.7 for the current Java file structure.*

**Java core dependencies:** `embabel-agent-starter`, `embabel-agent-starter-openai-custom`, `spring-boot-starter-web`, `yahoofinance` (v3.17.0), `ta4j-core` (v0.18), `spring-boot-starter-thymeleaf`

**Optional:** `embabel-agent-starter-anthropic` (provided scope), `langgraph-checkpoint-sqlite` (not used — Java uses file-based checkpoints)

---

## 11. Reimplementation Guide

To reimplement this system in another language or framework, follow this structure:

### 11.1 Required Capabilities

1. **Graph-based workflow orchestration** — A directed graph with conditional edges and state passing between nodes
2. **LLM integration** — Support for multiple LLM providers with:
   - Chat completions API
   - Tool/function calling
   - Structured output (JSON schema)
   - Provider-specific features (thinking, reasoning)
3. **Tool execution** — A way for agents to call external functions (data fetching)
4. **Data provider abstraction** — A routing layer that dispatches data requests to interchangeable providers
5. **State management** — Persistent state across graph nodes, with checkpoint/resume capability
6. **Markdown file I/O** — Append-only log file with atomic updates

### 11.2 Component Mapping

| TradingAgents Component | Equivalent in Other Frameworks |
|------------------------|-------------------------------|
| LangGraph StateGraph | Durable AI Studio, AutoGen, CrewAI, LangChain Agents, custom FSM |
| @tool-decorated functions | LangChain Tools, LlamaIndex Tools, custom function wrappers |
| Pydantic schemas | JSON Schema, Zod, TypeScript interfaces, Protobuf |
| create_llm_client() factory | Any LLM SDK wrapper (LiteLLM, Instructor, OpenAI SDK) |
| route_to_vendor() | Any provider abstraction pattern (Strategy pattern) |
| TradingMemoryLog | Any append-only log (event sourcing pattern) |
| Rich live dashboard | ncurses, TUI framework, or web dashboard |

### 11.3 Agent Prompt Structure

Each agent follows this pattern:
1. **System prompt**: Role definition + instructions + constraints
2. **Context injection**: Analyst reports, debate history, memory log context
3. **Tool definitions** (for analysts): Available tools with descriptions
4. **Structured output** (for decision agents): Pydantic schema with field descriptions
5. **Rating scale guidance**: Explicit scale definitions for consistent ratings

### 11.4 Data Flow Requirements

1. **Ticker resolution**: Convert ticker symbol to canonical form (handle exchange suffixes, crypto symbols)
2. **Data fetching**: OHLCV, fundamentals, news, sentiment, macro indicators, prediction markets
3. **Indicator computation**: Technical indicators from OHLCV data (SMA, EMA, RSI, MACD, Bollinger, ATR, VWMA, MFI)
4. **State accumulation**: Each node's output becomes context for subsequent nodes
5. **Debate loop control**: Count-based termination (rounds * 2 for bull/bear, rounds * 3 for risk)
6. **Return calculation**: Post-trade return vs benchmark (alpha) computation
7. **Reflection generation**: LLM-generated analysis of past prediction accuracy

### 11.5 Configuration Requirements

- Single source of truth config dict
- Environment variable overrides with type coercion
- Provider-specific settings (thinking level, reasoning effort, temperature)
- Vendor selection per data category
- Benchmark mapping per market suffix
- Path configuration for results, cache, and memory

---

## 12. Supported Markets

| Market | Suffix | Example | Benchmark |
|--------|--------|---------|-----------|
| US | (none) | `AAPL`, `SPY` | SPY |
| Hong Kong | `.HK` | `0700.HK` | ^HSI |
| Japan | `.T` | `7203.T` | ^N225 |
| London | `.L` | `AZN.L` | ^FTSE |
| India (NSE) | `.NS` | `RELIANCE.NS` | ^NSEI |
| India (BSE) | `.BO` | `RELIANCE.BO` | ^BSESN |
| Canada | `.TO` | — | ^GSPTSE |
| Australia | `.AX` | — | ^AXJO |
| China A (Shanghai) | `.SS` | `600519.SS` | 000001.SS |
| China A (Shenzhen) | `.SZ` | `600519.SZ` | 399001.SZ |
| Crypto | `-USD` | `BTC-USD`, `ETH-USD` | SPY |

---

## 13. Technical Indicators

| Indicator | Description |
|-----------|-------------|
| `close_50_sma` | 50-day Simple Moving Average |
| `close_200_sma` | 200-day Simple Moving Average |
| `close_10_ema` | 10-day Exponential Moving Average |
| `macd` | Moving Average Convergence Divergence |
| `macds` | MACD Signal Line |
| `macdh` | MACD Histogram |
| `rsi` | Relative Strength Index |
| `boll` | Bollinger Band Middle |
| `boll_ub` | Bollinger Band Upper |
| `boll_lb` | Bollinger Band Lower |
| `atr` | Average True Range |
| `vwma` | Volume Weighted Moving Average |
| `mfi` | Money Flow Index |

---

## 14. API Reference

### 14.1 Programmatic API

```python
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG

# Minimal usage
ta = TradingAgentsGraph(debug=True)
_, decision = ta.propagate("NVDA", "2024-05-10")
print(decision)  # e.g., "Buy"

# Full configuration
config = DEFAULT_CONFIG.copy()
config["llm_provider"] = "anthropic"
config["deep_think_llm"] = "claude-opus-4"
config["quick_think_llm"] = "claude-sonnet-4"
config["max_debate_rounds"] = 3
config["temperature"] = 0.0
config["checkpoint_enabled"] = True
config["data_vendors"] = {
    "core_stock_apis": "yfinance",
    "technical_indicators": "yfinance",
    "fundamental_data": "yfinance",
    "news_data": "yfinance",
    "macro_data": "fred",
    "prediction_markets": "polymarket",
}

ta = TradingAgentsGraph(
    selected_analysts=("market", "social", "news", "fundamentals"),
    debug=True,
    config=config,
)
_, decision = ta.propagate("AAPL", "2026-01-15")
print(decision)
```

### 14.2 CLI Commands

```bash
# Launch interactive CLI
tradingagents

# With checkpoint enabled
tradingagents analyze --checkpoint

# Clear all checkpoints
tradingagents analyze --clear-checkpoints
```

---

## 15. Directory Structure (Reimplementation Checklist)

For a clean-room reimplementation, create the following module structure:

```
<project>/
├── config/                    # Configuration management
│   └── default_config.py      # DEFAULT_CONFIG + env-var overrides
│
├── agents/                    # Agent implementations
│   ├── schemas.py             # Pydantic/JSON schemas
│   ├── analysts/              # 4 analyst agents
│   ├── researchers/           # 2 researcher agents
│   ├── managers/              # 2 manager agents
│   ├── trader/                # 1 trader agent
│   ├── risk_mgmt/             # 3 risk debator agents
│   └── utils/                 # Shared utilities (memory, rating, structured output)
│
├── data/                      # Data provider abstraction
│   ├── interface.py           # route_to_vendor()
│   ├── providers/             # Individual provider implementations
│   │   ├── yfinance.py
│   │   ├── alpha_vantage.py
│   │   ├── fred.py
│   │   ├── polymarket.py
│   │   ├── stocktwits.py
│   │   └── reddit.py
│   └── indicators.py          # Technical indicator computation
│
├── graph/                     # Workflow orchestration
│   ├── trading_graph.py       # Main orchestrator class
│   ├── setup.py               # Graph construction
│   ├── propagation.py         # State initialization
│   ├── conditional_logic.py   # Debate loop routing
│   ├── signal_processing.py   # Rating extraction
│   ├── reflection.py          # Decision reflection
│   └── checkpointer.py        # Checkpoint/resume
│
├── llm/                       # LLM provider abstraction
│   ├── factory.py             # Provider factory
│   ├── base.py                # Abstract base client
│   ├── openai.py              # OpenAI-compatible client
│   ├── anthropic.py           # Anthropic client
│   ├── google.py              # Google Gemini client
│   ├── azure.py               # Azure OpenAI client
│   ├── bedrock.py             # AWS Bedrock client
│   ├── capabilities.py        # Per-model capability detection
│   └── catalog.py             # Known model lists
│
├── cli/                       # Command-line interface
│   ├── main.py                # CLI entry point
│   ├── dashboard.py           # Live status dashboard
│   └── prompts.py             # Interactive prompts
│
├── tests/                     # Test suite
└── main.py                    # Example usage
```

---

*Document version: 1.0 | Based on TradingAgents v0.2.5 | Last updated: 2026-06-15*

## 16. Current Java Implementation (as of 2026-06-24)

The system has been refactored from Python/LangGraph to **Java 25 / Spring Boot 3.5.13 / Embabel 0.5.0-SNAPSHOT**.

### 16.1 Entry Points

| Component | Type | Description |
|-----------|------|-------------|
| `TradingHtmxController` | Spring MVC Controller | Web UI entry point — Thymeleaf + HTMX, SSE for live updates |
| `OrchestratorAgent` | Embabel Agent | Entry point agent — ticker input, HITL plan approval, orchestrates research |
| `mvn spring-boot:run` | CLI | Start the web application |

### 16.2 Agent Architecture (Embabel @Agent Pattern)

The system uses Embabel's annotation-based agent framework with `asSubProcess` for nested agent composition:

| Agent | File | Role |
|-------|------|------|
| `OrchestratorAgent` | `agent/OrchestratorAgent.java` | Entry point — resolves pending decisions, resolves instrument identity, generates research plan, HITL approval via `WaitFor.formSubmission()` |
| `DebateAgent` | `agent/DebateAgent.java` | Central orchestrator — generates all 4 analyst reports, orchestrates bull/bear debate, delegates risk debate, produces final plan |
| `DebateLoopAgent` | `agent/DebateLoopAgent.java` | Bull/bear iterative debate loop — uses `RepeatUntilBuilder` with Jaccard bigram similarity convergence detection |
| `RiskDebateAgent` | `agent/RiskDebateAgent.java` | 3-round risk debate (aggressive/conservative/neutral) with explicit round-robin and fallback parsing |
| `Trader` | `agent/Trader.java` | Generates structured `TraderProposalOutput` (Buy/Hold/Sell + entry, stop-loss, sizing) |
| `PortfolioManager` | `agent/managers/PortfolioManager.java` | Generates final `PortfolioDecisionOutput` (5-tier rating + thesis) |
| `InstrumentIdentityAgent` | `agent/identity/InstrumentIdentityAgent.java` | Resolves ticker to company name/sector/industry via Yahoo Finance |
| `CheckpointAgent` | `agent/checkpoint/CheckpointAgent.java` | Manages crash recovery via file-based checkpoint store |
| `DecisionMemoryAgent` | `agent/memory/DecisionMemoryAgent.java` | Manages append-only decision memory lifecycle |

### 16.3 Data Providers (Java Services)

| Service | File | Data |
|---------|------|------|
| `YFinService` | `dataflows/YFinService.java` | Stock data, fundamentals, indicators via `yahoofinance` Java library |
| `AlphaVantageService` | `dataflows/AlphaVantageService.java` | Stock data, fundamentals, news, insider transactions via REST API |
| `FredService` | `dataflows/FredService.java` | Macroeconomic indicators via FRED REST API |
| `PolymarketService` | `dataflows/PolymarketService.java` | Prediction market probabilities via REST API |
| `VendorRouter` | `dataflows/VendorRouter.java` | Routes data requests to Alpha Vantage (yfinance used directly by agents) |

**Note**: The Python version had a dual-vendor routing layer (yfinance + Alpha Vantage). The Java version routes through `VendorRouter` to Alpha Vantage only; yfinance is accessed directly via `YFinService` by agents that need it.

### 16.4 Configuration

Configuration uses Spring Boot's `@ConfigurationProperties`:

| Key | Default | Description |
|-----|---------|-------------|
| `app.llm-options.ticker-llm` | (from model provider) | LLM options for ticker input processing |
| `app.llm-options.writer-llm` | (from model provider) | LLM options for report generation |
| `app.llm-options.provider` | (from model provider) | LLM provider name |
| `app.llm-options.best-model` | (from model provider) | Model for complex reasoning |
| `app.llm-options.cheapest-model` | (from model provider) | Model for quick tasks |
| `app.llm-options.max-concurrency` | `1` | Analyst parallelism |
| `app.llm-options.similarity-threshold` | `0.8` | Jaccard similarity threshold for debate convergence |
| `app.llm-options.max-debate-iterations` | `5` | Max debate loop iterations |
| `app.llm-options.anthropic.effort` | (from model provider) | Anthropic effort level |
| `app.llm-options.google.thinking-level` | (from model provider) | Gemini thinking level |
| `app.llm-options.openai.reasoning-effort` | (from model provider) | OpenAI reasoning effort |
| `app.checkpoint.enabled` | `false` | Enable checkpoint resume |
| `app.memory.log-path` | `~/.tradingagents/memory/trading_memory.md` | Decision log path |

> *Note: LLM options are managed by Embabel's `LlmOptions` record with `ModelProvider.BEST_ROLE` / `CHEAPEST_ROLE` abstraction. Provider-specific settings are nested under `app.llm-options.<provider>.*`.*

### 16.5 State Management (Java Records)

State is managed via Java records in `domain/ResearchTypes.java`:

```
ResearchTypes.Ticker — {content, feedback}
ResearchTypes.DebateBriefs — {fundamentalsBrief, marketBrief, newsBrief, socialBrief}
ResearchTypes.InvestmentDebateState — {history, bullHistory, bearHistory, currentResponse, count, briefs, riskAssessment, latestSpeaker, currentAggressiveResponse, currentConservativeResponse, currentNeutralResponse, traderProposal}
ResearchTypes.InvestmentPlan — {judgeDecision, investmentDebateState}
ResearchTypes.ResearchPlan — {content}
ResearchTypes.PlanApproval — {feedback, approved}
ResearchTypes.InvestmentReviewFeedback — {feedback, approved}
```

**Structured output records** (used by decision agents with `createObject(T.class, ...)`):

| Record | Produced By | Fields |
|--------|------------|--------|
| `PortfolioDecisionOutput` | PortfolioManager | `rating` (5-tier), `executiveSummary`, `investmentThesis`, `priceTarget`, `timeHorizon` |
| `TraderProposalOutput` | Trader | `action` (Buy/Hold/Sell), `reasoning`, `entryPrice`, `stopLoss`, `positionSizing` |
| `ResearchPlanOutput` | (unused — dead code) | `recommendation`, `rationale`, `strategicActions` |

> *Note: `ResearchPlanOutput` exists but is never used — all research plan generation uses `String.class` and wraps in `ResearchTypes.InvestmentPlan`. This is intentional because the LLM produces narrative plans, not structured data.*

**Risk assessment:**
| Record | Produced By | Fields |
|--------|------------|--------|
| `RiskAssessment` | RiskDebateAgent | `level` (RiskLevel enum), `reasoning` |

**Key enums** (same as Python version):
- `PortfolioRating`: BUY, OVERWEIGHT, HOLD, UNDERWEIGHT, SELL
- `TraderAction`: BUY, HOLD, SELL
- `SentimentBand`: BULLISH, MILDLY_BULLISH, NEUTRAL, MIXED, MILDLY_BEARISH, BEARISH
- `RiskLevel`: LOW, MEDIUM, HIGH (used by RiskAssessment)

### 16.6 Persistence

| Mechanism | Implementation |
|-----------|---------------|
| Decision Log | Append-only markdown at `app.memory.log-path` — two-phase lifecycle (pending → resolved) |
| Checkpoint | File-based JSON at `data/checkpoints/<TICKER>.json` — `CheckpointStore` + `CheckpointAgent` |
| LLM Cache | Disk-based `FileCache` with SHA-256 hashed keys, per-key locking, atomic writes at `data/llm/cache/` |
| Web UI | Spring MVC + Thymeleaf + HTMX — processing page at `/plan/status/{processId}` with SSE updates |

### 16.7 File Structure

```
TradingAgents/
├── pom.xml                          # Maven build (Spring Boot 3.5.13, Embabel 0.5.0-SNAPSHOT)
├── src/main/java/com/embabel/gekko/
│   ├── agent/                       # Agent implementations
│   │   ├── OrchestratorAgent.java   # Entry point agent
│   │   ├── DebateAgent.java         # Central orchestrator
│   │   ├── DebateLoopAgent.java     # Bull/bear loop with convergence
│   │   ├── RiskDebateAgent.java     # 3-round risk debate
│   │   ├── Trader.java              # Trading proposal agent
│   │   ├── checkpoint/              # Checkpoint management
│   │   ├── identity/                # Instrument identity resolution
│   │   ├── managers/                # Research/Portfolio managers
│   │   ├── memory/                  # Decision memory
│   │   ├── researchers/             # Bull/Bear researchers
│   │   └── risk/                    # Risk debators
│   ├── config/                      # Spring configuration
│   ├── dataflows/                   # Data provider services
│   ├── domain/                      # Java records & enums
│   ├── htmx/                        # HTMX controllers
│   ├── util/                        # FileCache, etc.
│   └── web/                         # Web controllers
├── src/main/resources/
│   ├── application.yaml             # Spring Boot config
│   ├── prompts/                     # Jinja prompt templates
│   │   ├── analysts/                # Analyst agent prompts
│   │   ├── debate/                  # Debate prompts
│   │   ├── managers/                # Manager prompts
│   │   ├── memory/                  # Memory prompts
│   │   ├── researchers/             # Researcher prompts
│   │   └── risk/                    # Risk debate prompts
│   └── templates/                   # Thymeleaf HTML templates
└── data/                            # Runtime directories
    ├── llm/cache/                   # LLM response cache
    └── checkpoints/                 # Checkpoint files
```

### 16.8 Key Differences from Python Version

| Aspect | Python (documented in sections 1–16) | Java (current) |
|--------|--------------------------------------|----------------|
| Framework | LangGraph + LangChain | Embabel + Spring Boot |
| Agent definition | Factory functions (`create_*`) | `@Agent`, `@Action`, `@AchievesGoal` annotations |
| State management | LangGraph `AgentState` dict | Java records in `ResearchTypes.java` |
| Structured output | Pydantic models | Java records with validation |
| Graph orchestration | `StateGraph` with conditional edges | `asSubProcess` for nested agent composition |
| Loop control | LangGraph conditional edges | `RepeatUntilBuilder` (convergence) + explicit `for` loops |
| CLI | Typer + Rich live dashboard | Spring MVC + Thymeleaf + HTMX + SSE |
| Caching | Redis | `FileCache` — disk-based with SHA-256 keys |
| Checkpoint | SQLite via LangGraph | JSON file-based via `CheckpointStore` |
| Data providers | Python yfinance + Alpha Vantage | Java `yahoofinance` lib + Alpha Vantage REST |
| Prompt templates | Python f-strings | Jinja templates in `prompts/*.jinja` |
| Vendor routing | Dual-vendor (yfinance + Alpha Vantage) | Alpha Vantage via `VendorRouter`; yfinance direct via `YFinService` |
| HITL | Custom implementation | Native Embabel `WaitFor.formSubmission()` |
| Convergence | Count-based only | Jaccard bigram similarity + count-based |
| Missing features | — | StockTwits sentiment (not implemented), Reddit sentiment (not implemented) |

### 16.9 Known Issues (from code audit)

| Issue | Severity | Status |
|-------|----------|--------|
| Hardcoded Alpha Vantage API key in `application-local.yaml` | High | Gitignored, local only |
| Base64-encoded Langfuse credentials in `application-local.yaml` | High | Gitignored, local only |
| `createObject(String.class, ...)` wrapped in typed records — no validation | High | Open — intentional for free-text reports; structured output used where schema is known |
| Path traversal in `FileCache` — sanitization strips valid characters causing collisions | High | Open — aggressive sanitization for defense-in-depth; may cause theoretical collisions |
| Dead `InvestmentDebateFeedback` record | Medium | **Dead code** — zero references in codebase; `InvestmentReviewFeedback` is the active HITL record |
| No timeout on RestTemplate | Medium | **Fixed** — `AlphaVantageService` now has connect/read timeouts |
| Cache key bug: `getNews()` ignores date range params | Medium | **Fixed** — `AlphaVantageService.getNews()` now includes `time_from` and `time_to` |

### 16.10 API Key Environment Variables

| Variable | Purpose |
|----------|---------|
| `OPENAI_API_KEY` | OpenAI-compatible provider (LiteLLM) |
| `OPENAI_BASE_URL` | Custom API endpoint (default: http://spark.local:4000) |
| `OPENAI_MODEL` | Default model override |
| `FRED_API_KEY` | FRED macroeconomic data |
| `ALPHA_VANTAGE_API_KEY` | Alpha Vantage API |
| `LANGFUSE_SECRET_KEY` | Langfuse observability (base64-encoded) |
| `LANGFUSE_HOST` | Langfuse host |
