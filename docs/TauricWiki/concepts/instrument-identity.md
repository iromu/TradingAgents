---
title: "Instrument Identity"
type: "concept"
status: "active"
language: "default"
source_paths: [tradingagents/agents/utils/agent_utils.py]
updated_at: "2026-06-14"
---

# Instrument Identity

`resolve_instrument_identity()` in `tradingagents/agents/utils/agent_utils.py` does a deterministic yfinance lookup to get the real company name, sector, industry, and exchange for a given ticker.

## Why it matters

Without this, the LLM can hallucinate a different company when analyzing chart patterns. For example, two tickers with similar price patterns might be confused by the model. The resolved identity is injected into every agent's prompt as `instrument_context`, anchoring the model to the real company.

## How it works

1. Calls `yf.Ticker(ticker).info` to fetch company metadata
2. Results are cached (LRU) to avoid repeated API calls
3. The result is formatted into a context string and injected into every agent's prompt
4. Fail-open: if the lookup fails, the system continues without the context (graceful degradation)

## CLI integration

Both the `propagate()` path and the CLI call `resolve_instrument_context()` so the resolved identity reaches the whole graph regardless of entry point.

## Symbol normalization

For crypto and commodities, `dataflows/symbol_utils.py:normalize_symbol()` handles ticker normalization (e.g., `XAUUSD` → `GC=F`) so the identity resolution hits the correct instrument.
