---
title: "Reproducibility"
type: "feature"
status: "active"
language: "default"
source_paths: [README.md, tradingagents/graph/trading_graph.py]
updated_at: "2026-06-14"
---

# Reproducibility

TradingAgents is LLM-driven, so two runs of the same ticker and date can differ. This is expected for a research tool built on language models, not a defect.

## Sources of variation

1. **LLM sampling is non-deterministic** — Even at fixed temperature, providers don't guarantee byte-identical output. Reasoning models vary the most.
2. **Live data moves** — News, StockTwits, and Reddit return different content as time passes. Pinning the analysis date holds price and indicator windows fixed, but social/news sources still reflect "now".

## Reducing variation

Set a low temperature and use a non-reasoning model:

```python
config = DEFAULT_CONFIG.copy()
config["llm_provider"] = "openai"
config["deep_think_llm"] = "gpt-4.1"      # non-reasoning model honors temperature
config["quick_think_llm"] = "gpt-4.1"
config["temperature"] = 0.0
```

## What's deterministic now

- **Company identity** is resolved deterministically from the ticker before any agent runs (see [[concepts/instrument-identity]])
- **Market analyst** grounds exact price and indicator claims in a verified data snapshot (`get_verified_market_snapshot`)
- Earlier reports of "different companies" or fabricated price levels across runs are addressed by these two mechanisms

## What's not guaranteed

Backtest results are **not** guaranteed to match any published figure. Returns depend on the model, temperature, date range, data quality, and sampling. Treat the framework as a research scaffold for studying multi-agent analysis, not as a strategy with a fixed, replicable return.
