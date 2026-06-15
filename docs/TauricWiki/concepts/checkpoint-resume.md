---
title: "Checkpoint Resume"
type: "concept"
status: "active"
language: "default"
source_paths: [tradingagents/graph/checkpointer.py, tradingagents/graph/trading_graph.py]
updated_at: "2026-06-14"
---

# Checkpoint Resume

TradingAgents can resume a crashed or interrupted run from the last successful node instead of starting over. This is **opt-in** via the `--checkpoint` CLI flag or `checkpoint_enabled: True` in config.

## How it works

When enabled:

1. A per-ticker SQLite checkpoint database is created at `~/.tradingagents/cache/checkpoints/<TICKER>.db`
2. LangGraph saves state after each node completes
3. On a resume run, the graph detects the last completed step and resumes from there
4. Checkpoints are **cleared automatically** on successful completion

## CLI usage

```bash
tradingagents analyze --checkpoint           # enable for this run
tradingagents analyze --clear-checkpoints    # reset all checkpoints
```

## Programmatic usage

```python
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG

config = DEFAULT_CONFIG.copy()
config["checkpoint_enabled"] = True
ta = TradingAgentsGraph(debug=True, config=config)
_, decision = ta.propagate("NVDA", "2026-01-15")
```

## Log messages

- `Resuming from step N for <TICKER> on <date>` — a checkpoint was found
- `Starting fresh for <TICKER> on <date>` — no checkpoint exists

## Thread ID

Checkpoints are keyed by `(ticker, trade_date)`. The same ticker+date combination resumes; different dates start fresh.

## Cache directory

Override the base cache directory with `TRADINGAGENTS_CACHE_DIR` env var or `data_cache_dir` config key.
