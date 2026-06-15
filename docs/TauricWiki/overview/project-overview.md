---
title: "Project Overview"
type: "overview"
status: "active"
language: "default"
source_paths: [tradingagents/, cli/, main.py]
updated_at: "2026-06-14"
---

# Project Overview

TradingAgents is a multi-agent financial trading framework built on [LangGraph](https://langchain-ai.github.io/langgraph/). It decomposes the trading process into specialized roles — analysts, researchers, a trader, and risk managers — each powered by a large language model.

## Architecture at a glance

```
┌──────────────────────────────────────────────────────────────┐
│  TradingAgentsGraph (main entry point)                       │
│  ┌────────┬────────┬──────────┬──────────┬────────────────┐ │
│  │Market  │Sentimt │  News    │Fundams   │  (configurable) │ │
│  │Analyst │ nt     │  Analyst │ Analyst │  │               │ │
│  └───┬────┴───┬────┴────┬─────┴────┬─────┘ │               │ │
│      └───────┴──────────┴──────────┴───────┘ │               │ │
│              ▼                                ▼               │ │
│     ┌────────────────┐            ┌──────────────────┐       │ │
│     │ Bull ↔ Bear    │───►        │ Trader           │       │
│     │ Debate Loop    │  Research  │ Proposal         │       │
│     └────────────────┘  Manager   └────────┬─────────┘       │ │
│                                            ▼                 │ │
│                                    ┌──────────────────┐       │ │
│                                    │ Aggressive ↔     │       │
│                                    │ Conservative ↔   │       │
│                                    │ Neutral Debate   │       │
│                                    └────────┬─────────┘       │ │
│                                             ▼                 │ │
│                                      ┌──────────────┐        │ │
│                                      │ Portfolio Mgr  │───►   │
│                                      │ (final rating) │        │
│                                      └──────────────┘        │
└──────────────────────────────────────────────────────────────┘
```

## Key design principles

- **Modularity**: Each agent is a LangGraph node. Swap agents, add new ones, or remove them via the `selected_analysts` parameter.
- **Multi-provider LLM**: Supports OpenAI, Anthropic, Google Gemini, Azure OpenAI, AWS Bedrock, Ollama (local), and any OpenAI-compatible endpoint.
- **Multi-vendor data**: Data comes from pluggable vendors (Yahoo Finance, Alpha Vantage, FRED, Polymarket) with configurable fallback chains.
- **Learning over time**: The memory log records decisions and resolves them with actual returns on subsequent runs, injecting lessons back into agent prompts.
- **Crash recovery**: Optional LangGraph checkpointing lets interrupted runs resume from the last successful node.

## Project structure

| Path | Role |
|---|---|
| `tradingagents/graph/` | LangGraph workflow orchestration |
| `tradingagents/agents/` | All agent implementations |
| `tradingagents/dataflows/` | Data provider abstraction layer |
| `tradingagents/llm_clients/` | Multi-provider LLM client abstraction |
| `tradingagents/default_config.py` | Single source of truth for configuration |
| `cli/` | Interactive CLI (Typer + Rich) |
| `main.py` | Example script showing programmatic usage |
| `tests/` | Test suite |

## Supported markets

TradingAgents uses Yahoo Finance tickers with exchange suffixes:

- **US**: `AAPL`, `SPY` (no suffix)
- **Hong Kong**: `0700.HK`
- **Japan**: `7203.T`
- **London**: `AZN.L`
- **India**: `RELIANCE.NS` (NSE), `RELIANCE.BO` (BSE)
- **Canada**: `.TO`
- **Australia**: `.AX`
- **China A-shares**: `600519.SS` (Shanghai), `.SZ` (Shenzhen)
- **Crypto**: `BTC-USD`, `ETH-USD`
