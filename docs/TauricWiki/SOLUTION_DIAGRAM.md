# TradingAgents — Solution Diagram & Architecture Reference

> **Purpose**: This document defines the complete architecture of the TradingAgents system so that it can be reimplemented in another programming language or with different frameworks. It covers every component, their responsibilities, interfaces, data flows, and external dependencies.

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

### 2.2 Agent Teams (12 Agents, 5 Teams)

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

## 10. File Structure Reference

```
TradingAgents/
├── main.py                          # Entry point example
├── pyproject.toml                   # Build config, dependencies, tooling
├── requirements.txt                 # Flat dependency list
├── docker-compose.yml               # Docker compose (tradingagents + ollama)
├── Dockerfile                       # Multi-stage build (Python 3.12 slim)
├── .env.example                     # API key templates
├── .env.enterprise.example          # Azure OpenAI templates
│
├── tradingagents/                   # Main package
│   ├── __init__.py
│   ├── default_config.py            # DEFAULT_CONFIG + env-var overrides
│   │
│   ├── agents/                      # Agent implementations
│   │   ├── __init__.py              # Re-exports all create_* functions
│   │   ├── schemas.py               # Pydantic schemas (ResearchPlan, etc.)
│   │   ├── analysts/
│   │   │   ├── market_analyst.py
│   │   │   ├── sentiment_analyst.py
│   │   │   ├── news_analyst.py
│   │   │   └── fundamentals_analyst.py
│   │   ├── researchers/
│   │   │   ├── bull_researcher.py
│   │   │   └── bear_researcher.py
│   │   ├── managers/
│   │   │   ├── research_manager.py
│   │   │   └── portfolio_manager.py
│   │   ├── trader/
│   │   │   └── trader.py
│   │   ├── risk_mgmt/
│   │   │   ├── aggressive_debator.py
│   │   │   ├── conservative_debator.py
│   │   │   └── neutral_debator.py
│   │   └── utils/
│   │       ├── agent_states.py      # AgentState, InvestDebateState, RiskDebateState
│   │       ├── agent_utils.py       # Tool imports, instrument context, message delete
│   │       ├── memory.py            # TradingMemoryLog (append-only decision log)
│   │       ├── rating.py            # 5-tier rating parser (heuristic)
│   │       ├── structured.py        # bind_structured / invoke_structured_or_freetext
│   │       ├── core_stock_tools.py  # get_stock_data tool
│   │       ├── technical_indicators_tools.py  # get_indicators tool
│   │       ├── fundamental_data_tools.py      # get_fundamentals, get_balance_sheet, etc.
│   │       ├── news_data_tools.py             # get_news, get_global_news, get_insider_transactions
│   │       ├── macro_data_tools.py            # get_macro_indicators tool (FRED)
│   │       ├── prediction_markets_tools.py    # get_prediction_markets tool (Polymarket)
│   │       └── market_data_validation_tools.py # get_verified_market_snapshot tool
│   │
│   ├── dataflows/                   # Data provider abstraction
│   │   ├── interface.py             # route_to_vendor() -- central dispatch
│   │   ├── config.py                # get_config(), set_config()
│   │   ├── errors.py                # VendorError, NoMarketDataError, etc.
│   │   ├── symbol_utils.py          # normalize_symbol() (broker -> Yahoo symbols)
│   │   ├── utils.py                 # safe_ticker_component(), save_output()
│   │   ├── stockstats_utils.py      # load_ohlcv(), yf_retry(), StockstatsUtils
│   │   ├── y_finance.py             # yfinance: stock data, fundamentals, indicators
│   │   ├── yfinance_news.py         # yfinance: ticker news, global news
│   │   ├── alpha_vantage.py         # Aggregator for Alpha Vantage sub-modules
│   │   ├── alpha_vantage_stock.py   # Alpha Vantage: OHLCV
│   │   ├── alpha_vantage_indicator.py # Alpha Vantage: technical indicators
│   │   ├── alpha_vantage_fundamentals.py # Alpha Vantage: financial statements
│   │   ├── alpha_vantage_news.py    # Alpha Vantage: news, insider transactions
│   │   ├── alpha_vantage_common.py  # Alpha Vantage: shared utilities
│   │   ├── fred.py                  # FRED: macroeconomic indicators
│   │   ├── polymarket.py            # Polymarket: prediction market probabilities
│   │   ├── stocktwits.py            # StockTwits: retail sentiment
│   │   ├── reddit.py                # Reddit: finance subreddit posts
│   │   └── market_data_validator.py # Verified market snapshot builder
│   │
│   ├── graph/                       # LangGraph workflow orchestration
│   │   ├── __init__.py
│   │   ├── trading_graph.py         # TradingAgentsGraph (main entry class)
│   │   ├── setup.py                 # GraphSetup (StateGraph construction)
│   │   ├── propagation.py           # Propagator (state initialization)
│   │   ├── conditional_logic.py     # ConditionalLogic (debate loop routing)
│   │   ├── analyst_execution.py     # AnalystWallTimeTracker, execution plan
│   │   ├── signal_processing.py     # SignalProcessor (rating extraction)
│   │   ├── reflection.py            # Reflector (decision reflection)
│   │   └── checkpointer.py          # LangGraph checkpoint resume support
│   │
│   └── llm_clients/                 # Multi-provider LLM abstraction
│       ├── __init__.py
│       ├── base_client.py           # BaseLLMClient (abstract), normalize_content()
│       ├── factory.py               # create_llm_client() -- lazy provider imports
│       ├── openai_client.py         # OpenAI, xAI, DeepSeek, Ollama, OpenRouter, etc.
│       ├── anthropic_client.py      # Anthropic Claude
│       ├── google_client.py         # Google Gemini
│       ├── azure_client.py          # Azure OpenAI
│       ├── bedrock_client.py        # AWS Bedrock (optional [bedrock] extra)
│       ├── model_catalog.py         # Known model lists per provider
│       ├── capabilities.py          # Per-model capability detection
│       ├── validators.py            # Model name validation
│       └── api_key_env.py           # Provider → API key env var mapping
│
├── cli/                             # Interactive CLI
│   ├── __init__.py
│   ├── main.py                      # Typer app, live Rich dashboard
│   ├── utils.py                     # User prompts, provider selection
│   ├── config.py                    # CLI config helpers
│   ├── announcements.py             # Fetch/display project announcements
│   ├── stats_handler.py             # StatsCallbackHandler (LLM/tool token tracking)
│   ├── models.py                    # Typer model definitions
│   └── static/welcome.txt           # ASCII art welcome banner
│
├── tests/                           # Pytest suite (30+ test files)
├── scripts/                         # Utility scripts
└── assets/                          # Images (schema.png, agent.png, etc.)
```

