---
title: "Test Coverage"
type: "note"
status: "active"
language: "default"
source_paths:
  - "src/test/java/com/embabel/gekko/"
updated_at: "2026-08-02"
---

# Test Coverage

The project has **62 test classes** across agent, data, tool, utility, and integration layers.

## Agent Tests

### Core Agent Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `AllAgentsPlanValidationTest` | `agent/AllAgentsPlanValidationTest.java` | Plan validation across all agents |
| `DebateAgentHelperTest` | `agent/DebateAgentHelperTest.java` | DebateAgent helper methods |
| `DebateAgentLLMTest` | `agent/DebateAgentLLMTest.java` | DebateAgent with mocked LLM |
| `DebateAgentSubProcessIntegrationTest` | `agent/DebateAgentSubProcessIntegrationTest.java` | DebateAgent asSubProcess integration |
| `DebateBriefsUnitTest` | `agent/DebateBriefsUnitTest.java` | Brief distillation logic |
| `DebateLoopAgentTest` | `agent/DebateLoopAgentTest.java` | Bull/bear loop and convergence detection |
| `DebateLoopAgentIntegrationTest` | `agent/DebateLoopAgentIntegrationTest.java` | DebateLoopAgent integration |
| `OrchestratorAgentResearchPlanTest` | `agent/OrchestratorAgentResearchPlanTest.java` | Research plan generation |
| `OrchestratorExecuteDebateIntegrationTest` | `agent/OrchestratorExecuteDebateIntegrationTest.java` | Orchestrator executeDebate integration |
| `PureLogicTest` | `agent/PureLogicTest.java` | Jaccard similarity, bigram extraction |
| `TraderAgentTickerValidationTest` | `agent/TraderAgentTickerValidationTest.java` | Ticker format validation |
| `TraderLLMTest` | `agent/TraderLLMTest.java` | Trader agent LLM tests |

### Checkpoint Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `CheckpointAgentTest` | `agent/checkpoint/CheckpointAgentTest.java` | Save, restore, clear checkpoint actions |
| `CheckpointStoreTest` | `agent/checkpoint/CheckpointStoreTest.java` | JSON serialization, atomic writes, path traversal |
| `CheckpointResumeIntegrationTest` | `agent/checkpoint/CheckpointResumeIntegrationTest.java` | End-to-end crash recovery flow |

### Identity Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `InstrumentContextPromptContributorTest` | `agent/identity/InstrumentContextPromptContributorTest.java` | Instrument context prompt contributions |
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
| `AggressiveDebatorTest` | `agent/risk/AggressiveDebatorTest.java` | Aggressive debator arguments |
| `ConservativeDebatorTest` | `agent/risk/ConservativeDebatorTest.java` | Conservative debator arguments |
| `NeutralDebatorTest` | `agent/risk/NeutralDebatorTest.java` | Neutral debator arguments |
| `RiskDebateAgentIntegrationTest` | `agent/RiskDebateAgentIntegrationTest.java` | RiskDebateAgent integration |
| `RiskDebateServiceLLMTest` | `agent/RiskDebateServiceLLMTest.java` | Risk debate with mocked LLM |
| `RiskDebateServiceUnitTest` | `agent/RiskDebateServiceUnitTest.java` | Risk level classification, fallback parsing |

## Integration Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `AgentDetectionIntegrationTest` | `agent/AgentDetectionIntegrationTest.java` | Agent scanning and registration |
| `FullPipelineIntegrationTest` | `agent/FullPipelineIntegrationTest.java` | End-to-end research pipeline |
| `MultiProviderLLMIntegrationTest` | `config/MultiProviderLLMIntegrationTest.java` | Multiple LLM provider configs |
| `ReportGeneratorIntegrationTest` | `agent/integration/ReportGeneratorIntegrationTest.java` | Report generator integration |
| `ResearcherIntegrationTest` | `agent/integration/ResearcherIntegrationTest.java` | Researcher integration |
| `RiskDebateServiceIntegrationTest` | `agent/integration/RiskDebateServiceIntegrationTest.java` | Risk debate service integration |

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
| `MarketDataToolsIntegrationTest` | `tools/MarketDataToolsIntegrationTest.java` | Market data tools integration |
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
| `LlmBudgetTrackerTest` | `util/LlmBudgetTrackerTest.java` | LLM budget tracking and warnings |

## Configuration Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `PortfolioManagerExtendedTest` | `agent/managers/PortfolioManagerExtendedTest.java` | Extended portfolio manager tests |
| `PortfolioManagerLLMTest` | `agent/managers/PortfolioManagerLLMTest.java` | Portfolio manager LLM tests |
| `TraderAgentConfigTest` | `config/TraderAgentConfigTest.java` | Config property binding and defaults |

## Web Tests

| Test Class | Source | What it tests |
|------------|--------|---------------|
| `HitlServiceTest` | `htmx/HitlServiceTest.java` | HITL session management |
| `TemplateParsingTest` | `web/TemplateParsingTest.java` | Thymeleaf template parsing |
| `WaitForPollingUnitTest` | `htmx/WaitForPollingUnitTest.java` | WaitFor polling unit tests |

## Test Summary

| Category | Count |
|----------|-------|
| Agent tests | 25 |
| Data flow tests | 4 |
| Tool tests | 7 |
| Indicator tests | 4 |
| Utility tests | 4 |
| Configuration tests | 3 |
| Web tests | 3 |
| Integration tests | 6 |
| **Total** | **62** |