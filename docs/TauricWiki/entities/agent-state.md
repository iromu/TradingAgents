---
title: "Agent Debate Flow"
type: "flow"
status: "active"
language: "default"
source_paths: [tradingagents/agents/utils/agent_states.py]
updated_at: "2026-06-14"
---

# Agent State

The shared state object (`AgentState`) carries all data between nodes in the LangGraph pipeline. It extends `MessagesState` from LangGraph.

## State fields

| Field | Type | Description |
|---|---|---|
| `messages` | `list[Message]` | LangGraph message history |
| `company_of_interest` | `str` | Ticker/symbol being analyzed |
| `asset_type` | `str` | `"stock"` or `"crypto"` |
| `instrument_context` | `str` | Deterministic ticker identity (company name, sector, exchange) |
| `trade_date` | `str` | Trading date (`YYYY-MM-DD`) |
| `sender` | `str` | Which agent sent the current message |
| `market_report` | `str` | Market analyst output |
| `sentiment_report` | `str` | Sentiment analyst output |
| `news_report` | `str` | News analyst output |
| `fundamentals_report` | `str` | Fundamentals analyst output |
| `investment_debate_state` | `InvestDebateState` | Bull/bear debate history |
| `investment_plan` | `str` | Research Manager's plan |
| `trader_investment_plan` | `str` | Trader's proposal |
| `risk_debate_state` | `RiskDebateState` | Risk debate history |
| `final_trade_decision` | `str` | Portfolio Manager's final decision |
| `past_context` | `str` | Memory log context (prior decisions injected at start) |

## InvestDebateState

Used in `investment_debate_state`:

| Field | Description |
|---|---|
| `bull_history` | Bull researcher's conversation history |
| `bear_history` | Bear researcher's conversation history |
| `history` | Combined conversation history |
| `current_response` | Latest response |
| `judge_decision` | Final judge decision |
| `count` | Length of the current conversation |

## RiskDebateState

Used in `risk_debate_state`:

| Field | Description |
|---|---|
| `aggressive_history` | Aggressive debator's history |
| `conservative_history` | Conservative debator's history |
| `neutral_history` | Neutral debator's history |
| `history` | Combined conversation history |
| `latest_speaker` | Analyst that spoke last |
| `current_aggressive_response` | Latest aggressive response |
| `current_conservative_response` | Latest conservative response |
| `current_neutral_response` | Latest neutral response |
| `judge_decision` | Judge's decision |
| `count` | Length of the current conversation |
