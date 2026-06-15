---
title: "Fragile Invariants"
type: "risk"
status: "active"
language: "default"
source_paths: [tradingagents/dataflows/interface.py, tradingagents/agents/utils/agent_utils.py, tradingagents/agents/utils/rating.py, tradingagents/agents/agents/analysts/sentiment_analyst.py]
updated_at: "2026-06-14"
---

# Fragile Invariants

Things that are easy to break and hard to debug. Review before making changes.

## Vendor routing must not silently fall back

`dataflows/interface.py:route_to_vendor()` must **not** fall back to vendors the user didn't configure. The configured vendor chain IS the chain. Silent fallback to unconfigured vendors returned data from unexpected sources and caused cross-vendor inconsistencies ([#988](https://github.com/TauricResearch/TradingAgents/issues/988), [#289](https://github.com/TauricResearch/TradingAgents/issues/289)).

## Instrument identity must be resolved before any agent runs

`resolve_instrument_identity()` in `agents/utils/agent_utils.py` does a deterministic yfinance lookup to get the real company name, sector, and exchange. This is injected as `instrument_context` into every agent's prompt. Without it, the LLM can hallucinate a different company when analyzing chart patterns ([#814](https://github.com/TauricResearch/TradingAgents/issues/814)).

## Sentiment Analyst must pre-fetch data

The Sentiment Analyst was redesigned in v0.2.5. The old "social media analyst" had no real social data but was prompted to analyze it, causing hallucination. The new version pre-fetches all three data sources (news, StockTwits, Reddit) before invoking the LLM. Do not revert to the old tool-calling pattern.

## 5-tier rating parsing is fragile

`agents/utils/rating.py` greps for specific keywords (`Buy`, `Sell`, `Hold`, `Overweight`, `Underweight`) in the Portfolio Manager's prose output. If the PM's output format changes significantly, the rating extraction will break silently. The structured output path (`PortfolioDecision` Pydantic schema) avoids this but only applies when the provider supports structured output.

## Benchmark resolution must handle all ticker suffixes

`TradingAgentsGraph._resolve_benchmark()` in `trading_graph.py` matches ticker suffixes against `benchmark_map`. Unrecognized suffixes (including US tickers with dots like `BRK.B`) fall back to SPY. Adding a new market requires updating both `benchmark_map` and the CLI's market documentation.

## Memory log format is parseable by regex

`agents/utils/memory.py` uses regex patterns (`_DECISION_RE`, `_REFLECTION_RE`) to parse log entries. The `<!-- ENTRY_END -->` separator is an HTML comment chosen specifically because it cannot appear in LLM prose output. Changing the format requires updating both the writer and the parser.

## Checkpoint thread_id must be ticker+date

`graph/checkpointer.py:thread_id()` keys checkpoints by `(ticker, trade_date)`. Changing this breaks resume semantics — different dates would collide, or the same date+ticker wouldn't resume.

## Tool vendor config is strict

`data_vendors` and `tool_vendors` in the config are exact — if you configure `"alpha_vantage"` for a category, only Alpha Vantage is used. If the API key is missing, `VendorNotConfiguredError` is raised (after trying other vendors in the chain). Silent fallback to yfinance does not happen.
