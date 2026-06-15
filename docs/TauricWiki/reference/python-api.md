---
title: "Python API"
type: "reference"
status: "active"
language: "default"
source_paths: [tradingagents/graph/trading_graph.py, main.py]
updated_at: "2026-06-14"
---

# Python API

The main programmatic entry point is `TradingAgentsGraph`.

## Basic usage

```python
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG

ta = TradingAgentsGraph(debug=True, config=DEFAULT_CONFIG.copy())

# forward propagate
_, decision = ta.propagate("NVDA", "2026-01-15")
print(decision)
```

## Custom configuration

```python
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG

config = DEFAULT_CONFIG.copy()
config["llm_provider"] = "openai"
config["deep_think_llm"] = "gpt-5.5"
config["quick_think_llm"] = "gpt-5.4-mini"
config["max_debate_rounds"] = 2
config["temperature"] = 0.0

ta = TradingAgentsGraph(debug=True, config=config)
_, decision = ta.propagate("NVDA", "2026-01-15")
print(decision)
```

## Selecting analysts

```python
# Only run market and fundamentals analysts
ta = TradingAgentsGraph(
    selected_analysts=("market", "fundamentals"),
    config=config,
)
```

Available analysts: `market`, `social` (sentiment), `news`, `fundamentals`.

## Crypto support

```python
# Auto-detected from ticker, or pass explicitly
_, decision = ta.propagate("BTC-USD", "2026-01-15", asset_type="crypto")
```

## Checkpoint resume

```python
config["checkpoint_enabled"] = True
ta = TradingAgentsGraph(config=config)
_, decision = ta.propagate("NVDA", "2026-01-15")
```

## Callbacks

```python
from cli.stats_handler import StatsCallbackHandler

stats = StatsCallbackHandler()
ta = TradingAgentsGraph(callbacks=[stats], config=config)
_, decision = ta.propagate("NVDA", "2026-01-15")
print(stats.get_stats())  # LLM calls, tool calls, tokens
```

## Key methods on TradingAgentsGraph

| Method | Description |
|---|---|
| `propagate(ticker, date, asset_type="stock")` | Run the full pipeline. Returns `(state, rating)` |
| `resolve_instrument_context(ticker, asset_type)` | Resolve company identity for the ticker |
| `reflect_and_remember(n_position_returns)` | (Deprecated) Run reflection on past decisions |

## State returned

`propagate()` returns a tuple of `(final_state_dict, rating_string)`. The state dict contains all agent reports, debate histories, and the final decision. The rating string is the extracted 5-tier rating.
