## Context

The TradingAgents project uses Embabel 0.5.0-SNAPSHOT for agent orchestration. There are 16 LLM-calling actions across TraderAgent (11 actions), BullResearcher (1), BearResearcher (1), and RiskDebateService (3). Currently, zero tests use Embabel's testing infrastructure — existing tests extract pure logic from methods rather than testing the actual agent actions.

The `embabel-agent-test` dependency is already in pom.xml with test scope, providing `FakeOperationContext`, `FakePromptRunner`, and `EmbabelMockitoIntegrationTest`.

## Goals / Non-Goals

**Goals:**
- Unit tests for all 16 LLM-calling actions using FakePromptRunner
- Integration tests for core workflows using EmbabelMockitoIntegrationTest
- Each test verifies: LLM role (CHEAPEST/BEST), interaction IDs, prompt content, hyperparameters
- Preserve all existing tests; enhance existing BullResearcherTest/BearResearcherTest

**Non-Goals:**
- Testing non-LLM actions (tickerFromForm, waitForReview) — already covered by existing tests
- Increasing test coverage percentage — focus on correctness of LLM interaction patterns
- Testing actual LLM responses — all LLM calls are stubbed
- Performance testing or load testing

## Decisions

### Decision 1: Separate unit and integration test files
**Choice:** 8 unit test files + 5 integration test files (13 total), NOT a single file per action.
**Rationale:** Unit tests verify prompt construction and LLM interaction patterns in isolation. Integration tests verify full workflow orchestration under Spring Boot. Mixing both in one file couples concerns and makes failure diagnosis harder.
**Alternatives considered:**
- One file per action with both unit and integration tests → harder to run fast unit tests in isolation
- One massive test file → unmanageable, slow to run

### Decision 2: FakePromptRunner for unit tests, EmbabelMockitoIntegrationTest for integration
**Choice:** Use `FakeOperationContext.create()` + `FakePromptRunner` for unit tests; `EmbabelMockitoIntegrationTest` for integration tests.
**Rationale:** FakePromptRunner gives direct access to LLM invocations for verifying prompt content, IDs, and hyperparameters. EmbabelMockitoIntegrationTest provides Spring Boot context with `whenCreateObject()` / `verifyCreateObjectMatching()` for workflow-level verification.
**Alternatives considered:**
- Mockito stubs for unit tests → loses access to prompt content verification
- Only integration tests → slower, harder to diagnose which action fails

### Decision 3: Group related actions in single test files
**Choice:** Group report generators (generateFundamentalsReport, generateMarketReport, generateNewsReport, generateSocialMediaReport) in one test file. Group researchers (BullResearcher, BearResearcher) in one file.
**Rationale:** These actions share patterns (same LLM role, similar cache key structure, same template). Testing them together exposes common patterns and reduces duplication.
**Alternatives considered:**
- One test file per action → more files but cleaner isolation; however, 16 files is excessive for tests that share 80% of structure

### Decision 4: No unit test for debateInvestment
**Choice:** Skip unit test for `debateInvestment` (RepeatUntil subprocess); cover only via integration test.
**Rationale:** debateInvestment uses RepeatUntilBuilder which orchestrates bullAgent and bearAgent sub-calls with cache-based deduplication. Mocking this at the unit level requires mocking the entire RepeatUntil infrastructure, which is complex and fragile. The integration test provides Spring context smoke tests (agent registration) but does NOT invoke the debate loop end-to-end — that remains a known gap.
**Alternatives considered:**
- Mock RepeatUntilBuilder → fragile, breaks with framework updates
- Integration test only → acceptable because the complexity is in the framework, not the business logic
- Extract convergence logic into a pure method → would enable unit testing but adds complexity to the agent

**Known limitation:** No test currently invokes `DebateLoopAgent.debate()` end-to-end. The debate loop (convergence detection, max-iteration stopping, history structure) is the most complex logic in the pipeline and has no direct test coverage. This is documented as a follow-up item.

### Decision 5: Preserve existing tests, add new ones
**Choice:** Do NOT replace existing tests. Add new test files alongside existing ones.
**Rationale:** Existing tests cover pure logic (validation, similarity, parsing) that complements LLM interaction tests. They are fast and don't require Embabel infrastructure. Replacing them would lose valuable fast-feedback tests.
**Alternatives considered:**
- Replace with FakePromptRunner tests → would lose pure logic tests that are faster to run
- Delete existing tests → unnecessary churn, existing tests are valid

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| `embabel-agent-test` 0.5.0-SNAPSHOT API surface may differ from skill docs | Verify API with `mvn test -Dtest=TraderAgentLLMTest` early; adjust if methods differ |
| Spring Boot context fails to start in integration tests | Use `@SpringBootTest` with `@MockBean` for external dependencies (Alpha Vantage API, FileCache) |
| ToolObject mocking (FundamentalDataTools, NewsDataTools) is complex | Unit tests verify `withToolObject()` was called; integration tests mock tool responses via `whenCreateObject()` |
| 13 new test files increase CI time | Unit tests run fast (< 1s each); integration tests grouped in Wave 3; CI can run unit tests on every commit |
| RepeatUntilBuilder mocking for debateInvestment | Skip unit test; cover via integration test only |

## Open Questions

1. Should integration tests use `@MockBean` for FileCache to avoid disk I/O, or use an in-memory cache?
2. Should the full pipeline integration test include the HITL checkpoint (waitForReview), or skip it and test the post-HITL path only?
3. Should we add a `@SpringBootTest` configuration class for integration tests to centralize mock beans?
