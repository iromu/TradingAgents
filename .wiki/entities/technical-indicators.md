---
title: "Technical Indicators"
type: "entity"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/indicators/"
  - "src/main/java/com/embabel/gekko/util/IndicatorMapper.java"
updated_at: "2026-06-11"
---

# Technical Indicators

Gekko uses the TA4J library for technical analysis and includes several custom indicators.

## TA4J Integration

The `IndicatorMapper` utility maps TA4Z indicator calculations to the data the Market Analyst needs.

## Custom Indicators

| Indicator | File | Purpose |
|-----------|------|---------|
| **VWAP** | `VWAPIndicator.java` | Volume-Weighted Average Price — average price weighted by volume |
| **VWMA** | `VWMAIndicator.java` | Volume-Weighted Moving Average |
| **MFI** | `MFIIndicator.java` | Money Flow Index — volume-weighted RSI |
| **Subtract** | `SubtractIndicator.java` | Custom TA4Z indicator for computing price differences |

## Why Custom Indicators?

TA4Z provides a core set of indicators, but Gekko needs some specialized calculations:
- VWAP is essential for understanding where volume-weighted price levels are
- MFI adds volume context to the traditional RSI
- SubtractIndicator enables custom price difference calculations

## How They're Used

The Market Analyst agent receives indicator values as context in its prompt. The indicators are calculated from historical price data and passed to the LLM as structured data.
