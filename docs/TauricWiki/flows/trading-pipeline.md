---
title: "Trading Pipeline"
type: "flow"
status: "active"
language: "default"
source_paths: [tradingagents/graph/trading_graph.py, tradingagents/graph/setup.py]
updated_at: "2026-06-14"
---

# Trading Pipeline

The trading pipeline is a LangGraph `StateGraph` that executes a fixed sequence of phases with conditional loops for debate.

## Pipeline phases

### Phase 1: Analyst Data Collection

Four analyst agents collect data in sequence (or concurrently if `analyst_concurrency_limit > 1`):

1. **Market Analyst** — fetches OHLCV prices and technical indicators (RSI, MACD, Bollinger Bands, ATR)
2. **Sentiment Analyst** — aggregates news headlines, StockTwits, and Reddit chatter (pre-fetched, no tool calls at runtime)
3. **News Analyst** — fetches global news, macro indicators (FRED), insider transactions, and prediction markets (Polymarket)
4. **Fundamentals Analyst** — retrieves balance sheet, cashflow, and income statement data

Each analyst calls its tools iteratively via LangGraph's `ToolNode`, then writes its report into the shared `AgentState`.

### Phase 2: Bull/Bear Debate

The **Bull Researcher** and **Bear Researcher** alternate turns, each reading all four analyst reports plus the debate history. The loop is controlled by `should_continue_debate()`, which tracks turn count against `2 * max_debate_rounds`. When the limit is reached, control passes to the **Research Manager**, which synthesizes the debate into a structured `ResearchPlan` with a recommendation, rationale, and strategic actions.

### Phase 3: Trader Proposal

The **Trader** reads the Research Manager's plan and all analyst reports, then produces a structured `TraderProposal` (Buy/Hold/Sell with entry price, stop-loss, and position sizing).

### Phase 4: Risk Debate

Three debaters — **Aggressive**, **Conservative**, and **Neutral** — rotate in a cycle. Each reads the trader's proposal plus the full debate history. The loop is controlled by `should_continue_risk_analysis()`, limiting to `3 * max_risk_discuss_rounds` turns. The cycle ends with the **Portfolio Manager**.

### Phase 5: Portfolio Manager Decision

The **Portfolio Manager** produces the final `PortfolioDecision` — a 5-tier rating (Buy / Overweight / Hold / Underweight / Sell) with an executive summary, investment thesis, price target, and time horizon. This is the system's final output.

## State flow

The shared `AgentState` (see [[entities/agent-state]]) carries all data between nodes. Key state keys:

| Key | Populated by |
|---|---|
| `market_report` | Market Analyst |
| `sentiment_report` | Sentiment Analyst |
| `news_report` | News Analyst |
| `fundamentals_report` | Fundamentals Analyst |
| `investment_debate_state` | Bull/Bear Researchers |
| `investment_plan` | Research Manager |
| `trader_investment_plan` | Trader |
| `risk_debate_state` | Risk Debators |
| `final_trade_decision` | Portfolio Manager |
| `past_context` | Memory system (injected at start) |

## Graph construction

The `GraphSetup` class (`tradingagents/graph/setup.py`) builds the `StateGraph`:

- Adds analyst nodes dynamically based on `selected_analysts`
- Wires each analyst → tool node → clear node → next analyst
- Adds the fixed debate and decision nodes
- Configures conditional edges for debate loops
- Compiles the graph with an optional checkpointer

## Entry point

```python
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG

ta = TradingAgentsGraph(debug=True, config=DEFAULT_CONFIG.copy())
_, decision = ta.propagate("NVDA", "2026-01-15")
print(decision)
```

The `propagate()` method handles memory resolution, checkpoint setup, graph execution, state logging, and memory log storage.
