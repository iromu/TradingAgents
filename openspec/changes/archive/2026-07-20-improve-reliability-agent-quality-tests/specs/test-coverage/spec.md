## ADDED Requirements

### Requirement: Core agent actions have unit tests
The system SHALL have unit tests covering the core agent actions to verify correct behavior of the investment research pipeline.

#### Scenario: tickerFromForm validates input
- **WHEN** `tickerFromForm()` is called with valid ticker input
- **THEN** it returns a `Ticker` with uppercase content
- **AND** invalid formats throw `IllegalArgumentException`

#### Scenario: generateResearchPlan uses cache
- **WHEN** `generateResearchPlan()` is called twice with the same ticker
- **THEN** the cache is hit on the second call
- **AND** the result is a valid `ResearchPlan`

#### Scenario: prepareDebateBriefs validates inputs
- **WHEN** `prepareDebateBriefs()` is called with null or blank reports
- **THEN** it throws `IllegalArgumentException`
- **AND** all four report types are validated

### Requirement: Researcher agents have unit tests
The system SHALL have unit tests covering `BullResearcher` and `BearResearcher` to verify correct argument generation.

#### Scenario: BullResearcher.argue produces response
- **WHEN** `argue()` is called with valid briefs and history
- **THEN** it returns a non-empty string response
- **AND** the response references the debate briefs

#### Scenario: BearResearcher.argue produces response
- **WHEN** `argue()` is called with valid briefs and history
- **THEN** it returns a non-empty string response
- **AND** the response references the debate briefs

### Requirement: Integration tests cover full pipeline
The system SHALL have integration tests that exercise the full research pipeline from ticker input to investment plan generation.

#### Scenario: Full pipeline executes end-to-end
- **WHEN** a ticker is submitted through the pipeline
- **THEN** all four analyst reports are generated
- **AND** debate briefs are produced
- **AND** the investment debate completes
- **AND** the research manager produces a plan

#### Scenario: Pipeline handles API failures gracefully
- **WHEN** an external API (Alpha Vantage or Yahoo Finance) returns an error
- **THEN** the pipeline throws a descriptive exception
- **AND** the exception does not leak internal implementation details

### Requirement: Prompt file extension tests
The system SHALL verify that all prompt files load correctly with the unified `.jinja` extension.

#### Scenario: All analyst prompts load as .jinja
- **WHEN** the system starts
- **THEN** all four analyst prompts load without errors
- **AND** the prompt content is non-empty

#### Scenario: No .txt prompt files remain on classpath
- **WHEN** the prompts directory is scanned at runtime
- **THEN** no `.txt` files are found
- **AND** all prompts use `.jinja` extension

### Requirement: PortfolioManager has integration tests
The system SHALL have integration tests for `PortfolioManager.portfolioDecision()` to verify correct LLM interaction.

#### Scenario: PortfolioManager makes LLM call with correct role
- **WHEN** `portfolioDecision()` is called with valid inputs
- **THEN** the LLM is invoked with `BEST_ROLE`
- **AND** the interaction ID is "portfolioManager"

#### Scenario: PortfolioManager prompt contains all inputs
- **WHEN** `portfolioDecision()` is called
- **THEN** the prompt includes ticker, debate state, research plan, trader proposal, and risk assessment
- **AND** the prompt is non-empty

#### Scenario: PortfolioManager tries structured output first
- **WHEN** `portfolioDecision()` is called
- **THEN** it attempts `PortfolioDecisionOutput` structured output first
- **AND** falls back to free-text String on failure

### Requirement: InstrumentIdentityAgent has unit tests
The system SHALL have unit tests for `InstrumentIdentityAgent.resolveIdentity()` to verify ticker resolution.

#### Scenario: Identity resolution uses YFinService
- **WHEN** `resolveIdentity()` is called with a valid ticker
- **THEN** it returns an `InstrumentContext` with company details
- **AND** the result is cached

#### Scenario: Identity resolution handles invalid tickers
- **WHEN** `resolveIdentity()` is called with an invalid ticker
- **THEN** it throws an appropriate exception
- **AND** the error is descriptive

#### Scenario: Cached identity is returned on second call
- **WHEN** `resolveIdentity()` is called twice with the same ticker
- **THEN** the second call returns the cached result
- **AND** YFinService is not called twice

### Requirement: InstrumentContextPromptContributor has tests
The system SHALL have tests for `InstrumentContextPromptContributor` to verify prompt injection.

#### Scenario: Contribution returns formatted context
- **WHEN** `contribution()` is called after setting context
- **THEN** it returns a non-empty string with company name, sector, industry, exchange
- **AND** the string includes the ticker

#### Scenario: Contribution is empty before context is set
- **WHEN** `contribution()` is called without setting context
- **THEN** it returns an empty string

### Requirement: RiskDebateAgent has integration tests
The system SHALL have integration tests for `RiskDebateAgent.assessRisk()` to verify the full 3-round risk debate workflow.

