---
title: "Test Coverage"
type: "note"
status: "active"
language: "default"
source_paths:
  - "src/test/java/com/embabel/gekko/"
updated_at: "2026-07-06"
---

# Test Coverage

The project has **43 test classes** across agent, data, tool, utility, and integration layers.

## Agent Tests

### Core Agent Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `DebateAgentLLMTest` | `agent/DebateAgentLLMTest.java` | DebateAgent with mocked LLM |
| `DebateBriefsUnitTest` | `agent/DebateBriefsUnitTest.java` | Brief distillation logic |
| `DebateLoopAgentTest` | `agent/DebateLoopAgentTest.java` | Bull/bear loop and convergence detection |
| `OrchestratorAgentResearchPlanTest` | `agent/OrchestratorAgentResearchPlanTest.java` | Research plan generation |
| `PureLogicTest` | `agent/PureLogicTest.java` | Jaccard similarity, bigram extraction |
| `TraderAgentLLMTest` | `agent/TraderAgentLLMTest.java` | Legacy trader agent LLM tests |
| `TraderAgentTickerValidationTest` | `agent/TraderAgentTickerValidationTest.java` | Ticker format validation |

### Checkpoint Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `CheckpointAgentTest` | `agent/checkpoint/CheckpointAgentTest.java` | Save, restore, clear checkpoint actions |
| `CheckpointStoreTest` | `agent/checkpoint/CheckpointStoreTest.java` | JSON serialization, atomic writes, path traversal |
| `CheckpointResumeIntegrationTest` | `agent/checkpoint/CheckpointResumeIntegrationTest.java` | End-to-end crash recovery flow |

### Identity Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `InstrumentIdentityAgentTest` | `agent/identity/InstrumentIdentityAgentTest.java` | Ticker validation, cache, Yahoo Finance resolution |
| `InstrumentIdentityIntegrationTest` | `agent/InstrumentIdentityIntegrationTest.java` | Full identity resolution pipeline |

### Memory Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `DecisionMemoryAgentTest` | `agent/memory/DecisionMemoryAgentTest.java` | Store, resolve, past context generation |
| `DecisionMemoryIntegrationTest` | `agent/memory/DecisionMemoryIntegrationTest.java` | End-to-end decision memory flow |
| `DecisionMemoryRepositoryTest` | `agent/memory/DecisionMemoryRepositoryTest.java` | File I/O, regex parsing, atomic writes, rotation |

### Researcher Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `BullResearcherTest` | `agent/researchers/BullResearcherTest.java` | Bull argument generation |
| `BearResearcherTest` | `agent/researchers/BearResearcherTest.java` | Bear argument generation |
| `ResearcherLLMTest` | `agent/researchers/ResearcherLLMTest.java` | Researcher LLM integration |

### Risk Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `RiskDebateServiceLLMTest` | `agent/RiskDebateServiceLLMTest.java` | Risk debate with mocked LLM |
| `RiskDebateServiceUnitTest` | `agent/RiskDebateServiceUnitTest.java` | Risk level classification, fallback parsing |

## Integration Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `AgentDetectionIntegrationTest` | `agent/AgentDetectionIntegrationTest.java` | Agent scanning and registration |
| `FullPipelineIntegrationTest` | `agent/FullPipelineIntegrationTest.java` | End-to-end research pipeline |
| `MultiProviderLLMIntegrationTest` | `config/MultiProviderLLMIntegrationTest.java` | Multiple LLM provider configs |

## Data Flow Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `AlphaVantageServiceTest` | `dataflows/AlphaVantageServiceTest.java` | Alpha Vantage API client |
| `FredServiceTest` | `dataflows/FredServiceTest.java` | FRED API client |
| `PolymarketServiceTest` | `dataflows/PolymarketServiceTest.java` | Polymarket API client |
| `VendorRouterTest` | `dataflows/VendorRouterTest.java` | Data source routing |

## Tool Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `ExtendedDataSourcesIntegrationTest` | `tools/ExtendedDataSourcesIntegrationTest.java` | FRED + Polymarket tool integration |
| `FredDataToolsTest` | `tools/FredDataToolsTest.java` | FRED data tools |
| `FundamentalDataToolsTest` | `tools/FundamentalDataToolsTest.java` | Fundamental data tools |
| `MarketDataToolsTest` | `tools/MarketDataToolsTest.java` | Market data and technical indicators |
| `NewsDataToolsTest` | `tools/NewsDataToolsTest.java` | News data tools |
| `PolymarketDataToolsTest` | `tools/PolymarketDataToolsTest.java` | Polymarket data tools |

## Indicator Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `MFIIndicatorTest` | `indicators/MFIIndicatorTest.java` | Money Flow Index custom indicator |
| `SubtractIndicatorTest` | `indicators/SubtractIndicatorTest.java` | Subtract indicator |
| `VWAPIndicatorTest` | `indicators/VWAPIndicatorTest.java` | Volume Weighted Average Price |
| `VWMAIndicatorTest` | `indicators/VWMAIndicatorTest.java` | Volume Weighted Moving Average |

## Utility Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `DateUtilsTest` | `util/DateUtilsTest.java` | Date formatting utilities |
| `FileCacheTest` | `util/FileCacheTest.java` | File cache read/write, locking, hashing |
| `IndicatorMapperTest` | `util/IndicatorMapperTest.java` | TA4J indicator mapping |

## Configuration Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `TraderAgentConfigTest` | `config/TraderAgentConfigTest.java` | Config property binding and defaults |

## Web Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `HitlServiceTest` | `htmx/HitlServiceTest.java` | HITL session management |
| `TemplateParsingTest` | `web/TemplateParsingTest.java` | Thymeleaf template parsing |

## Test Summary

| Category | Count |
|----------|-------|
| Agent tests | 16 |
| Data flow tests | 4 |
| Tool tests | 6 |
| Indicator tests | 4 |
| Utility tests | 3 |
| Configuration tests | 2 |
| Web tests | 2 |
| Integration tests | 3 |
| **Total** | **43** |