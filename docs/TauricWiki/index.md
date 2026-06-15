---
title: "TradingAgents Wiki"
type: "index"
status: "active"
language: "default"
last_commit: "c15200dc286b66abce3f1bcf09b298dc06b8539d"
updated_at: "2026-06-14"
---

# TradingAgents Wiki

## What is this?

**TradingAgents** is a multi-agent LLM financial trading framework. It deploys specialized AI agents — analysts, researchers, a trader, and risk managers — that collaborate through structured debate to produce trading decisions (Buy / Hold / Sell) for stocks and crypto. Built on LangGraph, it mirrors how a real trading firm's desk operates.

## Get Started

1. **Install**: `pip install .` (requires Python 3.10+)
2. **Configure**: Copy `.env.example` → `.env` and fill in your LLM API key (e.g. `OPENAI_API_KEY`)
3. **Run the CLI**: `tradingagents` or `python -m cli.main`
4. **First files to read**: This wiki's [[overview/project-overview]], [[flows/trading-pipeline]], and the [README](https://github.com/TauricResearch/TradingAgents/blob/main/README.md)
5. **Safe first change**: Swap `llm_provider` in your `.env` to try a different model
6. **Tempting dangerous change**: Lower `max_debate_rounds` to 0 — the debate loop will be skipped entirely

## Why does it exist?

The project (backed by [Tauric Research](https://tauric.ai)) is a research scaffold for studying how multi-agent LLM systems reason about financial markets. It is **not** financial advice and trading performance varies widely. See the [arXiv paper](https://arxiv.org/abs/2412.20138).

## What happens when I run it?

The CLI walks you through selecting a ticker, date, LLM provider, and analysts. Then the LangGraph pipeline runs:

1. **Analysts** collect data (prices, indicators, news, sentiment, fundamentals)
2. **Bull and Bear researchers** debate the investment case
3. **Trader** converts the debate into a Buy/Hold/Sell proposal
4. **Risk debaters** (aggressive, conservative, neutral) critique the proposal
5. **Portfolio Manager** makes the final rating (Buy / Overweight / Hold / Underweight / Sell)

Results are saved as markdown reports in `~/.tradingagents/logs/` and the decision is logged to `~/.tradingagents/memory/trading_memory.md` for reflection on future runs.

## Where is data saved?

| Location | Purpose |
|---|---|
| `~/.tradingagents/logs/` | Per-ticker JSON state logs and markdown reports |
| `~/.tradingagents/memory/trading_memory.md` | Append-only decision log with reflection |
| `~/.tradingagents/cache/` | Cached data and optional checkpoint DBs |
| `results/` (project root) | Local results directory (if not overridden) |

## What are the important moving parts?

- **[[flows/trading-pipeline]]** — The full agent workflow from data collection to final decision
- **[[flows/agent-debate]]** — Bull/bear and risk debate loops
- **[[features/agent-team]]** — The analyst, researcher, trader, and risk management agents
- **[[features/data-vendor-layer]]** — How data is fetched from Yahoo Finance, Alpha Vantage, FRED, Polymarket
- **[[features/llm-provider-layer]]** — Multi-provider LLM client abstraction (OpenAI, Anthropic, Google, Azure, Bedrock, Ollama, etc.)
- **[[features/reproducibility]]** — Why runs vary and how to reduce variation
- **[[concepts/memory-system]]** — The decision log and reflection mechanism
- **[[concepts/checkpoint-resume]]** — Crash recovery via LangGraph checkpoints
- **[[concepts/instrument-identity]]** — Deterministic company resolution prevents hallucination

## What should I avoid breaking?

See [[risks/fragile-invariants]] for the full list. Key things:

- The **vendor routing chain** (`dataflows/interface.py`) must not silently fall back to unconfigured vendors
- **Instrument identity resolution** (`resolve_instrument_identity`) prevents the LLM from hallucinating a different company
- The **Sentiment Analyst** now pre-fetches data before invoking the LLM — don't revert to the old tool-calling pattern
- The **5-tier rating** parsing (`agents/utils/rating.py`) is fragile — it greps for specific keywords in the PM's prose

## Where do I look first?

| Need | Go to |
|---|---|
| Understand the overall architecture | [[overview/project-overview]] |
| Follow the agent workflow | [[flows/trading-pipeline]] |
| Understand the debate loops | [[flows/agent-debate]] |
| Add a new data source | [[features/data-vendor-layer]] |
| Switch LLM provider | [[features/llm-provider-layer]] |
| Configure the system | [[reference/configuration]] |
| Use from Python code | [[reference/python-api]] |
| Use the CLI | [[reference/cli]] |
| Understand the shared state | [[entities/agent-state]] |
| Understand Pydantic schemas | [[entities/schemas]] |
| Learn about memory and reflection | [[concepts/memory-system]] |
| Learn about crash recovery | [[concepts/checkpoint-resume]] |
| Learn about reproducibility | [[features/reproducibility]] |
| Learn about company identity | [[concepts/instrument-identity]] |
| What to avoid breaking | [[risks/fragile-invariants]] |
