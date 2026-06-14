## ADDED Requirements

### Requirement: All LLM-calling agent actions MUST have unit tests using FakePromptRunner

Every @Action method in TraderAgent, BullResearcher, BearResearcher, and RiskDebateService that invokes an LLM call MUST have at least one unit test using `FakeOperationContext.create()` and `FakePromptRunner` that verifies the LLM interaction.

#### Scenario: Unit test verifies LLM role selection

- **WHEN** a unit test calls an action method with a FakeOperationContext
- **THEN** the test verifies the correct LLM role was used (CHEAPEST_ROLE for report generators and distillers, BEST_ROLE for researchers and research manager)

#### Scenario: Unit test verifies interaction ID

- **WHEN** a unit test calls an action method with a FakeOperationContext
- **THEN** the test verifies the `.withId("...")` interaction ID matches the action's expected ID

#### Scenario: Unit test verifies prompt content

- **WHEN** a unit test calls an action method with a FakeOperationContext
- **THEN** the test extracts the prompt from `FakePromptRunner.getLlmInvocations()` and verifies it contains expected content (ticker, report data, template variables)

#### Scenario: Unit test verifies tool attachment

- **WHEN** an action calls `withToolObject(...)` and a unit test invokes that action
- **THEN** the test verifies the tool object was attached to the LLM interaction

### Requirement: All LLM-calling agent actions MUST have integration tests using EmbabelMockitoIntegrationTest

Every @Action method in TraderAgent, BullResearcher, BearResearcher, and RiskDebateService that invokes an LLM call MUST have at least one integration test using `EmbabelMockitoIntegrationTest` that stubs LLM responses and verifies the full workflow.

#### Scenario: Integration test stubs LLM response

- **WHEN** an integration test invokes an agent action via AgentInvocation
- **THEN** the test uses `whenCreateObject()` or `whenGenerateText()` to stub the LLM response before invocation

#### Scenario: Integration test verifies LLM call parameters

- **WHEN** an integration test completes an agent action invocation
- **THEN** the test uses `verifyCreateObjectMatching()` to verify the prompt content, LLM role, and interaction ID

### Requirement: Unit tests for report generators MUST verify caching behavior

The four report generator actions (generateFundamentalsReport, generateMarketReport, generateNewsReport, generateSocialMediaReport) use `FileCache.getOrCompute()` with ticker-specific keys. Unit tests MUST verify the cache key construction.

#### Scenario: Unit test verifies cache key for fundamentals report

- **WHEN** generateFundamentalsReport is called with ticker "AAPL"
- **THEN** the cache key is "AAPL_fundamentals"

#### Scenario: Unit test verifies cache key for market report

- **WHEN** generateMarketReport is called with ticker "NVDA"
- **THEN** the cache key is "NVDA_market"

### Requirement: Unit tests for prepareDebateBriefs MUST verify multiple LLM invocations

The prepareDebateBriefs action calls 4 distill LLM calls (one per report type). Unit tests MUST verify all 4 invocations.

#### Scenario: Unit test verifies 4 distill invocations

- **WHEN** prepareDebateBriefs is called with all 4 reports
- **THEN** exactly 4 LLM invocations are recorded in FakePromptRunner

#### Scenario: Unit test verifies distill interaction IDs

- **WHEN** prepareDebateBriefs is called
- **THEN** the interaction IDs are: distillBrief_fundamentals, distillBrief_market, distillBrief_news, distillBrief_social_media

### Requirement: Unit tests for risk debate MUST verify 10 LLM invocations

The runRiskDebate action calls 3 rounds × 3 debaters + 1 judge = 10 LLM calls. Unit tests MUST verify the total count and interaction IDs.

#### Scenario: Unit test verifies 10 LLM invocations

- **WHEN** runRiskDebate is called with valid inputs
- **THEN** exactly 10 LLM invocations are recorded (9 debator + 1 judge)

#### Scenario: Unit test verifies debator vs judge IDs

- **WHEN** runRiskDebate is called
- **THEN** debator calls use id "riskDebator" and the judge call uses id "riskJudge"

### Requirement: Unit tests for researchers MUST verify BEST_ROLE and template usage

Both BullResearcher.argue() and BearResearcher.argue() use the BEST_ROLE and Jinja templates. Unit tests MUST verify both.

#### Scenario: Unit test verifies BEST_ROLE for bull researcher

- **WHEN** BullResearcher.argue() is called with a FakeOperationContext
- **THEN** the LLM interaction uses BEST_ROLE

#### Scenario: Unit test verifies template usage for bull researcher

- **WHEN** BullResearcher.argue() is called
- **THEN** the LLM interaction uses template "researchers/BullResearcher"

### Requirement: Unit tests for researchManager MUST verify model content

The researchManager action builds a model with debate history, risk assessment, and user feedback. Unit tests MUST verify model content is passed correctly.

#### Scenario: Unit test verifies debate history in model

- **WHEN** researchManager is called with an InvestmentDebateState
- **THEN** the prompt model includes the debate history

#### Scenario: Unit test verifies risk assessment in model

- **WHEN** researchManager is called with a risk assessment in the debate state
- **THEN** the prompt model includes risk_level and risk_reasoning

### Requirement: Unit tests for pure logic methods MUST cover security and edge cases

Methods like sanitizeForPrompt and computeSimilarity that don't call LLMs MUST have dedicated unit tests covering security sanitization and edge cases.

#### Scenario: Unit test verifies Jinja syntax stripping

- **WHEN** sanitizeForPrompt receives input containing `{{` or `{%`
- **THEN** the output has Jinja syntax replaced with safe placeholders

#### Scenario: Unit test verifies control character stripping

- **WHEN** sanitizeForPrompt receives input containing control characters (NUL, BEL, BS)
- **THEN** the output has control characters removed

#### Scenario: Unit test verifies similarity edge cases

- **WHEN** computeSimilarity receives null or empty strings
- **THEN** the result is 1.0 for two empty strings and 0.0 for one empty and one non-empty

### Requirement: Integration tests for full pipeline MUST verify end-to-end workflow

The full research pipeline (executeFullResearch) orchestrates report generation, debate, risk debate, HITL review, and research manager. An integration test MUST verify the complete flow.

#### Scenario: Integration test verifies full pipeline returns InvestmentPlan

- **WHEN** AgentInvocation.invoke() is called with a UserInput containing a ticker
- **THEN** the result is a non-null InvestmentPlan with a judge decision

#### Scenario: Integration test verifies call sequence

- **WHEN** the full pipeline executes
- **THEN** LLM calls occur in order: report generators → distillers → debate → risk debate → research manager
