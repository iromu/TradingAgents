---
title: "Market Data Tools"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/tools/MarketDataTools.java"
  - "src/test/java/com/embabel/gekko/tools/MarketDataToolsTest.java"
updated_at: "2026-06-13"
---

# Market Data Tools

`MarketDataTools` provides LLM-accessible tools for fetching stock data and calculating technical indicators.

## Available Tools

### `get_stock_data`

Fetches historical OHLCV stock price data.

| Parameter | Description |
|-----------|-------------|
| `ticker` | Ticker symbol (e.g., "AAPL") |
| `startDate` | Start date in yyyy-MM-dd format |
| `endDate` | End date in yyyy-MM-dd format |

**Source:** `YFinService.getYFinDataOnline()`

### `get_indicators`

Calculates technical indicators using TA4J.

| Parameter | Description |
|-----------|-------------|
| `ticker` | Ticker symbol (e.g., "AAPL") |
| `indicators` | Comma-separated list of indicator codes (e.g., "SMA,RSI,MACD") |
| `currDate` | Current date in yyyy-MM-dd format |
| `lookbackDays` | Number of days for lookback window |

**Source:** `YFinService.getStockStatsIndicatorsWindow()`

## Integration

The tools are injected into `TraderAgent` and exposed to LLM agents as `@Tool` methods. Agents can call them to get real-time market data and technical analysis.

## Error Handling

Both tools catch exceptions and return error messages as strings rather than throwing, making them safe for LLM consumption.
