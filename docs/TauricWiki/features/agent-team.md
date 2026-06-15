---
title: "Agent Team"
type: "feature"
status: "active"
language: "default"
source_paths: [tradingagents/agents/]
updated_at: "2026-06-14"
---

# Agent Team

TradingAgents deploys 12 specialized agents organized into five teams. Each agent is a LangGraph node created by a factory function (e.g., `create_market_analyst(llm)`).

## Analyst Team (Phase 1)

Four analysts collect and analyze data. Each is a tool-calling LLM node.

| Agent | Tools | Output |
|---|---|---|
| **Market Analyst** | `get_stock_data`, `get_indicators`, `get_verified_market_snapshot` | Technical analysis report with price data and indicators |
| **Sentiment Analyst** | Pre-fetched data only (no tool calls) | Sentiment summary from news, StockTwits, Reddit |
| **News Analyst** | `get_news`, `get_global_news`, `get_macro_indicators`, `get_prediction_markets` | Macro and news analysis report |
| **Fundamentals Analyst** | `get_fundamentals`, `get_balance_sheet`, `get_cashflow`, `get_income_statement` | Financial statement analysis |

> **Note**: The Sentiment Analyst was redesigned in v0.2.5. The old "social media analyst" had no real social data but was prompted to analyze it, causing hallucination. The new version pre-fetches all three data sources (news, StockTwits, Reddit) before invoking the LLM.

## Researcher Team (Phase 2)

Two agents debate the investment case, then a manager synthesizes.

| Agent | Role |
|---|---|
| **Bull Researcher** | Argues for the position — growth potential, competitive advantages, positive indicators |
| **Bear Researcher** | Argues against the position — risks, challenges, negative indicators |
| **Research Manager** | Synthesizes the debate into a structured `ResearchPlan` (recommendation, rationale, strategic actions) |

## Trader (Phase 3)

| Agent | Role |
|---|---|
| **Trader** | Converts the Research Manager's plan into a concrete `TraderProposal` (Buy/Hold/Sell with entry price, stop-loss, position sizing) |

## Risk Management Team (Phase 4)

Three debaters critique the trader's proposal from different risk perspectives.

| Agent | Perspective |
|---|---|
| **Aggressive Debator** | Champions high-risk, high-reward strategies |
| **Conservative Debator** | Prioritizes capital preservation and risk mitigation |
| **Neutral Debator** | Provides balanced, moderate perspective |

## Portfolio Manager (Phase 5)

| Agent | Role |
|---|---|
| **Portfolio Manager** | Makes the final `PortfolioDecision` — a 5-tier rating (Buy / Overweight / Hold / Underweight / Sell) with executive summary, investment thesis, price target, and time horizon |

## Structured output

Three agents use structured output (Pydantic schemas) for consistent formatting:

- **Research Manager** → `ResearchPlan` (see [[entities/schemas]])
- **Trader** → `TraderProposal`
- **Portfolio Manager** → `PortfolioDecision`

These use the `bind_structured()` / `invoke_structured_or_freetext()` pattern in `agents/utils/structured.py`, which wraps the LLM with provider-native structured output and falls back to free-text if unavailable.

## LLM assignment

| LLM | Used by |
|---|---|
| `deep_think_llm` (e.g., GPT-5.5) | Research Manager, Portfolio Manager |
| `quick_think_llm` (e.g., GPT-5.4-mini) | All analysts, researchers, trader, debaters |
