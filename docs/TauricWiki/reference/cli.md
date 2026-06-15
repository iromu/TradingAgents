---
title: "CLI"
type: "reference"
status: "active"
language: "default"
source_paths: [cli/main.py, cli/utils.py, cli/config.py, cli/announcements.py, cli/stats_handler.py]
updated_at: "2026-06-14"
---

# CLI

The CLI is built with [Typer](https://typer.tiangolo.com/) and [Rich](https://github.com/Textualize/rich). It provides an interactive, real-time dashboard during analysis runs.

## Launch

```bash
tradingagents          # installed command
python -m cli.main     # alternative: run directly from source
```

## Interactive workflow

The CLI walks through 8 steps:

1. **Ticker symbol** — Enter a ticker with exchange suffix (e.g., `SPY`, `0700.HK`, `BTC-USD`). Defaults to `SPY`.
2. **Analysis date** — Enter date in `YYYY-MM-DD` format. Defaults to today.
3. **Output language** — Language for analyst reports (agent debate stays in English).
4. **Analysts** — Select which analysts to run (Market, Sentiment, News, Fundamentals).
5. **Research depth** — Select research depth level.
6. **LLM provider** — Select provider (OpenAI, Google, Anthropic, etc.). Skipped if `TRADINGAGENTS_LLM_PROVIDER` is set.
7. **Thinking agents** — Select deep and shallow thinking models. Skipped if `TRADINGAGENTS_DEEP_THINK_LLM` / `TRADINGAGENTS_QUICK_THINK_LLM` are set.
8. **Additional settings** — Ollama endpoint confirmation, OpenAI-compatible URL, provider-specific config (Qwen region, GLM region, MiniMax region, Gemini thinking, OpenAI reasoning effort, Anthropic effort).

## Live dashboard

During execution, the CLI shows a Rich-based live dashboard with:

- **Header**: Welcome message
- **Progress panel**: Agent status per team (pending / in_progress / completed)
- **Messages panel**: Recent tool calls and agent messages (newest first)
- **Analysis panel**: Current report being generated
- **Footer**: Stats (agents completed, LLM calls, tool calls, tokens, reports, elapsed time)

## CLI subcommands

```bash
tradingagents analyze [--checkpoint] [--clear-checkpoints]
```

## Announcements

The CLI fetches and displays announcements from the project's update feed on startup. Failures are silent.

## Asset type detection

The CLI auto-detects whether a ticker is a stock or crypto from its format (e.g., `BTC-USD` → crypto, `AAPL` → stock).
