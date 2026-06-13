---
title: "Test Coverage"
type: "note"
status: "active"
language: "default"
source_paths: []
updated_at: "2026-06-13"
---

# Test Coverage

As of commit `f41770e`, the project has unit tests for the following components:

## Custom Indicators

| Test Class | Source |
|------------|--------|
| `MFIIndicatorTest` | `src/test/java/com/embabel/gekko/indicators/MFIIndicatorTest.java` |
| `SubtractIndicatorTest` | `src/test/java/com/embabel/gekko/indicators/SubtractIndicatorTest.java` |
| `VWAPIndicatorTest` | `src/test/java/com/embabel/gekko/indicators/VWAPIndicatorTest.java` |
| `VWMAIndicatorTest` | `src/test/java/com/embabel/gekko/indicators/VWMAIndicatorTest.java` |

## Tools

| Test Class | Source |
|------------|--------|
| `FundamentalDataToolsTest` | `src/test/java/com/embabel/gekko/tools/FundamentalDataToolsTest.java` |
| `NewsDataToolsTest` | `src/test/java/com/embabel/gekko/tools/NewsDataToolsTest.java` |

## Utilities

| Test Class | Source |
|------------|--------|
| `DateUtilsTest` | `src/test/java/com/embabel/gekko/util/DateUtilsTest.java` |
| `IndicatorMapperTest` | `src/test/java/com/embabel/gekko/util/IndicatorMapperTest.java` |

## Gaps

- No tests for `MarketDataTools` (despite the wiki page listing a test source path)
- No tests for `TraderAgent` actions
- No tests for `RiskDebateService`
- No tests for `FileCache`
- No tests for data flow services (`AlphaVantageService`, `YFinService`)
- No integration tests
