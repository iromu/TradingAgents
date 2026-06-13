---
title: "Indicator Mapper"
type: "reference"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/IndicatorMapper.java"
  - "src/test/java/com/embabel/gekko/util/IndicatorMapperTest.java"
updated_at: "2026-06-13"
---

# Indicator Mapper

`IndicatorMapper` provides a code-to-TA4J-indicator factory. It maps short string codes (e.g., "SMA", "RSI", "MACD") to fully constructed TA4J `Indicator<Num>` instances.

## Supported Indicator Codes

| Code | Indicator | Description |
|------|-----------|-------------|
| `close_50_sma` | `SMAIndicator(close, 50)` | 50-day simple moving average |
| `close_200_sma` | `SMAIndicator(close, 200)` | 200-day simple moving average |
| `close_10_ema` | `EMAIndicator(close, 10)` | 10-day exponential moving average |
| `macd` | `SubtractIndicator(EMA12, EMA26)` | MACD line (12-period EMA minus 26-period EMA) |
| `macds` | `EMAIndicator(macdLine, 9)` | MACD signal line |
| `macdh` | `SubtractIndicator(macdLine, signalLine)` | MACD histogram |
| `rsi` | `RSIIndicator(close, 14)` | 14-period relative strength index |
| `vwma` | `VWAPIndicator(series, 20)` | 20-period volume-weighted average price |
| `atr` | `ATRIndicator(series, 14)` | 14-period average true range |
| `boll` | `SMAIndicator(close, 20)` | Bollinger Bands middle line (20 SMA) |

## Integration

- Called by `MarketDataTools.get_indicators()` when an agent requests indicator calculations.
- The `getDescriptions()` method returns a map of codes to human-readable descriptions, useful for LLM tool descriptions.
- Custom indicators (VWAP, Subtract) are mapped through this factory to keep the LLM-facing API simple.