#### Scenario: Risk debate runs 3 rounds with 3 debators each
- **WHEN** `assessRisk()` is called with valid inputs
- **THEN** it invokes AggressiveDebator, ConservativeDebator, and NeutralDebator 3 times each (9 total)
- **AND** the judge produces a final `RiskAssessment`

#### Scenario: Risk debate produces valid risk level
- **WHEN** `assessRisk()` completes
- **THEN** the returned `RiskAssessment` has a valid `RiskLevel` (RISKY, NEUTRAL, or CONSERVATIVE)
- **AND** the reasoning field is non-empty

### Requirement: OrchestratorAgent sub-process delegation has integration tests
The system SHALL have integration tests for `OrchestratorAgent.executeDebate()` to verify sub-process delegation to DebateAgent.

#### Scenario: executeDebate delegates to DebateAgent
- **WHEN** `executeDebate()` is called
- **THEN** it invokes DebateAgent via `asSubProcess`
- **AND** the output type is `InvestmentPlan`

### Requirement: DebateAgent sub-process actions have integration tests
The system SHALL have integration tests for `DebateAgent.runDebate()`, `runTrader()`, `runRiskDebate()`, and `runPortfolioManager()` to verify sub-process and direct service calls.

#### Scenario: runDebate delegates to DebateLoopAgent
- **WHEN** `runDebate()` is called with briefs
- **THEN** it invokes DebateLoopAgent via `asSubProcess`
- **AND** returns an `InvestmentDebateState`

#### Scenario: runTrader calls Trader directly
- **WHEN** `runTrader()` is called with a research plan
- **THEN** it delegates to `Trader.traderProposal()`
- **AND** returns a non-empty string

#### Scenario: runRiskDebate calls RiskDebateAgent directly
- **WHEN** `runRiskDebate()` is called with debate state
- **THEN** it delegates to `RiskDebateAgent.assessRisk()`
- **AND** returns a `RiskAssessment`

#### Scenario: runPortfolioManager calls PortfolioManager directly
- **WHEN** `runPortfolioManager()` is called with debate state and risk assessment
- **THEN** it delegates to `PortfolioManager.portfolioDecision()`
- **AND** returns a non-empty string

### Requirement: DebateLoopAgent debate() has integration tests
The system SHALL have integration tests for `DebateLoopAgent.debate()` to verify the full LLM-calling debate loop.

#### Scenario: Debate loop runs with LLM calls
- **WHEN** `debate()` is called with briefs
- **THEN** it invokes BullResearcher and BearResearcher via their `argue()` methods
- **AND** returns an `InvestmentDebateState` with history

#### Scenario: Debate loop converges on similar responses
- **WHEN** consecutive bull responses have high similarity (>= threshold)
- **THEN** the debate loop terminates early
- **AND** the final state includes all debate history

### Requirement: MarketDataTools has integration tests
The system SHALL have integration tests for `MarketDataTools` to verify the tool methods.

#### Scenario: get_stock_data returns data for valid ticker
- **WHEN** `get_stock_data()` is called with a valid ticker and date range
- **THEN** it returns a non-empty string with stock data
- **AND** errors are handled gracefully

#### Scenario: get_indicators returns indicator calculations
- **WHEN** `get_indicators()` is called with valid parameters
- **THEN** it returns indicator calculation results
- **AND** errors for individual indicators are reported without failing the whole call

### Requirement: DebateAgent helper methods have unit tests
The system SHALL have unit tests for DebateAgent helper methods that are not directly exposed as @Action methods.

#### Scenario: sanitizeForPrompt blocks Jinja template injection
- **WHEN** input contains `{{ variable }}` syntax
- **THEN** it is replaced with `[BLOCKED_TEMPLATE]`
- **AND** input containing `{{` without closing `}}` is also blocked

#### Scenario: extractRating identifies buy/sell/hold
- **WHEN** content contains the word "buy"
- **THEN** it returns "Buy"
- **AND** "sell" → "Sell", "overweight" → "Overweight", default → "Hold"

#### Scenario: extractSummary extracts first paragraph
- **WHEN** content contains ".\n" within the first 500 characters
- **THEN** it returns text up to and including the first period followed by newline
- **AND** truncated content is limited to 500 characters

#### Scenario: extractThesis finds thesis/rationale section
- **WHEN** content contains "thesis" or "rationale"
- **THEN** it returns text from that keyword to the next double-newline
- **AND** the result is limited to 500 characters

### Requirement: DebateLoopAgent convergence has integration tests
The system SHALL have integration tests verifying the debate loop's convergence behavior with LLM calls.

#### Scenario: Debate loop stops at max iterations
- **WHEN** the debate reaches `maxDebateIterations`
- **THEN** the loop terminates
- **AND** the final state includes all debate history

#### Scenario: Debate loop converges before max iterations
- **WHEN** bull responses become similar (similarity >= threshold)
- **THEN** the loop terminates early
- **AND** the similarity is logged