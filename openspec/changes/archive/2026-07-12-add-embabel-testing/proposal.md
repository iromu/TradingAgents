## Why

The TradingAgents project has 18 test files but zero tests using Embabel's testing infrastructure. All agent actions that call LLMs are untested — existing tests extract pure logic from methods rather than testing the actual Embabel actions (prompt construction, LLM role selection, interaction IDs, tool attachment, caching). This leaves the core agent behavior unverified.

## What Changes

- Add **14 new test files** covering all LLM-calling actions across TraderAgent, BullResearcher, BearResearcher, and RiskDebateService
- **9 unit test files** using `FakeOperationContext` + `FakePromptRunner` — verify prompt content, LLM role (CHEAPEST/BEST), interaction IDs, tool attachment, cache keys
- **5 integration test files** using `EmbabelMockitoIntegrationTest` — verify full agent workflows under Spring Boot with `whenCreateObject()` / `verifyCreateObjectMatching()`
- Preserve all existing tests; enhance BullResearcherTest and BearResearcherTest to add argue() structure tests

## Capabilities

### New Capabilities
- `agent-testing`: Unit and integration tests for all agent actions using Embabel testing infrastructure (FakePromptRunner, EmbabelMockitoIntegrationTest)

### Modified Capabilities
<!-- None — no existing specs to modify -->

## Impact

- **New files**: 14 test files in `src/test/java/com/embabel/gekko/agent/` and `src/test/java/com/embabel/gekko/agent/integration/`
- **Modified files**: `BullResearcherTest.java`, `BearResearcherTest.java` (enhanced with argue() structure tests), `FullPipelineIntegrationTest.java` (enhanced)
- **Renamed file**: `TraderAgentLLMTest.java` → `OrchestratorAgentLLMTest.java` (class name mismatch fix)
- **Dependencies**: No new dependencies — `embabel-agent-test` and `spring-boot-starter-test` already in pom.xml test scope
- **No production code changes**

## Known Gaps

| Gap | Severity | Reason |
|-----|----------|--------|
| `debateInvestment` (DebateLoopAgent.debate()) | MEDIUM | RepeatUntilBuilder with asSubProcess cannot be intercepted by FakePromptRunner; covered by DebateLoopAgentTest.computeSimilarity (17 tests) + integration tests |
| `runDebate`/`runRiskDebate`/`runTrader`/`runPortfolioManager` (DebateAgent sub-process delegations) | LOW | Covered by unit tests of the target agents; sub-process delegation itself not tested |
| Integration tests use FakePromptRunner pattern | HIGH | Except FullPipelineIntegrationTest, "integration" tests use unit test patterns — no Spring context with `whenCreateObject()`/`verifyCreateObjectMatching()` |
