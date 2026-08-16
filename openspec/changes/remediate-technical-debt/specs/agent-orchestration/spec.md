## MODIFIED Requirements

### Requirement: DebateAgent orchestrates the full debate workflow

The `DebateAgent` SHALL have the following actions in its action list:
- `generateFundamentalsReport` — produce `FundamentalsReport` from `Ticker`
- `generateMarketReport` — produce `MarketReport` from `Ticker`
- `generateNewsReport` — produce `NewsReport` from `Ticker`
- `generateSocialMediaReport` — produce `SocialMediaReport` from `Ticker`
- `prepareDebateBriefs` — produce `DebateBriefs` from `Ticker` + all 4 reports
- `runDebate` — invoke `DebateLoopAgent` via `asSubProcess()`, produce `InvestmentDebateState`
- `runRiskDebate` — invoke `RiskDebateAgent` via `asSubProcess()`, produce `RiskAssessment`
- `waitForReview` — HITL checkpoint using `WaitFor.formSubmission()`, produce `InvestmentReviewFeedback`
- `researchManager` — produce `InvestmentPlan` from all prior state + feedback, marked with `@AchievesGoal`

The planner SHALL discover and chain these actions based on their input/output type flow on the blackboard.

The `researchManager` action SHALL generate the final `InvestmentPlan` from a prompt that includes, in addition to the debate history and any user feedback: the `RiskAssessment` (risk level, recommendation, and reasoning), the portfolio decision, and the instrument identity (ticker, company name, sector, industry, exchange). A change to any of these inputs SHALL be reflected in the result and in the result's cache key.

#### Scenario: DebateAgent generates all analyst reports
- **WHEN** `DebateAgent` is invoked with a `Ticker`
- **THEN** the planner discovers and executes all four report generation actions, producing `FundamentalsReport`, `MarketReport`, `NewsReport`, and `SocialMediaReport`

#### Scenario: DebateAgent prepares debate briefs from reports
- **WHEN** all four reports exist on the blackboard
- **THEN** `prepareDebateBriefs` executes, producing `DebateBriefs` containing distilled arguments for bull and bear positions

#### Scenario: DebateAgent runs debate loop as subagent
- **WHEN** `DebateBriefs` exists on the blackboard
- **THEN** `runDebate` invokes `DebateLoopAgent` via `asSubProcess()`, which returns `InvestmentDebateState`

#### Scenario: DebateAgent runs risk debate as subagent
- **WHEN** `InvestmentDebateState` exists on the blackboard
- **THEN** `runRiskDebate` invokes `RiskDebateAgent` via `asSubProcess()`, which returns `RiskAssessment`

#### Scenario: DebateAgent waits for user review
- **WHEN** `RiskAssessment` exists on the blackboard
- **THEN** `waitForReview` pauses the process with a HITL form for debate review

#### Scenario: DebateAgent generates final plan after approval
- **WHEN** user provides `InvestmentReviewFeedback`
- **THEN** `researchManager` produces an `InvestmentPlan` whose prompt incorporates the debate history, the risk assessment, the portfolio decision, the instrument identity, and the user feedback

#### Scenario: Final plan prompt includes the risk assessment
- **GIVEN** a `RiskAssessment` with a risk level, recommendation, and reasoning
- **WHEN** `researchManager` builds its prompt
- **THEN** the prompt includes the risk level, recommendation, and reasoning

#### Scenario: Final plan prompt includes the portfolio decision and identity
- **GIVEN** a portfolio decision and a resolved `InstrumentContext`
- **WHEN** `researchManager` builds its prompt
- **THEN** the prompt includes the portfolio decision and the ticker, company name, sector, industry, and exchange

#### Scenario: Final plan prompt handles a failed identity resolution
- **WHEN** `researchManager` builds its prompt and identity resolution has failed
- **THEN** the prompt includes a placeholder for the missing identity fields
- **AND** the action does not fail due to the missing identity

## ADDED Requirements

### Requirement: Risk assessment output is robust to structured-output failure

The risk-debate step SHALL produce a risk assessment (level, reasoning, recommendation) that is not defaulted to RISKY solely by the presence of debator speaker labels in the transcript. The debate transcript by construction always contains labels such as "Aggressive (Round 1)" and "Conservative (Round N)", so a substring keyword match on those labels MUST NOT determine the risk level. When the risk judge's structured output is unavailable or fails to parse, the system SHALL either re-request structured output or record an explicit undetermined/neutral level; it MUST NOT classify RISKY by keyword matching on the transcript.

#### Scenario: Structured risk output succeeds
- **GIVEN** the risk judge returns a well-formed structured risk assessment
- **WHEN** the risk debate completes
- **THEN** the assessment's level, reasoning, and recommendation are used as-is

#### Scenario: Structured risk output is unavailable
- **GIVEN** the risk judge's structured output fails to parse
- **WHEN** the risk debate completes
- **THEN** the system either re-requests structured output or records an explicit undetermined/neutral level
- **AND** it does not classify RISKY solely because the transcript contains "Aggressive" / "Conservative" speaker labels
