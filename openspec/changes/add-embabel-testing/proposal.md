## Why

The TradingAgents project has 18 test files but zero tests using Embabel's testing infrastructure. All agent actions that call LLMs are untested — existing tests extract pure logic from methods rather than testing the actual Embabel actions (prompt construction, LLM role selection, interaction IDs, tool attachment, caching). This leaves the core agent behavior unverified.

## What Changes

- Add **13 new test files** covering all 16 LLM-calling actions across TraderAgent, BullResearcher, BearResearcher, and RiskDebateService
- **8 unit test files** using `FakeOperationContext` + `FakePromptRunner` — verify prompt content, LLM role (CHEAPEST/BEST), interaction IDs, tool attachment, cache keys
- **5 integration test files** using `EmbabelMockitoIntegrationTest` — verify full agent workflows under Spring Boot with `whenCreateObject()` / `verifyCreateObjectMatching()`
- Preserve all existing tests; enhance BullResearcherTest and BearResearcherTest to add argue() structure tests

## Capabilities

### New Capabilities
- `agent-testing`: Unit and integration tests for all agent actions using Embabel testing infrastructure (FakePromptRunner, EmbabelMockitoIntegrationTest)

### Modified Capabilities
<!-- None — no existing specs to modify -->

## Impact

- **New files**: 13 test files in `src/test/java/com/embabel/gekko/agent/` and `src/test/java/com/embabel/gekko/agent/integration/`
- **Modified files**: `BullResearcherTest.java`, `BearResearcherTest.java` (enhanced with argue() structure tests)
- **Dependencies**: No new dependencies — `embabel-agent-test` and `spring-boot-starter-test` already in pom.xml test scope
- **No production code changes**
