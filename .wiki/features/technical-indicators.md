---
title: "Custom Technical Indicators"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/indicators/MFIIndicator.java"
  - "src/main/java/com/embabel/gekko/indicators/SubtractIndicator.java"
  - "src/main/java/com/embabel/gekko/indicators/VWAPIndicator.java"
  - "src/main/java/com/embabel/gekko/indicators/VWMAIndicator.java"
  - "src/main/java/com/embabel/gekko/util/IndicatorMapper.java"
  - "src/test/java/com/embabel/gekko/indicators/MFIIndicatorTest.java"
  - "src/test/java/com/embabel/gekko/indicators/SubtractIndicatorTest.java"
  - "src/test/java/com/embabel/gekko/indicators/VWAPIndicatorTest.java"
  - "src/test/java/com/embabel/gekko/indicators/VWMAIndicatorTest.java"
updated_at: "2026-06-13"
---

# Custom Technical Indicators

Gekko extends TA4J with four custom indicators that are not provided by the library itself. These are mapped by code via `[[indicator-mapper]]` and consumed through `[[market-data-tools]]`.

## Available Indicators

### MFIIndicator — Money Flow Index

A volume-weighted momentum indicator that measures buying and selling pressure. Returns a value between 0 and 100.

- **Logic:** Compares typical price (high+low+close)/3 multiplied by volume across consecutive bars, accumulating positive and negative money flow.
- **Rolling window:** Configurable `period` (default 14).
- **Edge case:** Returns 100 when negative flow is zero (all price increases).

### VWAPIndicator — Volume Weighted Average Price

Rolling-window VWAP: the ratio of price-volume sum to volume sum over a lookback window.

- **Logic:** Uses typical price (high+low+close)/3 × volume, not just close × volume.
- **Rolling window:** Configurable `period` (default 20).
- **Edge case:** Returns NaN when volume sum is zero.

### VWMAIndicator — Volume Weighted Moving Average

Similar to VWAP but uses close price instead of typical price.

- **Logic:** close price × volume / sum(volume) over the lookback window.
- **Rolling window:** Configurable `period` (default 20).
- **Edge case:** Returns NaN when volume sum is zero.

### SubtractIndicator — Indicator Subtraction

Generic indicator that subtracts one TA4J indicator from another: `left - right`.

- **Use case:** Used to build MACD line (EMA12 - EMA26) and MACD histogram (MACD line - signal line).
- **Unstable bars:** Takes the maximum of both operands' unstable bar counts.

## Integration

| Component | Role |
|-----------|------|
| `IndicatorMapper` | Maps string codes (e.g., "macd", "rsi", "vwma") to TA4J indicator instances → `[[indicator-mapper]]` |
| `MarketDataTools.get_indicators()` | Exposes indicator calculation to LLM agents → `[[market-data-tools]]` |
| `YFinService.getStockStatsIndicatorsWindow()` | Orchestrates indicator creation from stock data |

## Testing

Each indicator has a dedicated test class under `src/test/java/com/embabel/gekko/indicators/`:

- `MFIIndicatorTest.java`
- `SubtractIndicatorTest.java`
- `VWAPIndicatorTest.java`
- `VWMAIndicatorTest.java`

Tests verify numerical correctness against known values and edge cases (zero volume, insufficient bars).
