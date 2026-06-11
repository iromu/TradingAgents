---
title: "Analyst Agents"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/TraderAgent.java"
  - "src/main/java/com/embabel/gekko/domain/Analysts.java"
  - "src/main/java/com/embabel/gekko/tools/FundamentalDataTools.java"
  - "src/main/java/com/embabel/gekko/tools/NewsDataTools.java"
  - "src/main/resources/prompts/analysts/"
updated_at: "2026-06-11"
---

# Analyst Agents

The system has **four analyst agents**, each responsible for collecting and analyzing a different type of data about a stock ticker.

## The Four Analysts

| Analyst | What it does | Data source | Prompt file |
|---------|-------------|-------------|-------------|
| **Fundamentals** | Financial statements, ratios, company health | Alpha Vantage / Yahoo Finance | `prompts/analysts/FundamentalsAnalyst.txt` |
| **Market** | Stock price data, technical indicators | Alpha Vantage / TA4J | `prompts/analysts/MarketAnalyst.txt` |
| **News** | Recent news articles and sentiment | Alpha Vantage News API | `prompts/analysts/NewsAnalyst.txt` |
| **Social Media** | Social sentiment and discussion | Alpha Vantage News API | `prompts/analysts/SocialMediaAnalyst.txt` |

## How They Work

Each analyst is a `@Action` method on `TraderAgent`:

1. Takes a `Ticker` object as input
2. Calls external data tools (e.g., `FundamentalDataTools`, `NewsDataTools`)
3. Uses an LLM with a system prompt to analyze the raw data
4. Returns a typed report record (`FundamentalsReport`, `MarketReport`, etc.)
5. Results are cached in `FileCache` by key (e.g., `AAPL_fundamentals`)

## Shared Pattern

All analysts follow the same structure:

```
Ticker → [data collection] → [LLM analysis with system prompt] → Report
```

They use a shared base template `prompts/analysts/_BaseAnalyst.jinja` that handles the common structure, with each analyst providing its own `system_message` prompt.

## Tool Integration

- **Fundamentals** uses `FundamentalDataTools` — calls Alpha Vantage for financial statements
- **News** uses `NewsDataTools` — fetches news and sentiment from Alpha Vantage
- **Market** uses custom TA4J indicators (VWAP, MFI, VWMA, etc.)
- **Social Media** reuses `NewsDataTools` with a different system prompt