---

## 11. Dependencies

### 11.1 Core Dependencies

| Package | Purpose |
|---------|---------|
| `langchain-core` | LLM abstractions, tool definitions, messages |
| `langgraph` | StateGraph workflow orchestration |
| `langchain-openai` | OpenAI LLM client |
| `langchain-anthropic` | Anthropic Claude client |
| `langchain-google-genai` | Google Gemini client |
| `langchain-experimental` | Experimental features |
| `langgraph-checkpoint-sqlite` | SQLite-based checkpoint persistence |
| `yfinance` | Yahoo Finance data (stock prices, fundamentals, news) |
| `backtrader` | Backtesting framework |
| `pandas` | Data manipulation |
| `stockstats` | Technical indicators (RSI, MACD, Bollinger, ATR, etc.) |
| `typer` | CLI framework |
| `rich` | Rich terminal UI (live dashboard) |
| `python-dotenv` | .env file loading |
| `questionary` | Interactive prompts |
| `redis` | Caching |
| `requests` | HTTP requests |
| `parsel` | HTML parsing |
| `tqdm` | Progress bars |
| `pytz` | Timezone handling |
| `typing-extensions` | Type hints compatibility |
| `setuptools` | Build system |

### 11.2 Optional Dependencies

| Extra | Packages | Purpose |
|-------|---------|---------|
| `dev` | `ruff`, `pytest`, `pytest-subtests` | Development tooling |
| `bedrock` | `langchain-aws` | AWS Bedrock support |

---

## 12. Reimplementation Guide

To reimplement this system in another language or framework, follow this structure:

### 12.1 Required Capabilities

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

### 12.2 Component Mapping

| TradingAgents Component | Equivalent in Other Frameworks |
|------------------------|-------------------------------|
| LangGraph StateGraph | Durable AI Studio, AutoGen, CrewAI, LangChain Agents, custom FSM |
| @tool-decorated functions | LangChain Tools, LlamaIndex Tools, custom function wrappers |
| Pydantic schemas | JSON Schema, Zod, TypeScript interfaces, Protobuf |
| create_llm_client() factory | Any LLM SDK wrapper (LiteLLM, Instructor, OpenAI SDK) |
| route_to_vendor() | Any provider abstraction pattern (Strategy pattern) |
| TradingMemoryLog | Any append-only log (event sourcing pattern) |
| Rich live dashboard | ncurses, TUI framework, or web dashboard |

### 12.3 Agent Prompt Structure

Each agent follows this pattern:
1. **System prompt**: Role definition + instructions + constraints
2. **Context injection**: Analyst reports, debate history, memory log context
3. **Tool definitions** (for analysts): Available tools with descriptions
4. **Structured output** (for decision agents): Pydantic schema with field descriptions
5. **Rating scale guidance**: Explicit scale definitions for consistent ratings

### 12.4 Data Flow Requirements

1. **Ticker resolution**: Convert ticker symbol to canonical form (handle exchange suffixes, crypto symbols)
2. **Data fetching**: OHLCV, fundamentals, news, sentiment, macro indicators, prediction markets
3. **Indicator computation**: Technical indicators from OHLCV data (SMA, EMA, RSI, MACD, Bollinger, ATR, VWMA, MFI)
4. **State accumulation**: Each node's output becomes context for subsequent nodes
5. **Debate loop control**: Count-based termination (rounds * 2 for bull/bear, rounds * 3 for risk)
6. **Return calculation**: Post-trade return vs benchmark (alpha) computation
7. **Reflection generation**: LLM-generated analysis of past prediction accuracy

### 12.5 Configuration Requirements

- Single source of truth config dict
- Environment variable overrides with type coercion
- Provider-specific settings (thinking level, reasoning effort, temperature)
- Vendor selection per data category
- Benchmark mapping per market suffix
- Path configuration for results, cache, and memory

---

## 13. Supported Markets

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

## 14. Technical Indicators

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

## 15. API Reference

### 15.1 Programmatic API

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

### 15.2 CLI Commands

```bash
# Launch interactive CLI
tradingagents

# With checkpoint enabled
tradingagents analyze --checkpoint

# Clear all checkpoints
tradingagents analyze --clear-checkpoints
```

---

## 16. Directory Structure (Reimplementation Checklist)

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
