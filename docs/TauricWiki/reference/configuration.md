---
title: "Configuration"
type: "reference"
status: "active"
language: "default"
source_paths: [tradingagents/default_config.py]
updated_at: "2026-06-14"
---

# Configuration

All configuration is centralized in `tradingagents/default_config.py`. The `DEFAULT_CONFIG` dict is the single source of truth.

## Environment variable overrides

Every config key can be overridden via `TRADINGAGENTS_*` environment variables (or `.env` file). The mapping is defined in `_ENV_OVERRIDES`:

| Env var | Config key | Type | Default |
|---|---|---|---|
| `TRADINGAGENTS_LLM_PROVIDER` | `llm_provider` | str | `openai` |
| `TRADINGAGENTS_DEEP_THINK_LLM` | `deep_think_llm` | str | `gpt-5.5` |
| `TRADINGAGENTS_QUICK_THINK_LLM` | `quick_think_llm` | str | `gpt-5.4-mini` |
| `TRADINGAGENTS_LLM_BACKEND_URL` | `backend_url` | str \| None | `None` |
| `TRADINGAGENTS_OUTPUT_LANGUAGE` | `output_language` | str | `English` |
| `TRADINGAGENTS_MAX_DEBATE_ROUNDS` | `max_debate_rounds` | int | `1` |
| `TRADINGAGENTS_MAX_RISK_ROUNDS` | `max_risk_discuss_rounds` | int | `1` |
| `TRADINGAGENTS_CHECKPOINT_ENABLED` | `checkpoint_enabled` | bool | `False` |
| `TRADINGAGENTS_BENCHMARK_TICKER` | `benchmark_ticker` | str \| None | `None` |
| `TRADINGAGENTS_TEMPERATURE` | `temperature` | float \| None | `None` |

## Directory paths

| Config key | Default | Description |
|---|---|---|
| `project_dir` | `tradingagents/` | Project root |
| `results_dir` | `~/.tradingagents/logs/` | State logs and reports |
| `data_cache_dir` | `~/.tradingagents/cache/` | Cached data and checkpoints |
| `memory_log_path` | `~/.tradingagents/memory/trading_memory.md` | Decision log |
| `memory_log_max_entries` | `None` | Max resolved entries (None = no limit) |

## LLM settings

| Config key | Default | Description |
|---|---|---|
| `llm_provider` | `openai` | Provider: openai, google, anthropic, xai, deepseek, qwen, glm, minimax, openrouter, ollama, openai_compatible, azure, bedrock |
| `deep_think_llm` | `gpt-5.5` | Model for complex reasoning (Research Manager, Portfolio Manager) |
| `quick_think_llm` | `gpt-5.4-mini` | Model for quick tasks (all other agents) |
| `backend_url` | `None` | Custom API endpoint (e.g., Ollama, vLLM, LM Studio) |
| `google_thinking_level` | `None` | Gemini thinking level: "high", "minimal", etc. |
| `openai_reasoning_effort` | `None` | OpenAI reasoning effort: "high", "medium", "low" |
| `anthropic_effort` | `None` | Anthropic Claude effort: "high", "medium", "low" |
| `temperature` | `None` | Sampling temperature (cross-provider) |

## Debate settings

| Config key | Default | Description |
|---|---|---|
| `max_debate_rounds` | `1` | Max bull/bear debate rounds |
| `max_risk_discuss_rounds` | `1` | Max risk debate rounds |
| `max_recur_limit` | `100` | Max tool-calling recursion per agent |
| `analyst_concurrency_limit` | `1` | Concurrent analyst execution (1 = sequential) |

## Data fetching

| Config key | Default | Description |
|---|---|---|
| `news_article_limit` | `20` | Max articles per ticker (ticker-news) |
| `global_news_article_limit` | `10` | Max articles for global/macro news |
| `global_news_lookback_days` | `7` | Macro news lookback window |
| `global_news_queries` | (list) | Search queries for global news |

## Data vendors

| Config key | Default | Description |
|---|---|---|
| `data_vendors` | (dict) | Category-level vendor config |
| `tool_vendors` | (dict) | Tool-level vendor config (overrides category) |

## Benchmark

| Config key | Default | Description |
|---|---|---|
| `benchmark_ticker` | `None` | Override all benchmark resolution |
| `benchmark_map` | (dict) | Auto-detect benchmark by ticker suffix |

## Using custom config

```python
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG

config = DEFAULT_CONFIG.copy()
config["llm_provider"] = "anthropic"
config["deep_think_llm"] = "claude-opus-4"
config["max_debate_rounds"] = 2

ta = TradingAgentsGraph(debug=True, config=config)
_, decision = ta.propagate("AAPL", "2026-01-15")
```
